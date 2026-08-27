package com.revrec.pinot

import org.apache.pinot.segment.spi.partition.MurmurPartitionFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

/**
 * Hourly publish: Iceberg (source of truth) -> Pinot offline table (serving).
 *
 * Illustrative, not compiled. Shows the SHAPE of the production job; API details
 * drift between Pinot versions.
 *
 * The whole job is one Spark application. `SparkSegmentGenerationJobRunner`
 * internally parallelises over the input files on the *ambient* SparkContext, so
 * generation runs inside this app rather than as a second `spark-submit` — one
 * cluster allocation, one set of credentials, one failure domain.
 *
 * Ordering is forced by the lineage protocol: `segmentsTo` is not knowable until
 * the segments exist, and `startReplaceSegments` needs it up front. So the phases
 * are write -> generate -> open transaction -> push -> commit, per partition.
 */
object PublishReportToPinot {

  /** The unit of work: one (accounting period, merchant bucket) partition. */
  final case class Partition(period: String, bucket: Int) {

    /** Leftmost and stable, because this prefix is the ONLY index for finding
      * which segments a partition currently occupies — Pinot will not tell us
      * from the data. The run id goes to the RIGHT of it so the prefix still
      * matches segments written by earlier runs. */
    def segmentPrefix: String = f"revrec_${period}_b$bucket%02d_"

    def stagingDir(base: String, runId: String): String =
      s"$base/$period/b$bucket/r$runId"
  }

  def main(args: Array[String]): Unit = {
    val cfg   = JobConfig.parse(args)
    val runId = cfg.runId // e.g. the Airflow logical date; must be unique per run
    val spark = SparkSession.builder().appName("revrec-pinot-publish").getOrCreate()

    // One lineage transaction per partition, deliberately:
    //  - a failure isolates to a single partition rather than the whole run;
    //  - the ZK znode holding segmentsFrom/segmentsTo stays small (a whale
    //    merchant's full history would otherwise push ~10k segment names into
    //    one entry, against ZK's 1MB default jute.maxbuffer).
    dirtyPartitions(spark, cfg).foreach { part =>
      val staging = part.stagingDir(cfg.stagingBase, runId)
      writeFullPartition(spark, part, cfg, staging)
      PinotSegmentSwap.generateAndSwap(spark, cfg, part, staging, runId)
    }

    spark.stop()
  }

  /**
   * The delta's ONLY job is to say which partitions are dirty — never to be the
   * data that gets pushed.
   *
   * Derived from the Iceberg snapshot the hourly `MERGE INTO` just produced, so
   * it reflects exactly what changed, including late-arriving corrections that
   * landed in an older accounting period.
   */
  private def dirtyPartitions(spark: SparkSession, cfg: JobConfig): Seq[Partition] =
    spark.read
      .format("iceberg")
      .option("start-snapshot-id", cfg.lastPublishedSnapshotId)
      .load(s"${cfg.icebergTable}.changes")
      .select(col("accounting_period"), pinotBucket(cfg.numBuckets)(col("merchant_id")).as("bucket"))
      .distinct()
      .collect()
      .map(r => Partition(r.getString(0), r.getInt(1)))
      .toSeq

  /**
   * Read the partition back in FULL and overwrite the staging dir.
   *
   * Not the delta. A Pinot offline table has no primary key and no upsert — it
   * appends whatever it is handed, so pushing only changed rows double-counts
   * silently in an accounting ledger. Correctness comes from replacing the
   * partition wholesale.
   */
  private def writeFullPartition(
      spark: SparkSession, part: Partition, cfg: JobConfig, staging: String): Unit = {

    val bucketOf = pinotBucket(cfg.numBuckets)

    val full: DataFrame = spark.read
      .format("iceberg")
      .load(cfg.icebergTable)
      .where(col("accounting_period") === part.period)
      .where(bucketOf(col("merchant_id")) === part.bucket)

    full
      // Every output file must contain rows of exactly ONE Pinot partition id,
      // or `segmentPartitionConfig` pruning silently degrades to a full scan
      // instead of erroring. The bucket filter above already guarantees it;
      // coalescing (rather than repartitioning) preserves it.
      .coalesce(cfg.filesPerPartition)
      // Matches `sortedColumn: merchant_id` in the table config: run-length
      // compression on the merchant column, and a merchant's rows land
      // contiguously so a later single-merchant recovery touches few segments.
      .sortWithinPartitions(col("merchant_id"))
      .write
      .mode(SaveMode.Overwrite)
      .parquet(staging)
  }

  /**
   * Pinot's `Murmur` partition function is Murmur2 with its own seed; Spark's
   * `hash()` is Murmur3. They do not agree, and a mismatch means segments are
   * not partition-pure — which disables broker pruning without raising anything.
   * So borrow Pinot's own implementation rather than reimplementing it.
   *
   * Instantiated lazily per executor: the function object is not serializable.
   */
  private def pinotBucket(numBuckets: Int) = udf { (merchantId: String) =>
    PinotBucket.fn(numBuckets).getPartition(merchantId)
  }

  private object PinotBucket {
    @transient private lazy val cache =
      new java.util.concurrent.ConcurrentHashMap[Int, MurmurPartitionFunction]()
    def fn(n: Int): MurmurPartitionFunction =
      cache.computeIfAbsent(n, new MurmurPartitionFunction(_))
  }
}

package com.revrec.pinot

import org.apache.pinot.common.restlet.resources.StartReplaceSegmentsRequest
import org.apache.pinot.common.utils.FileUploadDownloadClient
import org.apache.pinot.plugin.ingestion.batch.spark3.{SparkSegmentGenerationJobRunner, SparkSegmentMetadataPushJobRunner}
import org.apache.pinot.spi.ingestion.batch.IngestionJobLauncher
import org.apache.pinot.spi.ingestion.batch.spec.SegmentGenerationJobSpec
import org.apache.spark.sql.SparkSession

import scala.util.{Failure, Success, Try}

/**
 * Brackets a segment push in Pinot's segment lineage protocol so one partition
 * is replaced atomically, leaving every other partition untouched.
 *
 * Illustrative, not compiled.
 *
 * Deliberately NOT using `batchIngestionConfig.consistentDataPush`. That wraps a
 * push in this same protocol automatically, but with *whole-table* semantics:
 * `segmentsFrom` becomes every segment in the table. It exists for full-dataset
 * refreshes; here it would delete every partition absent from this push.
 */
object PinotSegmentSwap {

  def generateAndSwap(
      spark: SparkSession,
      cfg: JobConfig,
      part: PublishReportToPinot.Partition,
      staging: String,
      runId: String): Unit = {

    // ---- 1. build segments -------------------------------------------------
    // Runs on the ambient SparkContext: the runner parallelises over the parquet
    // files written by the previous phase. Nothing is pushed here.
    //
    // The run id is part of the segment name and is NOT cosmetic: `segmentsTo`
    // may not name segments that already exist, so a deterministic name would
    // collide with the very segments being replaced on every rerun.
    val genSpec = jobSpec(cfg, part, staging, runId, "SegmentCreation")
    val generator = new SparkSegmentGenerationJobRunner()
    generator.init(genSpec)
    generator.run()

    // ---- 2. the two lists --------------------------------------------------
    // segmentsTo: what we just built.
    val segmentsTo = listSegmentTars(spark, staging)

    // segmentsFrom: what those supersede. Computed by name prefix, because Pinot
    // has no content-based notion of "the segments holding this partition". Read
    // BEFORE the push, so it contains only prior runs' segments.
    val client = new FileUploadDownloadClient()
    val segmentsFrom =
      listExistingSegments(client, cfg, part.segmentPrefix)

    // Pinot guarantees ATOMICITY, NOT CONSERVATION: it never checks that
    // segmentsTo covers the rows that were in segmentsFrom. A short list is an
    // atomic, successful, silent delete — so refuse the shapes that would be one.
    require(segmentsTo.nonEmpty || !cfg.refuseEmptySegmentsTo,
      s"refusing: no segments generated under $staging; an empty segmentsTo " +
      s"would delete partition ${part.segmentPrefix}")
    val clash = segmentsFrom.toSet intersect segmentsTo.toSet
    require(clash.isEmpty || !cfg.refuseOverlappingLists,
      s"refusing: segmentsTo must not name existing segments, but $clash appear " +
      s"in both lists — the run id is not making names unique")

    // M and N are unrelated: maxNumRecordsPerSegment lets a partition's segment
    // count drift as its row count changes, which is why this is an M-to-N swap
    // and not name-collision replacement.
    log(s"${part.segmentPrefix}: ${segmentsFrom.size} -> ${segmentsTo.size} segments")

    // ---- 3. open the transaction ------------------------------------------
    // forceCleanup revokes a stale IN_PROGRESS entry left by a crashed run.
    val entryId = client.startReplaceSegments(
      FileUploadDownloadClient.getStartReplaceSegmentsURI(
        cfg.controllerUri, cfg.rawTableName, cfg.tableType, cfg.forceCleanup),
      new StartReplaceSegmentsRequest(segmentsFrom.asJava, segmentsTo.asJava))

    // ---- 4. push inside it, then commit ------------------------------------
    // Metadata push: the controller receives metadata plus a deep-store download
    // URI. Segment tarballs are never streamed through it — at these sizes that
    // would be the bottleneck. Uploaded segments stay UNROUTED until the commit.
    Try {
      val pushSpec = jobSpec(cfg, part, staging, runId, "SegmentMetadataPush")
      val pusher = new SparkSegmentMetadataPushJobRunner()
      pusher.init(pushSpec)
      pusher.run()

      client.endReplaceSegments(
        FileUploadDownloadClient.getEndReplaceSegmentsURI(
          cfg.controllerUri, cfg.rawTableName, cfg.tableType, entryId))
    } match {
      case Success(_) =>
        log(s"${part.segmentPrefix}: committed $entryId")

      case Failure(err) =>
        // Failing here is safe either way: an uncommitted entry never routes, so
        // the old segments keep serving. Revert explicitly rather than leaving
        // the next run's forceCleanup to reap it.
        log(s"${part.segmentPrefix}: push failed on $entryId — ${err.getMessage}")
        if (cfg.revertOnFailure) {
          Try(client.revertReplaceSegments(
            FileUploadDownloadClient.getRevertReplaceSegmentsURI(
              cfg.controllerUri, cfg.rawTableName, cfg.tableType, entryId,
              /* forceRevert = */ false)))
        }
        throw err
    }
  }

  /** Loads the YAML spec from version control and substitutes this run's values,
    * so one spec serves every partition. */
  private def jobSpec(
      cfg: JobConfig,
      part: PublishReportToPinot.Partition,
      staging: String,
      runId: String,
      jobType: String): SegmentGenerationJobSpec = {

    val values = Map(
      "period"  -> part.period,
      "bucket"  -> f"${part.bucket}%02d",
      "runId"   -> runId,
      "staging" -> staging,
      "jobType" -> jobType
    )
    IngestionJobLauncher.getSegmentGenerationJobSpec(
      cfg.jobSpecFile, /* propertyFile = */ null, values.asJava, null)
  }

  /** Segment names produced by generation = tarball basenames in the staging dir. */
  private def listSegmentTars(spark: SparkSession, staging: String): Seq[String] = {
    val fs = org.apache.hadoop.fs.FileSystem.get(
      new java.net.URI(staging), spark.sparkContext.hadoopConfiguration)
    fs.listStatus(new org.apache.hadoop.fs.Path(staging))
      .map(_.getPath.getName)
      .filter(_.endsWith(".tar.gz"))
      .map(_.stripSuffix(".tar.gz"))
      .sorted
      .toSeq
  }

  private def listExistingSegments(
      client: FileUploadDownloadClient, cfg: JobConfig, prefix: String): Seq[String] =
    client
      .getSegments(cfg.controllerUri, cfg.rawTableName, cfg.tableType,
                   /* excludeReplaced = */ true)
      .asScala.values.asScala.flatMap(_.asScala)
      .filter(_.startsWith(prefix))
      .toSeq
      .sorted

  private def log(msg: String): Unit = println(s"[pinot-swap] $msg")
}

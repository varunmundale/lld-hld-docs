package com.revrec.pinot

import org.yaml.snakeyaml.Yaml

import java.net.URI
import scala.jdk.CollectionConverters._

/**
 * Everything operational, loaded from `publish.yaml` — nothing here should need
 * a recompile to change.
 *
 * Illustrative, not compiled.
 *
 * Note the split against `jobspec.yaml`: Pinot's own `pushJobSpec` (parallelism,
 * retries, `copyToDeepStoreForMetadataPush`) lives there because the ingestion
 * runners read it directly. This file carries the config *our* code acts on.
 */
final case class JobConfig(
    // source
    icebergTable: String,
    lastPublishedSnapshotId: Option[Long],
    // layout
    numBuckets: Int,
    filesPerPartition: Int,
    stagingBase: String,
    // pinot
    controllerUri: URI,
    rawTableName: String,
    tableType: String,
    jobSpecFile: String,
    // swap
    forceCleanup: Boolean,
    revertOnFailure: Boolean,
    transactionScope: String,
    refuseEmptySegmentsTo: Boolean,
    refuseOverlappingLists: Boolean,
    // per-run, from the scheduler rather than the file
    runId: String)

object JobConfig {

  /** `--config publish.yaml --run-id 20260827T1200 [--snapshot-id 123]`
    *
    * runId must be unique per run: it is embedded in every segment name so that
    * `segmentsTo` never collides with the `segmentsFrom` it replaces.
    */
  def parse(args: Array[String]): JobConfig = {
    val a = args.sliding(2, 2).collect { case Array(k, v) => k.stripPrefix("--") -> v }.toMap
    val cfg = load(a("config"))
    cfg.copy(
      runId = a("run-id"),
      lastPublishedSnapshotId =
        a.get("snapshot-id").map(_.toLong).orElse(cfg.lastPublishedSnapshotId))
  }

  private def load(path: String): JobConfig = {
    val root = new Yaml()
      .load[java.util.Map[String, Any]](scala.io.Source.fromFile(path).mkString)
      .asScala

    def section(name: String): Map[String, Any] =
      root(name).asInstanceOf[java.util.Map[String, Any]].asScala.toMap

    val source = section("source")
    val layout = section("layout")
    val pinot  = section("pinot")
    val swap   = section("swap")

    JobConfig(
      icebergTable            = source("icebergTable").toString,
      lastPublishedSnapshotId = Option(source("lastPublishedSnapshotId")).map(_.toString.toLong),
      numBuckets              = layout("numBuckets").asInstanceOf[Int],
      filesPerPartition       = layout("filesPerPartition").asInstanceOf[Int],
      stagingBase             = layout("stagingBase").toString,
      controllerUri           = URI.create(pinot("controllerUri").toString),
      rawTableName            = pinot("rawTableName").toString,
      tableType               = pinot("tableType").toString,
      jobSpecFile             = pinot("jobSpecFile").toString,
      forceCleanup            = swap("forceCleanup").asInstanceOf[Boolean],
      revertOnFailure         = swap("revertOnFailure").asInstanceOf[Boolean],
      transactionScope        = swap("transactionScope").toString,
      refuseEmptySegmentsTo   = swap("refuseEmptySegmentsTo").asInstanceOf[Boolean],
      refuseOverlappingLists  = swap("refuseOverlappingLists").asInstanceOf[Boolean],
      runId                   = "")
  }
}

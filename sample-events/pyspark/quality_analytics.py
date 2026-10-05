"""StreamGuard quality analytics on the S3 validation-result lake.

Usage:
    spark-submit \\
        --packages org.apache.hadoop:hadoop-aws:3.4.1 \\
        quality_analytics.py s3a://streamguard-data/validation-results/

Produces per-event-type counts of violations, broken down by violationType,
plus a daily data-quality score.
"""
import sys

from pyspark.sql import SparkSession, functions as F


def main(input_path: str) -> None:
    spark = (
        SparkSession.builder
        .appName("streamguard-quality-analytics")
        .getOrCreate()
    )

    df = (
        spark.read.json(input_path)
        .select(
            F.col("eventType"),
            F.col("schemaVersion"),
            F.col("timestamp").cast("timestamp").alias("ts"),
            F.explode_outer("violations").alias("violation"),
        )
    )

    total_events = df.groupBy("eventType").agg(F.countDistinct("ts").alias("events"))
    violations = (
        df.filter(F.col("violation").isNotNull())
          .groupBy("eventType", "violation.violationType")
          .count()
    )

    quality = (
        total_events.join(
            violations.groupBy("eventType").agg(F.sum("count").alias("violations")),
            "eventType",
            "left",
        )
        .withColumn("violations", F.coalesce("violations", F.lit(0)))
        .withColumn(
            "qualityScore",
            F.round(
                F.when(F.col("events") == 0, F.lit(100.0))
                 .otherwise(100.0 * (F.col("events") - F.col("violations")) / F.col("events")),
                2,
            ),
        )
    )

    print("---- violations by type ----")
    violations.show(truncate=False)

    print("---- quality score ----")
    quality.show(truncate=False)

    spark.stop()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise SystemExit("usage: quality_analytics.py <s3-or-local-path>")
    main(sys.argv[1])
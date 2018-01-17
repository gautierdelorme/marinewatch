package com.foggly.csvformatter

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SQLContext
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import sys.process._

object CsvFormatter {
  val LatitudeDegrees = 180
  val LongitudeDegrees = 360
  val CellFilter = "0"
  val cellsOutput = "cells.csv"
  val relationshipsOutput = "relationships.csv"
  val HdfsOuputPath = "output"
  val LocalOuputPath = "/workdir/data/output"

  def main(args: Array[String]) {
    val sc = new SparkContext(new SparkConf().setAppName("CSV Formatter"))
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._

    val accuracy = args(0).toInt
    val matrix = sc.textFile(s"input/density-map-from-20160101-to-20161231-$accuracy-probability.csv").zipWithIndex.map{ case (line, index) =>
      (index, line.split(",").zipWithIndex)
    }

    val nodes = matrix.flatMap{ case (row, line) =>
      line.flatMap { case (cell, col) =>
        if (cell != CellFilter) {
          val coeff = accuracy.toDouble
          val lat = LatitudeDegrees/2 - row/coeff
          val long = col/coeff - LongitudeDegrees/2
          Some((s"${row}-${col}", ("%.3f".format(lat), "%.3f".format(long), "Cell")))
        } else
          None
      }
    }

    val relationships = matrix.flatMap{ case (row, line) =>
      line.flatMap { case (cell, col) =>
        neighbors(row, col, accuracy).flatMap { case (n_row, n_col) =>
          if (cell != CellFilter) {
            Some((s"${n_row}-$n_col", (s"${row}-$col", cell)))
          }
          else
            None
        }
      }
    }.join(nodes).groupByKey.flatMap { case (cell, data) =>
      data.map { case ((neighbor, cost), _) =>
        (cell, 1/cost.toDouble, neighbor, "LINKED")
      }
    }

    nodes.map { case (cell, (lat, long, lbl)) =>
      (cell, lat, lat, long, long, lbl)
    }.toDF("id:ID(Cell)", "latitude", "lat:FLOAT", "longitude", "long:FLOAT", ":LABEL")
    .write
    .format("com.databricks.spark.csv")
    .option("header", "true")
    .mode("overwrite")
    .save(s"$HdfsOuputPath/$cellsOutput")

    relationships.toDF(":START_ID(Cell)", "cost:FLOAT", ":END_ID(Cell)", ":TYPE")
    .write
    .format("com.databricks.spark.csv")
    .option("header", "true")
    .mode("overwrite")
    .save(s"$HdfsOuputPath/$relationshipsOutput")

    merge(sc, new Path(s"$HdfsOuputPath/$cellsOutput"), new Path(s"/tmp/$cellsOutput"))
    merge(sc, new Path(s"$HdfsOuputPath/$relationshipsOutput"), new Path(s"/tmp/$relationshipsOutput"))

    // clean duplicated headers after hadoop merging
    Seq("/bin/bash", "-c", s"sed -i -e '1b' -e '/LABEL/d' /tmp/$cellsOutput").!
    Seq("/bin/bash", "-c", s"sed -i -e '1b' -e '/TYPE/d' /tmp/$relationshipsOutput").!
    Seq("/bin/bash", "-c", s"mv /tmp/$relationshipsOutput /tmp/$cellsOutput $LocalOuputPath").!
  }

  def neighbors(i: Long, j: Long, accuracy: Int): Seq[(Long, Long)] = for {
    dx <- -1 to 1
    dy <- -1 to 1
    val n_i = if (i + dy < 0) LatitudeDegrees*accuracy - 1 else if (i + dy > LatitudeDegrees*accuracy - 1) 0 else i + dy
    val n_j = if (j + dx < 0) LongitudeDegrees*accuracy - 1 else if (j + dx > LongitudeDegrees*accuracy - 1) 0 else j + dx
    if (dx | dy) != 0
  } yield (n_i, n_j)

  def merge(sc: SparkContext, srcPath: Path, dstPath: Path): Unit =  {
    val hdfs = FileSystem.get(sc.hadoopConfiguration)
    val lfs = FileSystem.getLocal(sc.hadoopConfiguration)
    if (lfs.exists(dstPath))
      lfs.delete(dstPath);
    FileUtil.copyMerge(hdfs, srcPath, lfs, dstPath, true, sc.hadoopConfiguration, null)
  }
}

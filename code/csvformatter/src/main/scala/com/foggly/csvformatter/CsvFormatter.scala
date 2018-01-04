package com.foggly.csvformatter

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SQLContext

object CsvFormatter {
  val threshold = 0
  val accuracy = 5
  val latitude_degrees = 180
  val longitude_degrees = 360

  def main(args: Array[String]) {
    val sc = new SparkContext(new SparkConf().setAppName("CSV Formatter"))
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._

    val matrix = sc.textFile(s"input/density-map-from-20160101-to-20161231-$accuracy-probability.csv").zipWithIndex.map{ case (line, index) =>
      (index, line.split(",").zipWithIndex)
    }
    val costsMap = matrix.flatMap{ case (row, line) =>
      line.flatMap { case (cell, col) =>
        if (cell.toFloat > threshold)
          Some((s"${row}-${col}", cell.toDouble))
        else
          None
      }
    }.collect().toMap

    matrix.flatMap{ case (row, line) =>
      line.flatMap { case (cell, col) =>
        neighbors(row, col).flatMap { case (n_row, n_col) =>
          val key = s"${n_row}-${n_col}"
          if (cell.toFloat > threshold && costsMap.isDefinedAt(key) && costsMap(key) > threshold) {
            Some((s"${row}-$col", 1/costsMap(key), s"${n_row}-$n_col", "LINKED"))
          }
          else
            None
        }
      }
    }.toDF(":START_ID(Cell)", "cost", ":END_ID(Cell)", ":TYPE")
    .write
    .format("com.databricks.spark.csv")
    .option("header", "true")
    .mode("overwrite")
    .save("file:///workdir/data/output/relationships.csv")

    matrix.flatMap{ case (row, line) =>
      line.flatMap { case (cell, col) =>
        if (cell.toFloat > threshold) {
          val lat = latitude_degrees/2 - row/accuracy.toDouble
          val long = col/accuracy.toDouble - longitude_degrees/2
          Some((s"${row}-${col}", "%.1f".format(lat).toDouble, "%.1f".format(long).toDouble, "Cell"))
        } else
          None
      }
    }.toDF("id:ID(Cell)", "latitude", "longitude", ":LABEL")
    .write
    .format("com.databricks.spark.csv")
    .option("header", "true")
    .mode("overwrite")
    .save("file:///workdir/data/output/cells.csv")
  }

  def neighbors(i: Long, j: Long): Seq[(Long, Long)] = for {
    dx <- -1 to 1
    dy <- -1 to 1
    val n_i = if (i + dy < 0) latitude_degrees*accuracy - 1 else if (i + dy > latitude_degrees*accuracy - 1) 0 else i + dy
    val n_j = if (j + dx < 0) longitude_degrees*accuracy - 1 else if (j + dx > longitude_degrees*accuracy - 1) 0 else j + dx
    if (dx | dy) != 0
  } yield (n_i, n_j)

}

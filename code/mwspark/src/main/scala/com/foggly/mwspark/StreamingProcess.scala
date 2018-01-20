package com.foggly.mwspark

import scala.util.Try
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.neo4j.driver.v1._

object StreamingProcess {
  @transient implicit val formats = DefaultFormats

  def main(args: Array[String]) {
    val ssc = new StreamingContext(new SparkConf().setAppName("MarinewatchStreamingProcess"), Seconds(1))
    val lines = ssc.receiverStream(new HttpReceiver(args(0)))
      .flatMap(parseCoordinates)
      .foreachRDD { rdd =>
        rdd.foreachPartition { partitionOfRecords =>
          val driver = GraphDatabase.driver("bolt://neo4j/7687")
          val session = driver.session
          partitionOfRecords.foreach { case (latitude, longitude, cell) =>
            val cost = 1/cell
            session.run(s"MATCH (cell:Cell{latitude: '$latitude', longitude: '$longitude'})<-[r]-() SET r.cost = $cost")
          }
          session.close()
          driver.close()
        }
      }
    ssc.start()
    ssc.awaitTermination()
  }

  def parseCoordinates(jsonCoordinates: String) = {
    Try {
      val json = parse(jsonCoordinates)
      val latitude = (json \ "latitude").extract[String]
      val longitude = (json \ "longitude").extract[String]
      val cell = (json \ "cell").extract[Double]
      (latitude, longitude, cell)
    }.toOption
  }
}

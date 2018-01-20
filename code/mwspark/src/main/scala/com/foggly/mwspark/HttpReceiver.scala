package com.foggly.mwspark

import java.io._
import java.nio.charset.StandardCharsets
import org.apache.http.HttpEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver

class HttpReceiver(url: String)
  extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) {

  def onStart() {
    new Thread("HTTP Receiver") {
      override def run() { receive() }
    }.start()
  }

  def onStop() {}

  private def receive() {
    var response: CloseableHttpResponse = null
    var userInput: String = null
    try {
      response = HttpClients.createDefault().execute(new HttpGet(url))
      val reader = new BufferedReader(new InputStreamReader(response.getEntity.getContent, StandardCharsets.UTF_8))
      userInput = reader.readLine()
      while(!isStopped && userInput != null) {
        store(userInput)
        userInput = reader.readLine()
      }
      reader.close()
      response.close()
      restart("Trying to connect again")
    } catch {
      case e: java.net.ConnectException =>
        restart("Error connecting to " + url, e)
      case t: Throwable =>
        restart("Error receiving data", t)
    }
  }
}

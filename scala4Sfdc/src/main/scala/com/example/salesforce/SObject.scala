package com.example.salesforce

import java.io.{BufferedWriter, File, FileWriter}

import com.google.gson.{Gson, JsonArray, JsonObject, JsonParser}
import com.typesafe.config.ConfigFactory
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{BasicResponseHandler, HttpClientBuilder}

class SObject(
               sObjectName: String,
               outputPath: String
             )(
               implicit val httpClient: HttpClient,
               implicit val httpResponseHandler: BasicResponseHandler,
               implicit val gson: Gson,
               implicit val jsonParser: JsonParser,
               implicit val utility: Util
             ) {

  private val configuration = ConfigFactory.load("salesforce")
  private val host = configuration.getString("salesforce.InstanceUrl")
  private val dataServiceUrl = configuration.getString("salesforce.DataServiceUrl")

  private val fields = {
    def describe = requestGet(s"$host$dataServiceUrl/sObjects/$sObjectName/describe")

    gson.fromJson(describe, classOf[DescribeResponse]).fields.map(x => x.name)
  }

  private val query = fields.mkString("SELECT+", ",+", s"+FROM+$sObjectName")

  def dumpNewLineDelimitedJson(): Unit = {
    val bufferedWriter = new BufferedWriter(new FileWriter(new File(outputPath)))
    retrieveRecords.forEach(x => {
      bufferedWriter.write(x.toString)
      bufferedWriter.newLine()
    })
    bufferedWriter.close()
  }

  private def retrieveRecords: JsonArray = {
    def executeSOQL(soql: String) = requestGet(s"$host$dataServiceUrl/queryAll/?q=$soql")

    val httpResponse = httpResponseParser(executeSOQL(query))
    val parsedRecords = jsonParser.parse(gson.toJson(httpResponse.get("records"))).getAsJsonArray
    parsePaginatedResponses(httpResponse, parsedRecords)
  }

  private def parsePaginatedResponses(httpResponse: JsonObject, parsedRecords: JsonArray): JsonArray = {
    if (!httpResponse.get("done").getAsBoolean) {
      def getNextPaginatedResponse(identifier: String) = httpResponseParser(requestGet(host + identifier))

      val paginatedResponse = getNextPaginatedResponse(httpResponse.get("nextRecordsUrl").getAsString)
      parsedRecords.addAll(parsePaginatedResponses(paginatedResponse, parsedRecords))
    }
    parsedRecords
  }

  private def requestGet(url: String) = {
    val request = new HttpGet(url)
    request.addHeader("Authorization", "Bearer " + utility.getAccessToken)
    request.addHeader("Content-Type", "application/json")
    httpResponseHandler.handleResponse(httpClient.execute(request))
  }

  private def httpResponseParser(response: String) =
    jsonParser.parse(gson.toJson(gson.fromJson(response, classOf[Response]))).getAsJsonObject
}

object SObject {
  implicit val httpClient: HttpClient = HttpClientBuilder.create().build()
  implicit val httpResponseHandler: BasicResponseHandler = new BasicResponseHandler()
  implicit val gson: Gson = new Gson()
  implicit val jsonParser: JsonParser = new JsonParser()
  implicit val utility: Util = new Util()

  def apply(sObjectName: String, outputPath: String) = new SObject(sObjectName, outputPath)
}
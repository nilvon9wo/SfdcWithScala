package com.example.salesforce

import com.google.gson.{Gson, JsonArray, JsonParser}
import java.io.{BufferedWriter, File, FileWriter}
import java.util.List

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import com.typesafe.config.ConfigFactory
import jdk.nashorn.internal.parser.JSONParser

class SObject(
               sObjectName: String,
               outputPath: String
             )(
               implicit httpClient: DefaultHttpClient,
               implicit httpResponseHandler: BasicResponseHandler,
               implicit gson: Gson,
               implicit jsonParser: JSONParser,
               implicit utility: Util
             ) {
  val accessToken = util.getAccessToken()

  val configuration = ConfigFactory.load("salesforce")
  val host = configuration.getString("force.InstanceUrl")
  val dataServiceUrl = configuration.getString("force.DataServiceUrl")

  val fields = gson.fromJson(describe, classOf[DescribeResponse]).fields.map(_ => _.name)
  val query = fields.mkString("SELECT+", ",+", s"+FROM+$sObjectName")

  def requestGet(url: String) = {
    val request = new HttpGet(url)
    request.addHeader("Authorization", "Bearer " + accessToken)
    request.addHeader("Content-Type", "application/json")
    httpResponseHandler.handleResponse(httpClient.execute(request))
  }

  def getPaginated(identifier: String) = httpResponseParser(requestGet(host + identifier))

  def httpResponseParser(response: String) =
    parser.parse(gson.toJson(gson.fromJson(response, classOf[Response]))).getAsJsonObject

  def describe = requestGet(s"${host}${dataServiceUrl}/sObjects/$sObjectName/describe")

  def retrieveRecords = {
    val httpResponse = httpResponseParser(executeSOQL(query))
    val allRecords = parser.parse(gson.toJson(httpResponse.get("records"))).getAsJsonArray

    var done = httpResponse.get("done").getAsBoolean
    while (done == false) {
      val paginatedResponse = getPaginated(httpResponse.get("nextRecordsUrl").getAsString)
      val parsedRecords = jsonParser.parse(gson.toJson(paginatedResponse.get("records"))).getAsJsonArray
      allRecords.addAll(parsedRecords)
      done = parsedRecords.get("done").getAsBoolean
    }
    allRecords
  }

  def executeSOQL(soql: String) = requestGet(s"$host$dataServiceUrl/queryAll/?q=$soql")

  def dumpNewLineDelimitedJson = {
    val bufferedWriter = new BufferedWriter(new FileWriter(new File(outputPath)))
    retrieveRecords.forEach(x => {
      bufferedWriter.write(x.toString)
      bufferedWriter.newLine()
    })
    bufferedWriter.close()
  }

}

object SObject {
  implicit val httpClient = new DefaultHttpClient()
  implicit val httpResponseHandler = new BasicResponseHandler()
  implicit val gson = new Gson()
  implicit val jsonParser = new JSONParser()
  implicit val utility = new Util()

  def apply(sObjectName: String, outputPath: String) = new SObject(sObjectName, outputPath)
}
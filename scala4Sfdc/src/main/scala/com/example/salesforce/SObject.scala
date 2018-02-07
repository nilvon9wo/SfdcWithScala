package com.example.salesforce

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
  private val dataServiceUrl = configuration.getString("salesforce.DataServiceUrl")

  def retrieveRecords: JsonArray = {
    val describe = requestGet(s"$dataServiceUrl/sobjects/$sObjectName/describe")
    val query = gson.fromJson(describe, classOf[DescribeResponse])
      .fields.map(x => x.name)
      .mkString("SELECT+", ",+", s"+FROM+$sObjectName")

    val httpResponse = httpResponseParser(requestGet(s"$dataServiceUrl/queryAll/?q=$query"))
    val parsedRecords = jsonParser.parse(gson.toJson(httpResponse.get("records"))).getAsJsonArray
    parsePaginatedResponses(httpResponse, parsedRecords)
  }

  private def parsePaginatedResponses(httpResponse: JsonObject, parsedRecords: JsonArray): JsonArray = {
    if (!httpResponse.get("done").getAsBoolean) {
      val paginatedResponse = httpResponseParser(requestGet(httpResponse.get("nextRecordsUrl").getAsString))
      parsedRecords.addAll(parsePaginatedResponses(paginatedResponse, parsedRecords))
    }
    parsedRecords
  }

  private def requestGet(path: String) = {
    val token = utility.getAccessToken
    val request = new HttpGet(token.instance_url + path)
    request.addHeader("Authorization", "Bearer " + token.access_token)
    request.addHeader("Content-type", "application/json")
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
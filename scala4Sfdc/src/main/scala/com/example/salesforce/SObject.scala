package com.example.salesforce

import com.google.gson.{Gson, JsonArray, JsonObject, JsonParser}
import com.typesafe.config.ConfigFactory
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{BasicResponseHandler, HttpClientBuilder}

class SObject(
               sObjectName: String
             )(
               implicit val gson: Gson,
               implicit val httpClient: HttpClient,
               implicit val httpResponseHandler: BasicResponseHandler,
               implicit val jsonParser: JsonParser,
               implicit val utility: Utility
             ) {

  private val configuration = ConfigFactory.load("salesforce")
  private val dataServiceUrl = configuration.getString("salesforce.DataServiceUrl")

  def retrieveRecords: JsonArray = {
    val describe = requestGet(s"$dataServiceUrl/sobjects/$sObjectName/describe")
    val query = gson.fromJson(describe, classOf[DescribeResponse])
      .fields.map(x => x.name)
      .mkString("SELECT+", ",+", s"+FROM+$sObjectName")

    val initialResponse = convertToJsonObject(requestGet(s"$dataServiceUrl/queryAll/?q=$query"))
    val parsedRecords = jsonParser.parse(gson.toJson(initialResponse.get("records"))).getAsJsonArray
    parseNextResponse(initialResponse, parsedRecords)
  }

  private def parseNextResponse(lastResponse: JsonObject, parsedRecords: JsonArray): JsonArray = {
    if (!lastResponse.get("done").getAsBoolean) {
      val nextResponse = convertToJsonObject(requestGet(lastResponse.get("nextRecordsUrl").getAsString))
      parsedRecords.addAll(parseNextResponse(nextResponse, parsedRecords))
    }
    parsedRecords
  }

  private def convertToJsonObject(response: String) =
    jsonParser.parse(gson.toJson(gson.fromJson(response, classOf[Response]))).getAsJsonObject

  private def requestGet(path: String) = {
    val token = utility.getAccessToken
    val request = new HttpGet(token.instance_url + path)
    request.addHeader("Authorization", "Bearer " + token.access_token)
    request.addHeader("Content-type", "application/json")
    httpResponseHandler.handleResponse(httpClient.execute(request))
  }
}

object SObject {
  implicit val gson: Gson = new Gson()
  implicit val httpClient: HttpClient = HttpClientBuilder.create().build()
  implicit val httpResponseHandler: BasicResponseHandler = new BasicResponseHandler()
  implicit val jsonParser: JsonParser = new JsonParser()
  implicit val utility: Utility = new Utility()

  def apply(sObjectName: String) = new SObject(sObjectName)
}
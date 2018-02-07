package com.example.salesforce

import com.google.gson.{Gson, JsonArray, JsonObject, JsonParser}
import com.typesafe.config.ConfigFactory
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{BasicResponseHandler, HttpClientBuilder}

import scala.util.{Failure, Success, Try}

class HttpSObjectRetriever(
               sObjectName: String
             )(
               implicit val gson: Gson,
               implicit val httpClient: HttpClient,
               implicit val httpResponseHandler: BasicResponseHandler,
               implicit val jsonParser: JsonParser,
               implicit val utility: SfdcAuthorizer
             ) {

  private val configuration = ConfigFactory.load("salesforce")
  private val dataServiceUrl = configuration.getString("salesforce.DataServiceUrl")

  def retrieveRecords: JsonArray = {
    val describe: String = getResponseFor(s"$dataServiceUrl/sobjects/$sObjectName/describe")
    val query = gson.fromJson(describe, classOf[DescribeResponse])
      .fields.map(x => x.name)
      .mkString("SELECT+", ",+", s"+FROM+$sObjectName")

    val initialResponse = convertToSObjectResponse(getResponseFor(s"$dataServiceUrl/queryAll/?q=$query"))
    val parsedRecords = jsonParser.parse(gson.toJson(initialResponse.records)).getAsJsonArray
    parseNextResponse(initialResponse, parsedRecords)
  }

  private def parseNextResponse(lastResponse: SObjectResponse, parsedRecords: JsonArray): JsonArray = {
    if (!lastResponse.done) {
      val nextResponse = convertToSObjectResponse(getResponseFor(lastResponse.nextRecordsUrl))
      parsedRecords.addAll(parseNextResponse(nextResponse, parsedRecords))
    }
    parsedRecords
  }

  private def convertToSObjectResponse(response: String) = gson.fromJson(response, classOf[SObjectResponse])

  private def getResponseFor(path: String): String = {
    val token = utility.getAccessToken
    val httpGet = new HttpGet(token.instance_url + path)
    httpGet.addHeader("Authorization", "Bearer " + token.access_token)
    httpGet.addHeader("Content-type", "application/json")
    getResponseFor(httpGet: HttpGet)
  }

  private def getResponseFor(httpGet: HttpGet): String = {
    println(s"httpGet: $httpGet")
    Try(httpClient.execute(httpGet)) match {
      case Success(response: HttpResponse) => getResponseFor(response: HttpResponse)
      case Failure(throwable) => throw new HttpSObjectRetrieverException(throwable)
    }
  }

  private def getResponseFor(httpReponse: HttpResponse): String = {
    Try(httpResponseHandler.handleResponse(httpReponse)) match {
      case Success(response: String) => response
      case Failure(throwable) => throw new HttpSObjectRetrieverException(throwable)
    }
  }
}

object HttpSObjectRetriever {
  implicit val gson: Gson = new Gson()
  implicit val httpClient: HttpClient = HttpClientBuilder.create().build()
  implicit val httpResponseHandler: BasicResponseHandler = new BasicResponseHandler()
  implicit val jsonParser: JsonParser = new JsonParser()
  implicit val utility: SfdcAuthorizer = new SfdcAuthorizer()

  def apply(sObjectName: String) = new HttpSObjectRetriever(sObjectName)
}
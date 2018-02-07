package com.example.salesforce

import com.google.gson.Gson
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.{BasicResponseHandler, HttpClientBuilder}

class Util(
            implicit val httpClient: HttpClient,
            implicit val httpResponseHandler: BasicResponseHandler,
            implicit val gson: Gson
          ) {
  private val loginUrl = "https://login.salesforce.com"
  private val grantService = "/services/oauth2/token?grant_type=password"
  private val configuration: Config = ConfigFactory.load("salesforce")

  def getAccessToken: String = {
    val httpPost = new HttpPost(s"$loginUrl$grantService" +
      createUrlParam("client_id", "ConsumerKey") +
      createUrlParam("client_secret", "ConsumerSecret") +
      createUrlParam("username", "Username") +
      createUrlParam("password", "Password")
    )

    try {
      gson.fromJson(httpResponseHandler.handleResponse(httpClient.execute(httpPost)), classOf[Token])
        .access_token
    }
    catch {
      case _: java.io.IOException => ""
      case _: java.net.SocketTimeoutException => ""
    }
  }

  private def createUrlParam(paramName: String, propertyName: String): String = {
    s"&$paramName=${configuration.getString(s"salesforce.$propertyName")}"
  }
}

object Util {
  private implicit val httpClient: HttpClient = HttpClientBuilder.create().build()
  private implicit val httpResponseHandler: BasicResponseHandler = new BasicResponseHandler()
  private implicit val gson: Gson = new Gson()

  def apply = new Util()
}


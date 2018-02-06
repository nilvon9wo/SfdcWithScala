package com.example.salesforce

import com.google.gson.Gson
import com.typesafe.config.ConfigFactory
import java.io._
import org.apache.commons._
import org.apache.http._
import org.apache.http.client._
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.client.ResponseHandler

class Util()(
  implicit httpClient: DefaultHttpClient,
  implicit httpResponseHandler: BasicResponseHandler,
  implicit gson: Gson
) {
  val loginUrl = "https://login.salesforce.com"
  val grantService = "/services/oauth2/token?grant_type=password"

  def getAccessToken() = {
    val configuration = ConfigFactory.load("salesforce")
    val httpPost = new HttpPost(s"$loginUrl$grantService" +
      s"&client_id=${
        configuration.getString("salesforce.ConsumerKey")
      }" +
      s"&client_secret=${
        configuration.getString("salesforce.ConsumerSecret")
      }" +
      s"&username=${
        configuration.getString("salesforce.Username")
      }" +
      s"&password=${
        configuration.getString("salesforce.Password")
      }")

    try {
      gson.fromJson(httpResponseHandler.handleResponse(httpClient.execute(httpPost)), classOf[Token])
          .access_token
    }
    catch {
      case _: java.io.IOException => ""
      case _: java.net.SocketTimeoutException => ""
    }
  }
}

object Util {
  implicit val httpClient = new DefaultHttpClient()
  implicit val httpResponseHandler = new BasicResponseHandler()
  implicit val gson = new Gson()

  def apply = new Util()
}


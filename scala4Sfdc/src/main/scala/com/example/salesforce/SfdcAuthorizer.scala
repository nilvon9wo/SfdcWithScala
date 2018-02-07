package com.example.salesforce

import com.google.gson.Gson
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.{BasicResponseHandler, HttpClientBuilder}

import scala.util.{Failure, Success, Try}

class SfdcAuthorizer(
                      implicit val httpClient: HttpClient,
                      implicit val httpResponseHandler: BasicResponseHandler,
                      implicit val gson: Gson
                    ) {
  private val loginUrl = "https://login.salesforce.com"
  private val grantService = "/services/oauth2/token?grant_type=password"
  private val configuration: Config = ConfigFactory.load("salesforce")

  lazy val getAccessToken: SfdcAuthorizationToken = {
    def createUrlParam(paramName: String, propertyName: String): String = {
      s"&$paramName=${configuration.getString(s"salesforce.$propertyName")}"
    }

    getAccessToken(new HttpPost(s"$loginUrl$grantService" +
      createUrlParam("client_id", "ConsumerKey") +
      createUrlParam("client_secret", "ConsumerSecret") +
      createUrlParam("username", "Username") +
      createUrlParam("password", "Password")
    ))
  }

  private def getAccessToken(httpPost: HttpPost): SfdcAuthorizationToken = {
    println(httpPost)
    Try(httpClient.execute(httpPost)) match {
      case Success(response: HttpResponse) => getAccessToken(response)
      case Failure(throwable) => throw new SfdcAuthorizerException(throwable)
    }
  }

  private def getAccessToken(httpResponse: HttpResponse): SfdcAuthorizationToken = {
    println(httpResponse)
    Try(httpResponseHandler.handleResponse(httpResponse)) match {
      case Success(response: String) => gson.fromJson(response, classOf[SfdcAuthorizationToken])
      case Failure(throwable) => throw new SfdcAuthorizerException(throwable)
    }
  }
}

object SfdcAuthorizer {
  private implicit val httpClient: HttpClient = HttpClientBuilder.create().build()
  private implicit val httpResponseHandler: BasicResponseHandler = new BasicResponseHandler()
  private implicit val gson: Gson = new Gson()

  def apply = new SfdcAuthorizer()
}


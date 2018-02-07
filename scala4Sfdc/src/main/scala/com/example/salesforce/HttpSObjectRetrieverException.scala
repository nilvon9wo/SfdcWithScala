package com.example.salesforce

class HttpSObjectRetrieverException(throwable: Throwable) extends Exception {
  println(s"ERROR: $throwable")

}

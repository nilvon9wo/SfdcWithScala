package com.example.salesforce

class SObjectRetrieverException(throwable: Throwable) extends Exception {
  println(s"ERROR: $throwable")

}

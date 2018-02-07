package com.example.salesforce

class SfdcAuthorizerException(throwable: Throwable) extends Exception {
  println(s"ERROR: $throwable")

}


package com.example.salesforce

class UtilException(throwable: Throwable) extends Exception {
  println(s"ERROR: $throwable")

}


package com.example.salesforce

class UtilityException(throwable: Throwable) extends Exception {
  println(s"ERROR: $throwable")

}


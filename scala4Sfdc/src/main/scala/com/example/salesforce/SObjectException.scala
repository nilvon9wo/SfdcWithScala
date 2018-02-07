package com.example.salesforce

class SObjectException(throwable: Throwable) extends Exception {
  println(s"ERROR: $throwable")

}

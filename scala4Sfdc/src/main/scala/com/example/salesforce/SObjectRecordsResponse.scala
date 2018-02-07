package com.example.salesforce

case class SObjectRecordsResponse(
                     totalSize: Int,
                     done: Boolean,
                     nextRecordsUrl: String,
                     records: Array[Any]
                   )
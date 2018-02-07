package com.example.salesforce

case class SObjectResponse(
                     totalSize: Int,
                     done: Boolean,
                     nextRecordsUrl: String,
                     records: Array[Any]
                   )
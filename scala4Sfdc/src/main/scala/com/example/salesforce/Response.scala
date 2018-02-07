package com.example.salesforce

case class Response (
                    totalSize: Int,
                    done: Boolean,
                    nextRecordsUrl: String,
                    records: Array[Any]
                    )
package com.example.salesforce

object ObjectExporter {
  def main(args: Array[String]): Unit = {
    SObject.apply(sObjectName = args(1), outputPath = args(2))
      .dumpNewLineDelimitedJson
  }
}
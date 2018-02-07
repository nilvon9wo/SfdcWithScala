package com.example.salesforce

import java.io.{BufferedWriter, File, FileWriter}

import com.google.gson.JsonArray

object ObjectExporter {
  def main(args: Array[String]): Unit = {
    val sObjectName = valueOrDefault(args, 1, "Account")
    val outputPath = valueOrDefault(args, 2, "target/Account.json")
    val retrievedRecords = SObject.apply(sObjectName, outputPath).retrieveRecords
    dumpNewLineDelimitedJson(retrievedRecords, outputPath)
  }

  private def dumpNewLineDelimitedJson(retrievedRecords: JsonArray, outputPath: String): Unit = {
    val bufferedWriter = new BufferedWriter(new FileWriter(new File(outputPath)))
    retrievedRecords.forEach(x => {
      bufferedWriter.write(x.toString)
      bufferedWriter.newLine()
    })
    bufferedWriter.close()
  }

  private def valueOrDefault(args: Array[String], index: Integer, default: String) = {
    val value = if (!args.isEmpty) {
      args(index)
    }
    Option(value) match {
      case Some(v: String) if !v.isEmpty => v
      case _ => default
    }
  }
}
package com.example.salesforce

object ObjectExporter {
  def main(args: Array[String]): Unit = {
    val sObjectName = valueOrDefault(args, 1, "Account")
    val outputPath = valueOrDefault(args, 2, "Account.json")
    SObject.apply(sObjectName, outputPath).dumpNewLineDelimitedJson()
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
package com.example.salesforce

case class Token(
                  access_token: String,
                  id: String,
                  instance_url: String,
                  issued_at: String,
                  signature: String,
                  token_type: String
                )
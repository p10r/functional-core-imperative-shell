package com.example.fcis

interface EmailSystem {
    fun send(email: Email): String
}

data class Email(
    val recipient: String,
    val topic: String,
    val body: String
)

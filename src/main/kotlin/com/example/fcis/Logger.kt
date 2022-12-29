package com.example.fcis

import java.time.Clock

fun log(message: String) = println("${Clock.systemUTC().instant()} $message")

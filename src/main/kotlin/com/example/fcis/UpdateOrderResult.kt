package com.example.fcis

sealed interface UpdateOrderResult {
    val eventName: String
}

data class SuccessfullyUpdated(
    val updatedOrder: Order,
    val email: Email?,
    override val eventName: String,
) : UpdateOrderResult

data class OutdatedUpdate(
    val updateId: String,
    val orderId: String,
    override val eventName: String,
) : UpdateOrderResult

data class UnknownUpdate(
    val updateId: String,
    val orderId: String,
    override val eventName: String,
) : UpdateOrderResult

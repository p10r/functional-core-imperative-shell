package com.example.fcis

interface OrderRepository {
    fun findByIdOrNull(orderId: String): Order?
    fun update(order: Order): Order?
}

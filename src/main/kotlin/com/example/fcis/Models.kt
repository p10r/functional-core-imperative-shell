package com.example.fcis

import java.time.Instant

data class Order(
    val id: String,
    val orderId: String,
    val customer: Customer,
    val currentStatusDetails: StatusDetails,
) {
    data class Customer(
        val customerId: String,
        val emailAddress: String,
        val emailNotificationsEnabled: Boolean,
    )

    data class StatusDetails(
        val currentStatus: Status,
        val updatedAt: Instant
    )

    @Suppress("unused")
    enum class Status {
        RECEIVED, IN_PROGRESS, DISPATCHED, DELIVERED
    }
}

data class DeliveryUpdate(
    val id: String,
    val orderId: String,
    val statusUpdatedAt: Instant,
    val newStatus: Order.Status
)

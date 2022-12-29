package com.example.fcis

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed interface UpdateOrderResult

data class SuccessfullyUpdated(
    val updatedOrder: Order,
    val email: Email,
    val eventName: String,
) : UpdateOrderResult

data class OutdatedUpdate(
    val updateId: String,
    val orderId: String,
    val eventName: String,
) : UpdateOrderResult

fun Order.process(
    deliveryUpdate: DeliveryUpdate,
    now: () -> Instant = Instant::now
): UpdateOrderResult {
    if (deliveryUpdate.isOlderThan(currentStatusDetails)) {
        return OutdatedUpdate(
            updateId = deliveryUpdate.id,
            orderId = orderId,
            eventName = "updates.outdated"
        )
    }

    return SuccessfullyUpdated(
        updatedOrder = this.updateWith(deliveryUpdate, now),
        email = statusUpdateEmailOf(this),
        eventName = "updates.successful"
    )
}

private fun DeliveryUpdate.isOlderThan(currentStatusDetails: Order.StatusDetails): Boolean =
    statusUpdatedAt.toUTC().isBefore(currentStatusDetails.updatedAt.toUTC())

private fun Instant.toUTC(): LocalDateTime =
    atOffset(ZoneOffset.UTC).toLocalDateTime()

private fun statusUpdateEmailOf(order: Order) = with(order) {
    Email(
        recipient = customer.emailAddress,
        topic = "Your order $orderId has changed it's status!",
        body = """
        Hi,
        Your order $orderId has changed it's status to ${currentStatusDetails.currentStatus}.
        Check the website for more info.
        """.trimIndent()
    )
}

private fun Order.updateWith(deliveryUpdate: DeliveryUpdate, now: () -> Instant) = copy(
    currentStatusDetails = Order.StatusDetails(
        currentStatus = deliveryUpdate.newStatus,
        updatedAt = now()
    )
)

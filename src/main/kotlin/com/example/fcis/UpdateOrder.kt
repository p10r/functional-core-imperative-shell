package com.example.fcis

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Order?.process(
    deliveryUpdate: DeliveryUpdate,
    now: () -> Instant = Instant::now
): UpdateOrderResult = when {
    this == null -> {
        UnknownUpdate(
            updateId = deliveryUpdate.id,
            orderId = deliveryUpdate.orderId,
            eventName = "updates.unknown"
        )
    }

    deliveryUpdate.isOlderThan(currentStatusDetails) -> {
        OutdatedUpdate(
            updateId = deliveryUpdate.id,
            orderId = orderId,
            eventName = "updates.outdated"
        )
    }

    else -> SuccessfullyUpdated(
        updatedOrder = this.updateWith(deliveryUpdate, now),
        email = statusUpdateEmailOf(this),
        eventName = "updates.successful"
    )
}

private fun DeliveryUpdate.isOlderThan(currentStatusDetails: Order.StatusDetails): Boolean =
    statusUpdatedAt.toUTC().isBefore(currentStatusDetails.updatedAt.toUTC())

private fun Instant.toUTC(): LocalDateTime =
    atOffset(ZoneOffset.UTC).toLocalDateTime()

private fun statusUpdateEmailOf(order: Order): Email? = with(order) {
    if (order.customer.emailNotificationsEnabled)
        Email(
            recipient = customer.emailAddress,
            topic = "Your order $orderId has changed it's status!",
            body = """
                    Hi,
                    Your order $orderId has changed it's status to ${currentStatusDetails.currentStatus}.
                    Check the website for more info.
                    """.trimIndent()
        )
    else null
}

private fun Order.updateWith(deliveryUpdate: DeliveryUpdate, now: () -> Instant) = copy(
    currentStatusDetails = Order.StatusDetails(
        currentStatus = deliveryUpdate.newStatus,
        updatedAt = now()
    )
)

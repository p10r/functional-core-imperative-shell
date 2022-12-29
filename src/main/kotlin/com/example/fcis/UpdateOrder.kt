package com.example.fcis

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed interface UpdateOrderResult

object Ok: UpdateOrderResult

data class OutdatedUpdate(
    val updateId: String,
    val orderId: String,
    val eventName: String,
): UpdateOrderResult

fun Order.process(deliveryUpdate: DeliveryUpdate): UpdateOrderResult {
    if (deliveryUpdate.isOlderThan(currentStatusDetails)) {
        return OutdatedUpdate(
            updateId = deliveryUpdate.id,
            orderId = orderId,
            eventName = "updates.outdated"
        )
    }

    return Ok
}

private fun DeliveryUpdate.isOlderThan(currentStatusDetails: Order.StatusDetails): Boolean =
    statusUpdatedAt.toUTC().isBefore(currentStatusDetails.updatedAt.toUTC())

private fun Instant.toUTC(): LocalDateTime =
    atOffset(ZoneOffset.UTC).toLocalDateTime()

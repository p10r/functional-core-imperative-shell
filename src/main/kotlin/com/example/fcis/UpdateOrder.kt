package com.example.fcis

import com.example.fcis.Order.Status.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed interface UpdateResult
data class UnknownOrder(val event: Event) : UpdateResult
data class UpdateIgnored(val event: Event) : UpdateResult
data class OutdatedUpdate(val event: Event) : UpdateResult
data class SuccessfulUpdate(
    val updatedOrder: Order,
    val email: Email?,
    val event: Event
) : UpdateResult

data class Event(
    val name: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
)

fun Order?.update(
    deliveryUpdate: DeliveryUpdate,
    now: Instant
): UpdateResult {
    if (this == null)
        return UnknownOrder(unknownOrderEvent(deliveryUpdate))

    if (deliveryUpdate.isOlderThan(currentStatusDetails))
        return OutdatedUpdate(outdatedEvent(deliveryUpdate))

    if (deliveryUpdate.newStatus == RECEIVED || deliveryUpdate.newStatus == RECEIVED_REQUEST)
        return UpdateIgnored(ignoredEvent(deliveryUpdate))

    val updated = updateWith(deliveryUpdate, now)

    return if (customer.emailNotificationsEnabled) {
        SuccessfulUpdate(
            updatedOrder = updated,
            email = statusUpdateEmailOf(this),
            event = successEvent(deliveryUpdate)
        )
    } else {
        SuccessfulUpdate(
            updatedOrder = updated,
            email = null,
            event = successEvent(deliveryUpdate)
        )
    }
}

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

private fun DeliveryUpdate.isOlderThan(currentStatusDetails: Order.StatusDetails): Boolean =
    statusUpdatedAt.toUTC().isBefore(currentStatusDetails.updatedAt.toUTC())

private fun Instant.toUTC(): LocalDateTime =
    atOffset(ZoneOffset.UTC).toLocalDateTime()

private fun Order.updateWith(deliveryUpdate: DeliveryUpdate, now: Instant) = copy(
    currentStatusDetails = Order.StatusDetails(
        currentStatus = deliveryUpdate.newStatus,
        updatedAt = now
    )
)

private fun successEvent(deliveryUpdate: DeliveryUpdate) = Event(
    name = "updates.successful",
    message = "Processed update ${deliveryUpdate.id}.",
)

private fun ignoredEvent(deliveryUpdate: DeliveryUpdate) = Event(
    name = "updates.ignored",
    message = "Update ${deliveryUpdate.id} ignored, status was ${deliveryUpdate.newStatus}.",
)

private fun outdatedEvent(deliveryUpdate: DeliveryUpdate) = Event(
    name = "updates.outdated",
    message = "Incoming update ${deliveryUpdate.id} is outdated! Ignoring it.",
)

private fun unknownOrderEvent(deliveryUpdate: DeliveryUpdate) = Event(
    name = "updates.unknown",
    message = "No order with id ${deliveryUpdate.orderId} found!",
)

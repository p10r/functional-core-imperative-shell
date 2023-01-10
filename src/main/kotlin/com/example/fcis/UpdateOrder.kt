package com.example.fcis

import com.example.fcis.Order.Status.*
import java.time.Instant

sealed interface UpdateResult
object UnknownOrder : UpdateResult
object UpdateIgnored : UpdateResult
data class SuccessfulUpdate(
    val updatedOrder: Order,
    val email: Email?
) : UpdateResult

fun update(
    order: Order?,
    deliveryUpdate: DeliveryUpdate,
    now: Instant
): UpdateResult {
    if (order == null)
        return UnknownOrder

    if (deliveryUpdate.newStatus == RECEIVED || deliveryUpdate.newStatus == RECEIVED_REQUEST)
        return UpdateIgnored

    val updated = order.updateWith(deliveryUpdate, now)

    return if (order.customer.emailNotificationsEnabled) {
        SuccessfulUpdate(updated, statusUpdateEmailOf(order))
    } else
        SuccessfulUpdate(updated, email = null)
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

private fun Order.updateWith(deliveryUpdate: DeliveryUpdate, now: Instant) = copy(
    currentStatusDetails = Order.StatusDetails(
        currentStatus = deliveryUpdate.newStatus,
        updatedAt = now
    )
)

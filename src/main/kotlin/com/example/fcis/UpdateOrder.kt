package com.example.fcis

sealed interface UpdateResult
object UnknownOrder : UpdateResult
data class SuccessfulUpdate(val email: Email?) : UpdateResult

fun update(
    order: Order?,
    deliveryUpdate: DeliveryUpdate
): UpdateResult {
    if (order == null) return UnknownOrder

    return if (order.customer.emailNotificationsEnabled)
        SuccessfulUpdate(statusUpdateEmailOf(order))
    else
        SuccessfulUpdate(null)
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

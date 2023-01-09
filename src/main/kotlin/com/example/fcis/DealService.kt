package com.example.fcis

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class DealService(
    private val orderRepository: OrderRepository,
    private val emailSystem: EmailSystem,
    private val meterRegistry: MeterRegistry,
    private val now: () -> Instant = Instant::now
) {
    fun process(deliveryUpdate: DeliveryUpdate) {
        val order = orderRepository.findByIdOrNull(deliveryUpdate.orderId)

        if (order == null) {
            log("No order with id ${deliveryUpdate.orderId} in database!")
            monitor("updates.unknown")
            return
        }

        if (deliveryUpdate.isOlderThan(order.currentStatusDetails)) {
            log("Incoming update ${deliveryUpdate.id} is outdated! Ignoring it.")
            monitor("updates.outdated")
            return
        }

        orderRepository.update(order.updateWith(deliveryUpdate, now))

        if (order.customer.emailNotificationsEnabled) {
            emailSystem.send(statusUpdateEmailOf(order))
        }

        monitor("updates.successful")
        log("Processed update ${deliveryUpdate.id}")
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


    private fun monitor(eventName: String) {
        Counter.builder(eventName)
            .register(meterRegistry)
            .increment()
    }
}

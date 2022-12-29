package com.example.fcis

import com.example.fcis.Order.StatusDetails
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class DeliveryUpdateProcessor(
    private val orderRepository: OrderRepository,
    private val emailSystem: EmailSystem,
    private val meterRegistry: MeterRegistry = Metrics.globalRegistry,
    private val now: () -> Instant = Instant::now
) {
    fun process(deliveryUpdate: DeliveryUpdate) {
        val order = orderRepository.findByIdOrNull(deliveryUpdate.orderId)

        if (order == null) {
            monitorUnknownUpdate()
            log("No order with id ${deliveryUpdate.orderId} in database!")
            return
        }

        if (deliveryUpdate.isOlderThan(order.currentStatusDetails)) {
            monitorOutdatedUpdate()
            log("Incoming update ${deliveryUpdate.id} is outdated! Ignoring it.")
            return
        }

        order.updateWith(deliveryUpdate)

        if (order.customer.emailNotificationsEnabled) {
            emailSystem.send(statusUpdateEmailOf(order))
        }

        log("Processed update ${deliveryUpdate.id}")
        monitorSuccessfulUpdate()
    }

    private fun Order.updateWith(deliveryUpdate: DeliveryUpdate) = copy(
        currentStatusDetails = StatusDetails(
            currentStatus = deliveryUpdate.newStatus,
            updatedAt = this@DeliveryUpdateProcessor.now()
        )
    ).let { orderRepository.update(it) }

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

    private fun DeliveryUpdate.isOlderThan(currentStatusDetails: StatusDetails): Boolean =
        statusUpdatedAt.toUTC().isBefore(currentStatusDetails.updatedAt.toUTC())

    private fun Instant.toUTC(): LocalDateTime =
        atOffset(ZoneOffset.UTC).toLocalDateTime()

    private fun monitorUnknownUpdate() {
        monitor("updates.unknown")
    }

    private fun monitorOutdatedUpdate() {
        monitor("updates.outdated")
    }

    private fun monitorSuccessfulUpdate() {
        monitor("updates.successful")
    }

    private fun monitor(eventName: String) {
        Counter.builder(eventName)
            .register(meterRegistry)
            .increment()
    }
}

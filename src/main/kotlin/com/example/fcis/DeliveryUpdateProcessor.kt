package com.example.fcis

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import java.time.Instant

class DeliveryUpdateProcessor(
    private val orderRepository: OrderRepository,
    private val emailSystem: EmailSystem,
    private val meterRegistry: MeterRegistry = Metrics.globalRegistry,
    private val now: () -> Instant = Instant::now
) {
    fun process(deliveryUpdate: DeliveryUpdate) {
        val order = orderRepository.findByIdOrNull(deliveryUpdate.orderId)

        when (val result = order.process(deliveryUpdate, now)) {
            is UnknownUpdate       -> {
                monitor(result.eventName)
                log("No order with id ${deliveryUpdate.orderId} in database!")
            }

            is OutdatedUpdate      -> {
                monitor(result.eventName)
                log("Incoming update ${deliveryUpdate.id} is outdated! Ignoring it.")
            }

            is SuccessfullyUpdated -> {
                log("Processed update ${deliveryUpdate.id}")
                monitor(result.eventName)

                orderRepository.update(result.updatedOrder)

                if (order!!.customer.emailNotificationsEnabled) {
                    emailSystem.send(result.email)
                }
            }
        }
    }

    private fun monitor(eventName: String) {
        Counter.builder(eventName)
            .register(meterRegistry)
            .increment()
    }
}

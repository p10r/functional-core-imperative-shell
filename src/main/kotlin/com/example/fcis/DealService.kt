package com.example.fcis

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import java.time.Instant

class DealService(
    private val orderRepository: OrderRepository,
    private val emailSystem: EmailSystem,
    private val meterRegistry: MeterRegistry,
    private val now: () -> Instant = Instant::now
) {
    fun process(deliveryUpdate: DeliveryUpdate) {
        val order = orderRepository.findByIdOrNull(deliveryUpdate.orderId)

        when (val updateResult = order.process(deliveryUpdate, now)) {
            is UnknownUpdate -> {
                monitor(updateResult.eventName)
                log("No order with id ${deliveryUpdate.orderId} in database!")
            }

            is OutdatedUpdate -> {
                monitor(updateResult.eventName)
                log("Incoming update ${deliveryUpdate.id} is outdated! Ignoring it.")
            }

            is SuccessfullyUpdated -> {
                orderRepository.update(updateResult.updatedOrder)

                if (updateResult.email != null) {
                    emailSystem.send(updateResult.email)
                }

                monitor(updateResult.eventName)
                log("Processed update ${deliveryUpdate.id}")
            }
        }
    }

    private fun monitor(eventName: String) {
        Counter.builder(eventName)
            .register(meterRegistry)
            .increment()
    }
}

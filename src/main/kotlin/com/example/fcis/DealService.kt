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

        return when (val result = update(order, deliveryUpdate, now())) {
            is SuccessfulUpdate -> {
                orderRepository.update(result.updatedOrder)
                result.email?.let { emailSystem.send(it) }
                log(result.event.message)
                monitor(result.event.name)
            }

            is UnknownOrder -> {
                log(result.event.message)
                monitor(result.event.name)
            }

            is UpdateIgnored -> {
                log(result.event.message)
                monitor(result.event.name)
            }

            is OutdatedUpdate -> {
                log(result.event.message)
                monitor(result.event.name)
            }
        }
    }

    private fun monitor(eventName: String) {
        Counter.builder(eventName)
            .register(meterRegistry)
            .increment()
    }
}

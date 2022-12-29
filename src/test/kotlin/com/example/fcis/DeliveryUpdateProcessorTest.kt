package com.example.fcis

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

class DeliveryUpdateProcessorTest {
    val orderRepository = mockk<OrderRepository>()
    val emailSystem = mockk<EmailSystem>()
    val meterRegistry = SimpleMeterRegistry()
    val now: Instant = Instant.now()
    val deliveryUpdateProcessor = DeliveryUpdateProcessor(
        orderRepository,
        emailSystem,
        meterRegistry,
        now = { now }
    )

    @Test
    fun `stores the order after a successful delivery update`() {
        val anOrder = anOrderOf()
        val aValidUpdate = aDeliveryUpdateOf(anOrder.id)

        every { orderRepository.findByIdOrNull(any()) } returns anOrder
        every { orderRepository.update(any()) } returns anOrder
        every { emailSystem.send(any()) } returns "emailId"

        deliveryUpdateProcessor.process(aValidUpdate)

        verify { emailSystem.send(any()) }
    }

    @Test
    fun `sends an email to customer after a successful delivery update`() {
        val anOrder = anOrderOf()
        val aValidUpdate = aDeliveryUpdateOf(anOrder.id)

        every { orderRepository.findByIdOrNull(any()) } returns anOrder
        every { orderRepository.update(any()) } returns anOrder
        every { emailSystem.send(any()) } returns "emailId"

        deliveryUpdateProcessor.process(aValidUpdate)

        verify {
            orderRepository.update(
                anOrderOf().copy(
                    currentStatusDetails = Order.StatusDetails(
                        currentStatus = aValidUpdate.newStatus,
                        updatedAt = now
                    )
                )
            )
        }
    }


    @Test
    fun `monitors after a successful delivery update`() {
        val anOrder = anOrderOf()
        val aValidUpdate = aDeliveryUpdateOf(anOrder.id)

        every { orderRepository.findByIdOrNull(any()) } returns anOrder
        every { orderRepository.update(any()) } returns anOrder
        every { emailSystem.send(any()) } returns "emailId"

        deliveryUpdateProcessor.process(aValidUpdate)

        meterRegistry.shouldHaveOnlyMeterWithValue(meterId = "updates.successful", count = 1)
    }

    @Test
    fun `monitors when corresponding order doesn't exist`() {
        every { orderRepository.findByIdOrNull(any()) } returns null

        val anUnknownUpdate = aDeliveryUpdateOf(orderId = "UNKNOWN")
        deliveryUpdateProcessor.process(anUnknownUpdate)

        meterRegistry.shouldHaveOnlyMeterWithValue(
            meterId = "updates.unknown",
            count = 1
        )
    }

    @Test
    fun `sends no email when corresponding order doesn't exist`() {
        every { orderRepository.findByIdOrNull(any()) } returns null

        val anUnknownUpdate = aDeliveryUpdateOf(orderId = "UNKNOWN")
        deliveryUpdateProcessor.process(anUnknownUpdate)

        verify(exactly = 0) { emailSystem.send(any()) }
    }

    @Test
    fun `sends no email when update is outdated`() {
        val existingOrder = anOrderOf(lastUpdatedAt = "2023-01-01T10:00:00.00Z")
        val anOutdatedUpdate = aDeliveryUpdateOf(
            orderId = existingOrder.orderId,
            updatedAt = "2022-10-10T10:00:00.00Z"
        )

        every { orderRepository.findByIdOrNull(any()) } returns existingOrder

        deliveryUpdateProcessor.process(anOutdatedUpdate)

        verify(exactly = 0) { emailSystem.send(any()) }
    }

    @Test
    fun `monitors an outdated update`() {
        val existingOrder = anOrderOf(lastUpdatedAt = "2023-01-01T10:00:00.00Z")
        val anOutdatedUpdate = aDeliveryUpdateOf(
            orderId = existingOrder.orderId,
            updatedAt = "2022-10-10T10:00:00.00Z"
        )

        every { orderRepository.findByIdOrNull(any()) } returns existingOrder

        deliveryUpdateProcessor.process(anOutdatedUpdate)

        meterRegistry.shouldHaveOnlyMeterWithValue(
            meterId = "updates.outdated",
            count = 1
        )
    }

    @Test
    fun `sends no email when email notifications are disabled`() {
        val anOrder = anOrderOf(emailNotificationsEnabled = false)

        every { orderRepository.findByIdOrNull(any()) } returns anOrder
        every { orderRepository.update(any()) } returns anOrder
        every { emailSystem.send(any()) } returns "emailId"

        val aValidUpdate = aDeliveryUpdateOf(anOrder.id)

        deliveryUpdateProcessor.process(aValidUpdate)

        verify(exactly = 0) { emailSystem.send(any()) }
    }
}

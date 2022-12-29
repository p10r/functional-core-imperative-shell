package com.example.fcis

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Instant


fun aDeliveryUpdateOf(
    orderId: String,
    updatedAt: String = "2023-12-01T10:00:00.00Z"
) = DeliveryUpdate(
    id = "internalId",
    orderId = orderId,
    statusUpdatedAt = Instant.parse(updatedAt),
    newStatus = Order.Status.DISPATCHED
)

fun anOrderOf(
    lastUpdatedAt: String = "2023-01-01T10:00:00.00Z",
    emailNotificationsEnabled: Boolean = true
) = Order(
    id = "id",
    orderId = "oderId",
    customer = Order.Customer(
        customerId = "customerId",
        emailAddress = "test@example.com",
        emailNotificationsEnabled = emailNotificationsEnabled
    ),
    currentStatusDetails = Order.StatusDetails(
        currentStatus = Order.Status.DISPATCHED,
        updatedAt = Instant.parse(lastUpdatedAt)
    )
)

fun SimpleMeterRegistry.shouldHaveOnlyMeterWithValue(
    meterId: String,
    count: Int,
) {
    meters.shouldHaveSize(count)
        .first { it.id.name == meterId }
        .measure()
        .shouldHaveSize(count)
        .first()
        .value
        .shouldBe(count)
}

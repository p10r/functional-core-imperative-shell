package com.example.fcis

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class UpdateOrderTest {
//    we don't even need to check this because there's no email in our UpdateOrderResult
//    @Test
//    fun `sends no email when update is outdated`() {}

    @Test
    fun `monitors an outdated update`() {
        val existingOrder = anOrderOf(lastUpdatedAt = "2023-01-01T10:00:00.00Z")
        val anOutdatedUpdate = aDeliveryUpdateOf(
            orderId = existingOrder.orderId,
            updatedAt = "2022-10-10T10:00:00.00Z"
        )

        existingOrder.process(anOutdatedUpdate).shouldBeTypeOf<OutdatedUpdate>()
            .eventName shouldBe "updates.outdated"
    }
}

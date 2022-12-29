package com.example.fcis

import com.oneeyedmen.okeydoke.Approver
import com.oneeyedmen.okeydoke.junit5.ApprovalsExtension
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant


@ExtendWith(ApprovalsExtension::class)
class UpdateOrderTest {
    @Test
    fun `creates an updated order after a successfully processing the update`() {
        val order = anOrderOf()
        val aValidUpdate = aDeliveryUpdateOf(order.id)
        val now = { Instant.parse("2023-01-01T00:00:00.00Z") }

        order.process(aValidUpdate, now).shouldBeTypeOf<SuccessfullyUpdated>()
            .updatedOrder
            .currentStatusDetails.asClue {
                it.updatedAt shouldBe now()
                it.currentStatus shouldBe aValidUpdate.newStatus
            }
    }

    @Test
    fun `sends an email to customer after a successful delivery update`(approver: Approver) {
        val order = anOrderOf()
        val aValidUpdate = aDeliveryUpdateOf(order.id)

        val result = order.process(aValidUpdate).shouldBeTypeOf<SuccessfullyUpdated>()

        approver.assertApproved(result.email)
    }

    @Test
    fun `monitors after a successful delivery update`() {
        val order = anOrderOf()
        val aValidUpdate = aDeliveryUpdateOf(order.id)

        order.process(aValidUpdate).shouldBeTypeOf<SuccessfullyUpdated>()
            .eventName shouldBe "updates.successful"
    }


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

    @Test
    fun `monitors when corresponding order doesn't exist`() {
        val missingOrder: Order? = null
        val anUnknownUpdate = aDeliveryUpdateOf(orderId = "UNKNOWN")
        missingOrder.process(anUnknownUpdate).shouldBeTypeOf<UnknownUpdate>()
            .eventName shouldBe "updates.unknown"
    }
}

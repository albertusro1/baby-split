package com.babysplit.app.core.whatsapp

import com.babysplit.app.feature.expense.domain.model.Expense
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.ExpenseParticipant
import com.babysplit.app.feature.expense.domain.model.SplitType
import org.junit.Assert.assertTrue
import org.junit.Test

class BillSummaryFormatterTest {

    @Test
    fun testFormatMemberWhatsAppMessage() {
        val paymentDetails = HostPaymentDetails(
            hostName = "Rowan",
            bankName = "BCA",
            bankAccountNumber = "123-456-789"
        )

        val expense = Expense(
            groupId = 1L,
            title = "Dinner at Trattoria",
            totalAmountCents = 10000L,
            currency = "USD",
            category = ExpenseCategory.FOOD,
            paidByMemberId = 1L,
            paidByMemberName = "Rowan",
            splitType = SplitType.EQUAL,
            participants = listOf(
                ExpenseParticipant(2L, "Alice", 2500L)
            )
        )

        val message = BillSummaryFormatter.formatMemberWhatsAppMessage(
            tripName = "Bali Vacation",
            memberName = "Alice",
            memberExpenses = listOf(expense to 2500L),
            totalOwedCents = 2500L,
            currency = "USD",
            paymentDetails = paymentDetails
        )

        assertTrue(message.contains("Bali Vacation"))
        assertTrue(message.contains("Alice"))
        assertTrue(message.contains("Dinner at Trattoria"))
        assertTrue(message.contains("25.00"))
        assertTrue(message.contains("123-456-789"))
        assertTrue(message.contains("BCA"))
    }
}

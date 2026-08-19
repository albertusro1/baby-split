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

    @Test
    fun testFormatMemberWithAccountHolderName() {
        val paymentDetails = HostPaymentDetails(
            hostName = "Rowan",
            bankName = "Mandiri",
            accountHolderName = "Rowan Alexander",
            bankAccountNumber = "987-654-321"
        )

        val expense = Expense(
            groupId = 1L,
            title = "Villa Booking",
            totalAmountCents = 50000000L,
            currency = "IDR",
            category = ExpenseCategory.ACCOMMODATION,
            paidByMemberId = 1L,
            paidByMemberName = "Rowan",
            splitType = SplitType.EQUAL,
            participants = listOf(
                ExpenseParticipant(2L, "Bob", 25000000L)
            )
        )

        val message = BillSummaryFormatter.formatMemberWhatsAppMessage(
            tripName = "Bali Trip",
            memberName = "Bob",
            memberExpenses = listOf(expense to 25000000L),
            totalOwedCents = 25000000L,
            currency = "IDR",
            paymentDetails = paymentDetails
        )

        assertTrue(message.contains("Bali Trip"))
        assertTrue(message.contains("Bob"))
        assertTrue(message.contains("Mandiri"))
        assertTrue(message.contains("987-654-321"))
        assertTrue(message.contains("Rowan Alexander"))
    }

    @Test
    fun testFormatMemberOwesNonHostCreditor() {
        val paymentDetails = HostPaymentDetails(
            hostName = "Rowan",
            bankName = "BCA",
            accountHolderName = "Rowan",
            bankAccountNumber = "2450900365"
        )

        val expense = Expense(
            groupId = 1L,
            title = "Dinner by Itik",
            totalAmountCents = 30000000L,
            currency = "IDR",
            category = ExpenseCategory.FOOD,
            paidByMemberId = 3L,
            paidByMemberName = "Itik",
            splitType = SplitType.EQUAL,
            participants = listOf(
                ExpenseParticipant(2L, "Alice", 10000000L)
            )
        )

        val tx = com.babysplit.app.feature.balance.domain.engine.DebtSimplificationEngine.SimplifiedTransaction(
            debtorId = 2L,
            debtorName = "Alice",
            creditorId = 3L,
            creditorName = "Itik",
            amountCents = 10000000L
        )

        val message = BillSummaryFormatter.formatMemberWhatsAppMessage(
            tripName = "Bali Trip",
            memberName = "Alice",
            memberExpenses = listOf(expense to 10000000L),
            totalOwedCents = 10000000L,
            currency = "IDR",
            paymentDetails = paymentDetails,
            debtorTransactions = listOf(tx),
            hostMemberName = "Rowan"
        )

        assertTrue(message.contains("Pay *Itik*"))
        assertTrue(message.contains("Please contact *Itik* directly"))
        // Should NOT contain the host's bank account when Alice owes Itik!
        org.junit.Assert.assertFalse(message.contains("2450900365"))
    }
}


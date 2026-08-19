package com.babysplit.app.core.whatsapp

import com.babysplit.app.core.repository.ExpenseData
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.ParticipantData
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

        val expense = ExpenseData(
            id = "1",
            tripId = "1",
            title = "Dinner at Trattoria",
            totalAmountCents = 10000L,
            currency = "USD",
            categoryName = "FOOD",
            paidByMemberId = "1",
            paidByMemberName = "Rowan",
            splitType = "EQUAL",
            participants = listOf(
                ParticipantData("2", "Alice", 2500L)
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

        val expense = ExpenseData(
            id = "2",
            tripId = "1",
            title = "Villa Booking",
            totalAmountCents = 50000000L,
            currency = "IDR",
            categoryName = "ACCOMMODATION",
            paidByMemberId = "1",
            paidByMemberName = "Rowan",
            splitType = "EQUAL",
            participants = listOf(
                ParticipantData("2", "Bob", 25000000L)
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

        val expense = ExpenseData(
            id = "3",
            tripId = "1",
            title = "Dinner by Itik",
            totalAmountCents = 30000000L,
            currency = "IDR",
            categoryName = "FOOD",
            paidByMemberId = "3",
            paidByMemberName = "Itik",
            splitType = "EQUAL",
            participants = listOf(
                ParticipantData("2", "Alice", 10000000L)
            )
        )

        val tx = com.babysplit.app.feature.balance.domain.engine.DebtSimplificationEngine.SimplifiedTransaction(
            debtorId = "2",
            debtorName = "Alice",
            creditorId = "3",
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

    @Test
    fun testFormatMemberOwesNonHostCreditorWithSavedBankDetails() {
        val paymentDetails = HostPaymentDetails(
            hostName = "Rowan",
            bankName = "BCA",
            accountHolderName = "Rowan",
            bankAccountNumber = "2450900365"
        )

        val expense = ExpenseData(
            id = "4",
            tripId = "1",
            title = "Dinner by Itik",
            totalAmountCents = 30000000L,
            currency = "IDR",
            categoryName = "FOOD",
            paidByMemberId = "3",
            paidByMemberName = "Itik",
            splitType = "EQUAL",
            participants = listOf(
                ParticipantData("2", "Alice", 10000000L)
            )
        )

        val tx = com.babysplit.app.feature.balance.domain.engine.DebtSimplificationEngine.SimplifiedTransaction(
            debtorId = "2",
            debtorName = "Alice",
            creditorId = "3",
            creditorName = "Itik",
            amountCents = 10000000L
        )

        val itikMember = MemberData(
            id = "3",
            tripId = "1",
            name = "Itik",
            bankName = "Bank Jago",
            accountHolderName = "Itik Bebek",
            bankAccountNumber = "1029384756",
            eWalletHandle = "08198765432"
        )

        val message = BillSummaryFormatter.formatMemberWhatsAppMessage(
            tripName = "Bali Trip",
            memberName = "Alice",
            memberExpenses = listOf(expense to 10000000L),
            totalOwedCents = 10000000L,
            currency = "IDR",
            paymentDetails = paymentDetails,
            debtorTransactions = listOf(tx),
            creditorMembers = mapOf("Itik" to itikMember),
            hostMemberName = "Rowan"
        )

        assertTrue(message.contains("Pay *Itik*"))
        assertTrue(message.contains("Bank Jago"))
        assertTrue(message.contains("1029384756"))
        assertTrue(message.contains("Itik Bebek"))
        assertTrue(message.contains("08198765432"))
        // Should NOT contain host's BCA account
        org.junit.Assert.assertFalse(message.contains("2450900365"))
    }
}


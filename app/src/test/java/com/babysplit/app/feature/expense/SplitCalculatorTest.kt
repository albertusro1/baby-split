package com.babysplit.app.feature.expense

import com.babysplit.app.feature.expense.domain.engine.SplitCalculator
import com.babysplit.app.feature.expense.domain.model.SplitType
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun testEqualSplitWithRemainderCents() {
        // $10.00 split among 3 people = $3.34, $3.33, $3.33 -> sum = $10.00 (1000 cents)
        val members = listOf(
            SplitCalculator.MemberInput("1", "Alice"),
            SplitCalculator.MemberInput("2", "Bob"),
            SplitCalculator.MemberInput("3", "Charlie")
        )

        val result = SplitCalculator.calculateSplit(1000L, members, SplitType.EQUAL)

        assertEquals(3, result.size)
        assertEquals(334L, result[0].amountCents)
        assertEquals(333L, result[1].amountCents)
        assertEquals(333L, result[2].amountCents)
        assertEquals(1000L, result.sumOf { it.amountCents })
    }

    @Test
    fun testEqualSplitWithUntickedParticipants() {
        // $100.00 split among 3 people where Charlie is unticked (inputValue = 0.0) -> Alice $50.00, Bob $50.00, Charlie $0.00
        val members = listOf(
            SplitCalculator.MemberInput("1", "Alice", 1.0),
            SplitCalculator.MemberInput("2", "Bob", 1.0),
            SplitCalculator.MemberInput("3", "Charlie", 0.0)
        )

        val result = SplitCalculator.calculateSplit(10000L, members, SplitType.EQUAL)

        assertEquals(3, result.size)
        assertEquals(5000L, result[0].amountCents)
        assertEquals(5000L, result[1].amountCents)
        assertEquals(0L, result[2].amountCents)
        assertEquals(10000L, result.sumOf { it.amountCents })
    }

    @Test
    fun testExactSplit() {
        val members = listOf(
            SplitCalculator.MemberInput("1", "Alice", 2500.0), // $25.00
            SplitCalculator.MemberInput("2", "Bob", 1550.0),   // $15.50
            SplitCalculator.MemberInput("3", "Charlie", 950.0) // $9.50
        )

        val result = SplitCalculator.calculateSplit(5000L, members, SplitType.EXACT)

        assertEquals(3, result.size)
        assertEquals(2500L, result[0].amountCents)
        assertEquals(1550L, result[1].amountCents)
        assertEquals(950L, result[2].amountCents)
        assertEquals(5000L, result.sumOf { it.amountCents })
    }

    @Test
    fun testPercentageSplitWithRoundingAdjustment() {
        // $100.00 split 33.33%, 33.33%, 33.34%
        val members = listOf(
            SplitCalculator.MemberInput("1", "Alice", 33.33),
            SplitCalculator.MemberInput("2", "Bob", 33.33),
            SplitCalculator.MemberInput("3", "Charlie", 33.34)
        )

        val result = SplitCalculator.calculateSplit(10000L, members, SplitType.PERCENTAGE)

        assertEquals(3, result.size)
        assertEquals(10000L, result.sumOf { it.amountCents })
    }

    @Test
    fun testShareSplit() {
        // $120.00 split by shares 2 : 1 : 1 (Alice pays 2/4 = $60, Bob $30, Charlie $30)
        val members = listOf(
            SplitCalculator.MemberInput("1", "Alice", 2.0),
            SplitCalculator.MemberInput("2", "Bob", 1.0),
            SplitCalculator.MemberInput("3", "Charlie", 1.0)
        )

        val result = SplitCalculator.calculateSplit(12000L, members, SplitType.SHARE)

        assertEquals(3, result.size)
        assertEquals(6000L, result[0].amountCents)
        assertEquals(3000L, result[1].amountCents)
        assertEquals(3000L, result[2].amountCents)
        assertEquals(12000L, result.sumOf { it.amountCents })
    }

    @Test
    fun testAdjustmentSplit() {
        // $100.00 split equally among 2 people ($50 each), but Alice has +$10 adjustment -> Alice $60, Bob $40
        val members = listOf(
            SplitCalculator.MemberInput("1", "Alice", 1000.0), // +$10.00
            SplitCalculator.MemberInput("2", "Bob", -1000.0)   // -$10.00
        )

        val result = SplitCalculator.calculateSplit(10000L, members, SplitType.ADJUSTMENT)

        assertEquals(2, result.size)
        assertEquals(6000L, result[0].amountCents)
        assertEquals(4000L, result[1].amountCents)
        assertEquals(10000L, result.sumOf { it.amountCents })
    }
}

package com.babysplit.app.feature.balance

import com.babysplit.app.feature.balance.domain.engine.DebtSimplificationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtSimplificationEngineTest {

    @Test
    fun testCircularDebtSimplification() {
        // Alice owes Bob $20, Bob owes Charlie $20
        // Net: Alice -20, Bob 0, Charlie +20
        // Result: Alice pays Charlie $20 directly (Bob 0 transactions)
        val netBalances = mapOf(
            "1" to -2000L,
            "2" to 0L,
            "3" to 2000L
        )
        val memberNames = mapOf(
            "1" to "Alice",
            "2" to "Bob",
            "3" to "Charlie"
        )

        val transactions = DebtSimplificationEngine.simplifyDebts(netBalances, memberNames)

        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals("Alice", tx.debtorName)
        assertEquals("Charlie", tx.creditorName)
        assertEquals(2000L, tx.amountCents)
    }

    @Test
    fun testMultiPersonSimplification() {
        // 4 members:
        // Alice: owes $30 (-3000)
        // Bob: owes $20 (-2000)
        // Charlie: is owed $40 (+4000)
        // Dave: is owed $10 (+1000)
        val netBalances = mapOf(
            "1" to -3000L,
            "2" to -2000L,
            "3" to 4000L,
            "4" to 1000L
        )
        val memberNames = mapOf(
            "1" to "Alice",
            "2" to "Bob",
            "3" to "Charlie",
            "4" to "Dave"
        )

        val transactions = DebtSimplificationEngine.simplifyDebts(netBalances, memberNames)

        // Total debts settled must equal 5000
        assertEquals(5000L, transactions.sumOf { it.amountCents })
        // Number of transactions should be at most 3 (N-1)
        assert(transactions.size <= 3)
    }
}

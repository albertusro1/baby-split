package com.babysplit.app.feature.balance.domain.engine

import kotlin.math.min

/**
 * High-performance Debt Simplification Engine.
 * Reduces an arbitrary matrix of peer-to-peer debts to a minimal set of transactions (at most N-1 transactions).
 */
object DebtSimplificationEngine {

    data class SimplifiedTransaction(
        val debtorId: Long,
        val debtorName: String,
        val creditorId: Long,
        val creditorName: String,
        val amountCents: Long
    )

    /**
     * Simplifies debts given a map of member net balances (positive = owed money, negative = owes money).
     */
    fun simplifyDebts(
        netBalances: Map<Long, Long>,
        memberNames: Map<Long, String>
    ): List<SimplifiedTransaction> {
        // Separate members into debtors (negative balance) and creditors (positive balance)
        data class BalanceEntry(val memberId: Long, var amountCents: Long)

        val debtors = mutableListOf<BalanceEntry>()
        val creditors = mutableListOf<BalanceEntry>()

        for ((memberId, balance) in netBalances) {
            if (balance < 0) {
                debtors.add(BalanceEntry(memberId, -balance)) // store as positive amount owed
            } else if (balance > 0) {
                creditors.add(BalanceEntry(memberId, balance))
            }
        }

        // Sort descending by amount
        debtors.sortByDescending { it.amountCents }
        creditors.sortByDescending { it.amountCents }

        val simplified = mutableListOf<SimplifiedTransaction>()
        var debtorIdx = 0
        var creditorIdx = 0

        while (debtorIdx < debtors.size && creditorIdx < creditors.size) {
            val debtor = debtors[debtorIdx]
            val creditor = creditors[creditorIdx]

            val settleAmount = min(debtor.amountCents, creditor.amountCents)
            if (settleAmount > 0) {
                simplified.add(
                    SimplifiedTransaction(
                        debtorId = debtor.memberId,
                        debtorName = memberNames[debtor.memberId] ?: "Member #",
                        creditorId = creditor.memberId,
                        creditorName = memberNames[creditor.memberId] ?: "Member #",
                        amountCents = settleAmount
                    )
                )

                debtor.amountCents -= settleAmount
                creditor.amountCents -= settleAmount
            }

            if (debtor.amountCents == 0L) debtorIdx++
            if (creditor.amountCents == 0L) creditorIdx++
        }

        return simplified
    }
}

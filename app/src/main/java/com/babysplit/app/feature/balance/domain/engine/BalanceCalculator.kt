package com.babysplit.app.feature.balance.domain.engine

import com.babysplit.app.feature.expense.domain.model.Expense

data class MemberBalanceSummary(
    val memberId: Long,
    val memberName: String,
    val totalPaidCents: Long,
    val totalShareCents: Long,
    val netBalanceCents: Long // Positive = is owed, Negative = owes
)

object BalanceCalculator {

    /**
     * Computes net balance for all members across a list of [expenses].
     */
    fun calculateBalances(
        expenses: List<Expense>,
        memberNames: Map<Long, String>
    ): List<MemberBalanceSummary> {
        val paidMap = mutableMapOf<Long, Long>()
        val shareMap = mutableMapOf<Long, Long>()

        // Initialize with all members
        memberNames.keys.forEach { memberId ->
            paidMap[memberId] = 0L
            shareMap[memberId] = 0L
        }

        for (expense in expenses) {
            // Payer credit
            paidMap[expense.paidByMemberId] = (paidMap[expense.paidByMemberId] ?: 0L) + expense.totalAmountCents

            // Participants debit
            for (participant in expense.participants) {
                shareMap[participant.memberId] = (shareMap[participant.memberId] ?: 0L) + participant.amountCents
            }
        }

        return memberNames.map { (memberId, name) ->
            val totalPaid = paidMap[memberId] ?: 0L
            val totalShare = shareMap[memberId] ?: 0L
            MemberBalanceSummary(
                memberId = memberId,
                memberName = name,
                totalPaidCents = totalPaid,
                totalShareCents = totalShare,
                netBalanceCents = totalPaid - totalShare
            )
        }
    }
}

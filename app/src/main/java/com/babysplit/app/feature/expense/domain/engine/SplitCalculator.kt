package com.babysplit.app.feature.expense.domain.engine

import com.babysplit.app.feature.expense.domain.model.ExpenseParticipant
import com.babysplit.app.feature.expense.domain.model.SplitType
import kotlin.math.roundToLong

/**
 * High-precision mathematical splitting engine for Baby Split.
 * All monetary amounts are computed in integer cents (Long) with deterministic remainder distribution.
 */
object SplitCalculator {

    data class MemberInput(
        val memberId: String,
        val memberName: String,
        val inputValue: Double = 0.0 // Value used for Exact ($), % (0-100), Share (1, 2, ...), or Adj (+/- $)
    )

    /**
     * Splits [totalAmountCents] among [members] according to [splitType].
     * Guarantees that sum(participant.amountCents) == totalAmountCents.
     */
    fun calculateSplit(
        totalAmountCents: Long,
        members: List<MemberInput>,
        splitType: SplitType
    ): List<ExpenseParticipant> {
        if (members.isEmpty()) return emptyList()
        if (members.size == 1) {
            val single = members.first()
            return listOf(
                ExpenseParticipant(
                    memberId = single.memberId,
                    memberName = single.memberName,
                    amountCents = totalAmountCents,
                    rawShareValue = single.inputValue
                )
            )
        }

        return when (splitType) {
            SplitType.EQUAL -> calculateEqualSplit(totalAmountCents, members)
            SplitType.EXACT -> calculateExactSplit(members)
            SplitType.PERCENTAGE -> calculatePercentageSplit(totalAmountCents, members)
            SplitType.SHARE -> calculateShareSplit(totalAmountCents, members)
            SplitType.ADJUSTMENT -> calculateAdjustmentSplit(totalAmountCents, members)
            SplitType.SETTLEMENT -> calculateExactSplit(members)
        }
    }

    /**
     * Equal Split: Base share = floor(total / N_active), remainder cents assigned to first R active participants.
     * Unticked participants (inputValue <= 0.0) receive 0 cents.
     */
    private fun calculateEqualSplit(
        totalAmountCents: Long,
        members: List<MemberInput>
    ): List<ExpenseParticipant> {
        val hasExplicitSelection = members.any { it.inputValue > 0.0 }
        val activeMembers = if (hasExplicitSelection) members.filter { it.inputValue > 0.0 } else members
        if (activeMembers.isEmpty()) {
            return members.map {
                ExpenseParticipant(
                    memberId = it.memberId,
                    memberName = it.memberName,
                    amountCents = 0L,
                    rawShareValue = 0.0
                )
            }
        }

        val n = activeMembers.size
        val baseShare = totalAmountCents / n
        val remainder = (totalAmountCents % n).toInt()

        return members.map { member ->
            val activeIndex = activeMembers.indexOfFirst { it.memberId == member.memberId }
            if (activeIndex >= 0) {
                val extraCent = if (activeIndex < remainder) 1L else 0L
                ExpenseParticipant(
                    memberId = member.memberId,
                    memberName = member.memberName,
                    amountCents = baseShare + extraCent,
                    rawShareValue = 1.0
                )
            } else {
                ExpenseParticipant(
                    memberId = member.memberId,
                    memberName = member.memberName,
                    amountCents = 0L,
                    rawShareValue = 0.0
                )
            }
        }
    }

    /**
     * Exact Split: Each member's amount is explicitly given in cents.
     */
    private fun calculateExactSplit(
        members: List<MemberInput>
    ): List<ExpenseParticipant> {
        return members.map { member ->
            ExpenseParticipant(
                memberId = member.memberId,
                memberName = member.memberName,
                amountCents = member.inputValue.roundToLong(),
                rawShareValue = member.inputValue
            )
        }
    }

    /**
     * Percentage Split: Base share = round(total * (percent / 100)).
     * Remainder difference adjusted on the member with the largest percentage share.
     */
    private fun calculatePercentageSplit(
        totalAmountCents: Long,
        members: List<MemberInput>
    ): List<ExpenseParticipant> {
        val totalPercentage = members.sumOf { it.inputValue }
        if (totalPercentage <= 0.0) return calculateEqualSplit(totalAmountCents, members)

        val rawShares = members.map { member ->
            val shareCents = ((totalAmountCents * member.inputValue) / 100.0).roundToLong()
            member to shareCents
        }

        val allocatedSum = rawShares.sumOf { it.second }
        val diff = totalAmountCents - allocatedSum

        // Adjust the diff on the highest percentage participant
        val maxIndex = members.indices.maxByOrNull { members[it].inputValue } ?: 0

        return rawShares.mapIndexed { index, (member, shareCents) ->
            val finalAmount = if (index == maxIndex) shareCents + diff else shareCents
            ExpenseParticipant(
                memberId = member.memberId,
                memberName = member.memberName,
                amountCents = finalAmount,
                rawShareValue = member.inputValue
            )
        }
    }

    /**
     * Share Split: Share = floor(total * (shares_i / totalShares)). Remainder cents distributed to highest share holders.
     */
    private fun calculateShareSplit(
        totalAmountCents: Long,
        members: List<MemberInput>
    ): List<ExpenseParticipant> {
        val totalShares = members.sumOf { it.inputValue }
        if (totalShares <= 0.0) return calculateEqualSplit(totalAmountCents, members)

        val calculated = members.map { member ->
            val exactShare = (totalAmountCents * member.inputValue) / totalShares
            val floorShare = exactShare.toLong()
            val fraction = exactShare - floorShare
            Triple(member, floorShare, fraction)
        }

        val allocatedSum = calculated.sumOf { it.second }
        var remainder = (totalAmountCents - allocatedSum).toInt()

        // Distribute remainder cents to participants with largest fractional parts
        val sortedByFraction = calculated.indices.sortedByDescending { calculated[it].third }
        val finalAmounts = LongArray(members.size) { calculated[it].second }

        for (idx in sortedByFraction) {
            if (remainder <= 0) break
            finalAmounts[idx] += 1L
            remainder--
        }

        return members.mapIndexed { index, member ->
            ExpenseParticipant(
                memberId = member.memberId,
                memberName = member.memberName,
                amountCents = finalAmounts[index],
                rawShareValue = member.inputValue
            )
        }
    }

    /**
     * Adjustment Split: Base amount = (Total - sum(Adjustments)) / N.
     * Each member pays Base amount + their Adjustment.
     */
    private fun calculateAdjustmentSplit(
        totalAmountCents: Long,
        members: List<MemberInput>
    ): List<ExpenseParticipant> {
        val n = members.size
        val totalAdjustmentsCents = members.sumOf { it.inputValue.roundToLong() }
        val remainingToSplitEqually = totalAmountCents - totalAdjustmentsCents

        val baseShare = remainingToSplitEqually / n
        val remainder = (remainingToSplitEqually % n).toInt()

        return members.mapIndexed { index, member ->
            val extraCent = if (index < remainder) 1L else 0L
            val adjCents = member.inputValue.roundToLong()
            ExpenseParticipant(
                memberId = member.memberId,
                memberName = member.memberName,
                amountCents = baseShare + extraCent + adjCents,
                rawShareValue = member.inputValue
            )
        }
    }
}

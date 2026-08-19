package com.babysplit.app.feature.expense.domain.model

import java.util.UUID

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val groupId: Long,
    val title: String,
    val totalAmountCents: Long,
    val currency: String = "USD",
    val category: ExpenseCategory = ExpenseCategory.GENERAL,
    val paidByMemberId: Long,
    val paidByMemberName: String,
    val splitType: SplitType = SplitType.EQUAL,
    val participants: List<ExpenseParticipant>,
    val receiptImagePath: String? = null,
    val note: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val isSettlement: Boolean = false
)

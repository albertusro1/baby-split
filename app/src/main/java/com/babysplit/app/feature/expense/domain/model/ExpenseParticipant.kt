package com.babysplit.app.feature.expense.domain.model

data class ExpenseParticipant(
    val memberId: Long,
    val memberName: String,
    val amountCents: Long,
    val rawShareValue: Double = 0.0 // Percentage (e.g. 50.0), Shares (e.g. 2.0), or Adjustment (+/- cents)
)

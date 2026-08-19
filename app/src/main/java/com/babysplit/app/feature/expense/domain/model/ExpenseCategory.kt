package com.babysplit.app.feature.expense.domain.model

enum class ExpenseCategory(val displayName: String, val emoji: String, val colorHex: String) {
    FOOD("Food & Dining", "🍔", "#FF8A65"),
    GROCERIES("Groceries", "🛒", "#81C784"),
    TRANSPORTATION("Transportation", "🚗", "#64B5F6"),
    ACCOMMODATION("Accommodation", "🏨", "#BA68C8"),
    ENTERTAINMENT("Entertainment", "🎉", "#FFD54F"),
    BILLS("Bills & Utilities", "💡", "#4DB6AC"),
    SHOPPING("Shopping", "🛍️", "#F06292"),
    GENERAL("General", "🧾", "#90A4AE"),
    SETTLEMENT("Settlement Payment", "💸", "#4CAF50");

    companion object {
        fun fromName(name: String): ExpenseCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: GENERAL
        }
    }
}

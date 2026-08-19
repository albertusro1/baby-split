package com.babysplit.app.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object GroupDetail : Screen("group_detail/{tripId}") {
        fun createRoute(tripId: String) = "group_detail/$tripId"
    }
    data object AddEditExpense : Screen("add_edit_expense/{tripId}?expenseId={expenseId}") {
        fun createRoute(tripId: String, expenseId: String? = null) =
            if (expenseId != null) "add_edit_expense/$tripId?expenseId=$expenseId"
            else "add_edit_expense/$tripId"
    }
    data object Profile : Screen("profile")
}

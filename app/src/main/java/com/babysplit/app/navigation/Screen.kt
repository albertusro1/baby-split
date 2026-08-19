package com.babysplit.app.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: Long) = "group_detail/$groupId"
    }
    data object AddEditExpense : Screen("add_edit_expense/{groupId}?expenseId={expenseId}") {
        fun createRoute(groupId: Long, expenseId: String? = null) =
            if (expenseId != null) "add_edit_expense/$groupId?expenseId=$expenseId"
            else "add_edit_expense/$groupId"
    }
    data object Profile : Screen("profile")
}

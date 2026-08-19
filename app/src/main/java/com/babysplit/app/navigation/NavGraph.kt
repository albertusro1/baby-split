package com.babysplit.app.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.babysplit.app.BabySplitApplication
import com.babysplit.app.core.database.dao.ExpenseWithParticipants
import com.babysplit.app.core.database.entity.ExpenseEntity
import com.babysplit.app.core.database.entity.ExpenseParticipantEntity
import com.babysplit.app.core.database.entity.GroupEntity
import com.babysplit.app.core.database.entity.MemberEntity
import com.babysplit.app.feature.dashboard.presentation.DashboardScreen
import com.babysplit.app.feature.expense.presentation.AddEditExpenseScreen
import com.babysplit.app.feature.group.domain.TripLifecycleManager
import com.babysplit.app.feature.group.presentation.CreateGroupDialog
import com.babysplit.app.feature.group.presentation.GroupDetailScreen
import com.babysplit.app.feature.profile.presentation.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    app: BabySplitApplication,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val database = app.database
    val userPrefs = app.userPreferences
    val tripLifecycleManager = remember { TripLifecycleManager(database) }

    val groups by database.groupDao().getAllGroups().collectAsState(initial = emptyList())
    val paymentDetails by userPrefs.hostPaymentDetailsFlow.collectAsState(initial = null)
    val defaultCurrency by userPrefs.defaultCurrencyFlow.collectAsState(initial = "USD")
    val userEmail by userPrefs.userEmailFlow.collectAsState(initial = null)
    val userName by userPrefs.userNameFlow.collectAsState(initial = "Guest")

    var showCreateGroupDialog by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                groups = groups,
                userEmail = userEmail,
                userName = userName,
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onCreateGroupClick = { showCreateGroupDialog = true },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onDeleteGroup = { gId ->
                    scope.launch {
                        database.groupDao().deleteFullGroup(gId)
                    }
                }
            )

            if (showCreateGroupDialog) {
                CreateGroupDialog(
                    onDismiss = { showCreateGroupDialog = false },
                    onConfirm = { name, emoji, currency, simplifyDebts ->
                        scope.launch {
                            val groupId = database.groupDao().insertGroup(
                                GroupEntity(
                                    name = name,
                                    currency = currency,
                                    emoji = emoji,
                                    simplifyDebts = simplifyDebts
                                )
                            )
                            // Add host as the first member
                            database.memberDao().insertMember(
                                MemberEntity(
                                    groupId = groupId,
                                    name = "You (Host)",
                                    memberType = "HOST"
                                )
                            )
                            showCreateGroupDialog = false
                            navController.navigate(Screen.GroupDetail.createRoute(groupId))
                        }
                    }
                )
            }
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            val group by database.groupDao().getGroupById(groupId).collectAsState(initial = null)
            val members by database.memberDao().getMembersForGroup(groupId).collectAsState(initial = emptyList())
            val expensesWithParticipants by database.expenseDao().getExpensesWithParticipants(groupId).collectAsState(initial = emptyList())

            GroupDetailScreen(
                group = group,
                members = members,
                expensesWithParticipants = expensesWithParticipants,
                paymentDetails = paymentDetails,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { gId ->
                    navController.navigate(Screen.AddEditExpense.createRoute(gId))
                },
                onAddMember = { name, type, email, phone, bankName, holderName, bankAcc, walletName, walletHandle ->
                    scope.launch {
                        database.memberDao().insertMember(
                            MemberEntity(
                                groupId = groupId,
                                name = name,
                                memberType = type,
                                email = email,
                                phoneNumber = phone,
                                bankName = bankName,
                                accountHolderName = holderName,
                                bankAccountNumber = bankAcc,
                                eWalletName = walletName,
                                eWalletHandle = walletHandle
                            )
                        )
                    }
                },
                onUpdateMember = { updatedMember ->
                    scope.launch {
                        database.memberDao().updateMember(updatedMember)
                    }
                },
                onRecordSettlement = { payerId, receiverId, amountCents ->
                    scope.launch {
                        val payer = members.firstOrNull { it.id == payerId }?.name ?: "Payer"
                        val receiver = members.firstOrNull { it.id == receiverId }?.name ?: "Receiver"
                        val expId = java.util.UUID.randomUUID().toString()

                        database.expenseDao().insertFullExpense(
                            expense = ExpenseEntity(
                                id = expId,
                                groupId = groupId,
                                title = "Payment: $payer ➔ $receiver",
                                totalAmountCents = amountCents,
                                currency = group?.currency ?: "USD",
                                categoryName = "SETTLEMENT",
                                paidByMemberId = payerId,
                                paidByMemberName = payer,
                                isSettlement = true
                            ),
                            participants = listOf(
                                ExpenseParticipantEntity(
                                    expenseId = expId,
                                    memberId = receiverId,
                                    memberName = receiver,
                                    amountCents = amountCents
                                )
                            )
                        )
                    }
                },
                onFinishTrip = {
                    scope.launch {
                        tripLifecycleManager.finishTrip(context, groupId, paymentDetails)
                    }
                },
                onDeleteTrip = {
                    scope.launch {
                        database.groupDao().deleteFullGroup(groupId)
                        navController.popBackStack()
                    }
                },
                onEditExpense = { gId, expId ->
                    navController.navigate(Screen.AddEditExpense.createRoute(gId, expId))
                },
                onDeleteExpense = { expId ->
                    scope.launch {
                        database.expenseDao().deleteFullExpense(expId)
                    }
                }
            )
        }

        composable(
            route = Screen.AddEditExpense.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val group by database.groupDao().getGroupById(groupId).collectAsState(initial = null)
            val members by database.memberDao().getMembersForGroup(groupId).collectAsState(initial = emptyList())
            var existingExpense by remember { mutableStateOf<ExpenseWithParticipants?>(null) }

            LaunchedEffect(expenseId) {
                if (!expenseId.isNullOrBlank()) {
                    existingExpense = database.expenseDao().getExpenseWithParticipantsDirect(expenseId)
                }
            }

            AddEditExpenseScreen(
                groupId = groupId,
                currency = group?.currency ?: "USD",
                members = members,
                existingExpense = existingExpense,
                onBackClick = { navController.popBackStack() },
                onDeleteExpense = { expId ->
                    scope.launch {
                        database.expenseDao().deleteFullExpense(expId)
                        navController.popBackStack()
                    }
                },
                onSaveExpense = { expense ->
                    scope.launch {
                        database.expenseDao().insertFullExpense(
                            expense = ExpenseEntity(
                                id = expense.id,
                                groupId = expense.groupId,
                                title = expense.title,
                                totalAmountCents = expense.totalAmountCents,
                                currency = expense.currency,
                                categoryName = expense.category.name,
                                paidByMemberId = expense.paidByMemberId,
                                paidByMemberName = expense.paidByMemberName,
                                splitType = expense.splitType.name,
                                receiptImagePath = expense.receiptImagePath,
                                note = expense.note,
                                createdAtEpochMs = expense.createdAtEpochMs,
                                isSettlement = expense.isSettlement
                            ),
                            participants = expense.participants.map {
                                ExpenseParticipantEntity(
                                    expenseId = expense.id,
                                    memberId = it.memberId,
                                    memberName = it.memberName,
                                    amountCents = it.amountCents,
                                    rawShareValue = it.rawShareValue
                                )
                            }
                        )
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                currentPaymentDetails = paymentDetails,
                currentCurrency = defaultCurrency,
                userEmail = userEmail,
                userName = userName,
                onBackClick = { navController.popBackStack() },
                onSavePaymentDetails = { bank, holder, account, wallet, handle, note, curr ->
                    scope.launch {
                        userPrefs.savePaymentDetails(bank, holder, account, wallet, handle, note, curr)
                    }
                },
                onSaveUserProfile = { name, email ->
                    scope.launch {
                        userPrefs.saveUserProfile(name, email)
                    }
                },
                onRestoreBackups = { backups ->
                    scope.launch {
                        for (backup in backups) {
                            com.babysplit.app.core.gdrive.GoogleDriveBackupEngine.restoreTripBackup(database, backup)
                        }
                    }
                },
                onSignOutClick = {
                    scope.launch {
                        userPrefs.signOut()
                    }
                }
            )
        }
    }
}

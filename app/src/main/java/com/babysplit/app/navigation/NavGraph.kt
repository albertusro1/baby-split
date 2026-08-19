package com.babysplit.app.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.babysplit.app.core.repository.ExpenseData
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.ParticipantData
import com.babysplit.app.core.repository.TripData
import com.babysplit.app.feature.dashboard.presentation.DashboardScreen
import com.babysplit.app.feature.dashboard.presentation.DashboardViewModel
import com.babysplit.app.feature.dashboard.presentation.JoinTripResult
import com.babysplit.app.feature.expense.domain.model.Expense
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.SplitType
import com.babysplit.app.feature.expense.presentation.AddEditExpenseScreen
import com.babysplit.app.feature.group.presentation.CreateGroupDialog
import com.babysplit.app.feature.group.presentation.GroupDetailScreen
import com.babysplit.app.feature.group.presentation.GroupDetailViewModel
import com.babysplit.app.feature.group.presentation.JoinTripDialog
import com.babysplit.app.feature.profile.presentation.ProfileScreen
import com.babysplit.app.feature.profile.presentation.ProfileViewModel
import kotlinx.coroutines.launch

// ── Bridge Helpers: Convert domain models to Room entities for existing screens ──

private fun TripData.toGroupEntity() = GroupEntity(
    id = id.toLongOrNull() ?: 0L,
    name = name,
    currency = currency,
    emoji = emoji,
    simplifyDebts = simplifyDebts,
    isFinished = isFinished,
    createdAtEpochMs = createdAtEpochMs
)

private fun MemberData.toMemberEntity() = MemberEntity(
    id = id.toLongOrNull() ?: 0L,
    groupId = tripId.toLongOrNull() ?: 0L,
    name = name,
    memberType = memberType,
    email = email,
    phoneNumber = phoneNumber,
    avatarColorHex = avatarColorHex,
    bankName = bankName,
    accountHolderName = accountHolderName,
    bankAccountNumber = bankAccountNumber,
    eWalletName = eWalletName,
    eWalletHandle = eWalletHandle
)

private fun ExpenseData.toExpenseWithParticipants() = ExpenseWithParticipants(
    expense = ExpenseEntity(
        id = id,
        groupId = tripId.toLongOrNull() ?: 0L,
        title = title,
        totalAmountCents = totalAmountCents,
        currency = currency,
        categoryName = categoryName,
        paidByMemberId = paidByMemberId.toLongOrNull() ?: 0L,
        paidByMemberName = paidByMemberName,
        splitType = splitType,
        receiptImagePath = receiptImagePath,
        note = note,
        createdAtEpochMs = createdAtEpochMs,
        isSettlement = isSettlement
    ),
    participants = participants.map {
        ExpenseParticipantEntity(
            expenseId = id,
            memberId = it.memberId.toLongOrNull() ?: 0L,
            memberName = it.memberName,
            amountCents = it.amountCents,
            rawShareValue = it.rawShareValue
        )
    }
)

@Composable
fun NavGraph(
    app: BabySplitApplication,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userPrefs = app.userPreferences

    val paymentDetails by userPrefs.hostPaymentDetailsFlow.collectAsState(initial = null)
    val defaultCurrency by userPrefs.defaultCurrencyFlow.collectAsState(initial = "USD")
    val userName by userPrefs.userNameFlow.collectAsState(initial = "Guest")

    // Shared DashboardViewModel – holds auth state and active repository
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinTripDialog by remember { mutableStateOf(false) }

    // Handle join trip results
    LaunchedEffect(dashboardState.joinTripResult) {
        when (val result = dashboardState.joinTripResult) {
            is JoinTripResult.Success -> {
                android.widget.Toast.makeText(context, "Joined trip successfully! 🎉", android.widget.Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.GroupDetail.createRoute(result.tripId))
                dashboardViewModel.clearJoinTripResult()
            }
            is JoinTripResult.Error -> {
                android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_LONG).show()
                dashboardViewModel.clearJoinTripResult()
            }
            null -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            // Convert TripData to GroupEntity for existing DashboardScreen
            val groupEntities = dashboardState.trips.map { it.toGroupEntity() }

            DashboardScreen(
                groups = groupEntities,
                onGroupClick = { groupId ->
                    // Find the corresponding TripData to get its string ID
                    val trip = dashboardState.trips.find { it.id.toLongOrNull() == groupId || it.id == groupId.toString() }
                    val tripId = trip?.id ?: groupId.toString()
                    navController.navigate(Screen.GroupDetail.createRoute(tripId))
                },
                onCreateGroupClick = { showCreateGroupDialog = true },
                onJoinTripClick = { showJoinTripDialog = true },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onDeleteGroup = { groupId ->
                    val trip = dashboardState.trips.find { it.id.toLongOrNull() == groupId || it.id == groupId.toString() }
                    if (trip != null) {
                        dashboardViewModel.deleteTrip(trip.id)
                    }
                },
                onRestoreDiscoveredBackups = { backups ->
                    scope.launch {
                        for (backup in backups) {
                            com.babysplit.app.core.gdrive.GoogleDriveBackupEngine.restoreTripBackup(app.database, backup)
                        }
                    }
                }
            )

            if (showCreateGroupDialog) {
                CreateGroupDialog(
                    onDismiss = { showCreateGroupDialog = false },
                    onConfirm = { name, emoji, currency, simplifyDebts ->
                        dashboardViewModel.createTrip(
                            name = name,
                            emoji = emoji,
                            currency = currency,
                            simplifyDebts = simplifyDebts,
                            hostName = userName.ifBlank { "You (Host)" },
                            hostBankName = paymentDetails?.bankName,
                            hostAccountHolderName = paymentDetails?.accountHolderName ?: userName,
                            hostBankAccountNumber = paymentDetails?.bankAccountNumber,
                            hostEWalletName = paymentDetails?.eWalletName,
                            hostEWalletHandle = paymentDetails?.eWalletHandle
                        ) { tripId ->
                            showCreateGroupDialog = false
                            navController.navigate(Screen.GroupDetail.createRoute(tripId))
                        }
                    }
                )
            }

            if (showJoinTripDialog) {
                JoinTripDialog(
                    isSignedIn = dashboardState.isSignedIn,
                    onDismiss = { showJoinTripDialog = false },
                    onJoin = { code ->
                        dashboardViewModel.joinTripByCode(code)
                        showJoinTripDialog = false
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val activeRepo = dashboardViewModel.activeRepository
            val currentUserId = dashboardViewModel.authRepository.getCurrentUser()?.uid ?: ""

            val groupDetailViewModel = remember(tripId) {
                GroupDetailViewModel(
                    tripId = tripId,
                    repository = activeRepo,
                    currentUserId = currentUserId
                )
            }
            val groupState by groupDetailViewModel.uiState.collectAsState()

            // Bridge: Convert domain models to Room entities for GroupDetailScreen
            val groupEntity = groupState.trip?.toGroupEntity()
            val memberEntities = groupState.members.map { it.toMemberEntity() }
            val expenseEntities = groupState.expenses.map { it.toExpenseWithParticipants() }

            var showInviteSheet by remember { mutableStateOf(false) }

            GroupDetailScreen(
                group = groupEntity,
                members = memberEntities,
                expensesWithParticipants = expenseEntities,
                paymentDetails = paymentDetails,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { _ ->
                    navController.navigate(Screen.AddEditExpense.createRoute(tripId))
                },
                onAddMember = { name, type, email, phone, bankName, holderName, bankAcc, walletName, walletHandle ->
                    groupDetailViewModel.addMember(
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
                },
                onUpdateMember = { memberEntity ->
                    // Convert MemberEntity back to MemberData
                    val memberData = MemberData(
                        id = memberEntity.id.toString(),
                        tripId = tripId,
                        name = memberEntity.name,
                        memberType = memberEntity.memberType,
                        email = memberEntity.email,
                        phoneNumber = memberEntity.phoneNumber,
                        avatarColorHex = memberEntity.avatarColorHex,
                        bankName = memberEntity.bankName,
                        accountHolderName = memberEntity.accountHolderName,
                        bankAccountNumber = memberEntity.bankAccountNumber,
                        eWalletName = memberEntity.eWalletName,
                        eWalletHandle = memberEntity.eWalletHandle
                    )
                    groupDetailViewModel.updateMember(memberData)
                },
                onRecordSettlement = { payerId, receiverId, amountCents ->
                    // Convert Long IDs to String for the ViewModel
                    groupDetailViewModel.recordSettlement(
                        payerMemberId = payerId.toString(),
                        receiverMemberId = receiverId.toString(),
                        amountCents = amountCents
                    )
                },
                onFinishTrip = { groupDetailViewModel.finishTrip() },
                onDeleteTrip = {
                    groupDetailViewModel.deleteTrip {
                        navController.popBackStack()
                    }
                },
                onEditExpense = { _, expId ->
                    navController.navigate(Screen.AddEditExpense.createRoute(tripId, expId))
                },
                onDeleteExpense = { expId ->
                    groupDetailViewModel.deleteExpense(expId)
                },
                onInviteClick = {
                    if (groupState.inviteCode.isBlank()) {
                        groupDetailViewModel.refreshInviteCode()
                    }
                    showInviteSheet = true
                }
            )

            if (showInviteSheet) {
                com.babysplit.app.feature.members.presentation.InviteMembersSheet(
                    inviteCode = groupState.inviteCode.ifBlank { "TRIP-$tripId" },
                    tripName = groupState.trip?.name ?: "Trip",
                    onDismiss = { showInviteSheet = false }
                )
            }
        }

        composable(
            route = Screen.AddEditExpense.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val activeRepo = dashboardViewModel.activeRepository

            // Use the repository to get live data
            val members by activeRepo.getMembersStream(tripId).collectAsState(initial = emptyList())
            val trip by activeRepo.getTripStream(tripId).collectAsState(initial = null)

            var existingExpense by remember { mutableStateOf<ExpenseWithParticipants?>(null) }

            LaunchedEffect(expenseId) {
                if (!expenseId.isNullOrBlank()) {
                    val expenseData = activeRepo.getExpenseById(tripId, expenseId)
                    existingExpense = expenseData?.toExpenseWithParticipants()
                }
            }

            // Bridge: Convert MemberData to MemberEntity for AddEditExpenseScreen
            val memberEntities = members.map { it.toMemberEntity() }

            AddEditExpenseScreen(
                groupId = tripId.toLongOrNull() ?: 0L,
                currency = trip?.currency ?: defaultCurrency,
                members = memberEntities,
                existingExpense = existingExpense,
                onBackClick = { navController.popBackStack() },
                onDeleteExpense = { expId ->
                    scope.launch {
                        activeRepo.deleteExpense(tripId, expId)
                        navController.popBackStack()
                    }
                },
                onSaveExpense = { expense ->
                    scope.launch {
                        val expenseData = ExpenseData(
                            id = expense.id,
                            tripId = tripId,
                            title = expense.title,
                            totalAmountCents = expense.totalAmountCents,
                            currency = expense.currency,
                            categoryName = expense.category.name,
                            paidByMemberId = expense.paidByMemberId.toString(),
                            paidByMemberName = expense.paidByMemberName,
                            splitType = expense.splitType.name,
                            receiptImagePath = expense.receiptImagePath,
                            note = expense.note,
                            createdAtEpochMs = expense.createdAtEpochMs,
                            isSettlement = expense.isSettlement,
                            participants = expense.participants.map {
                                ParticipantData(
                                    memberId = it.memberId.toString(),
                                    memberName = it.memberName,
                                    amountCents = it.amountCents,
                                    rawShareValue = it.rawShareValue
                                )
                            }
                        )
                        if (expenseId.isNullOrBlank()) {
                            activeRepo.addExpense(tripId, expenseData)
                        } else {
                            activeRepo.updateExpense(tripId, expenseData)
                        }
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel()
            val profileState by profileViewModel.uiState.collectAsState()

            ProfileScreen(
                currentPaymentDetails = paymentDetails,
                currentCurrency = defaultCurrency,
                userName = profileState.userName,
                userEmail = profileState.userEmail,
                isSignedIn = profileState.isSignedIn,
                isSigningIn = profileState.isSigningIn,
                authErrorMessage = profileState.errorMessage,
                onSignInClick = {
                    val clientId = context.getString(com.babysplit.app.R.string.default_web_client_id)
                    profileViewModel.signInWithGoogle(context, clientId)
                },
                onSignOutClick = { profileViewModel.signOut(context) },
                onBackClick = { navController.popBackStack() },
                onSavePaymentDetails = { bank, holder, account, wallet, handle, note, curr ->
                    profileViewModel.savePaymentDetails(bank, holder, account, wallet, handle, note, curr)
                },
                onRestoreBackups = { backups ->
                    scope.launch {
                        for (backup in backups) {
                            com.babysplit.app.core.gdrive.GoogleDriveBackupEngine.restoreTripBackup(app.database, backup)
                        }
                    }
                }
            )
        }
    }
}

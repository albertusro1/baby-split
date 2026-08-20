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
import com.babysplit.app.core.repository.ExpenseData
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.TripData
import com.babysplit.app.feature.dashboard.presentation.DashboardScreen
import com.babysplit.app.feature.dashboard.presentation.DashboardViewModel
import com.babysplit.app.feature.dashboard.presentation.JoinTripResult
import com.babysplit.app.feature.expense.presentation.AddEditExpenseScreen
import com.babysplit.app.feature.group.presentation.CreateGroupDialog
import com.babysplit.app.feature.group.presentation.GroupDetailScreen
import com.babysplit.app.feature.group.presentation.GroupDetailViewModel
import com.babysplit.app.feature.group.presentation.JoinTripDialog
import com.babysplit.app.feature.profile.presentation.ProfileScreen
import com.babysplit.app.feature.profile.presentation.ProfileViewModel
import kotlinx.coroutines.launch

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
            DashboardScreen(
                groups = dashboardState.trips,
                onGroupClick = { tripId ->
                    navController.navigate(Screen.GroupDetail.createRoute(tripId))
                },
                onCreateGroupClick = { showCreateGroupDialog = true },
                onJoinTripClick = { showJoinTripDialog = true },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onDeleteGroup = { tripId ->
                    dashboardViewModel.deleteTrip(tripId)
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

            val groupDetailViewModel = remember(tripId, activeRepo) {
                GroupDetailViewModel(
                    tripId = tripId,
                    repository = activeRepo,
                    currentUserId = currentUserId
                )
            }
            val groupState by groupDetailViewModel.uiState.collectAsState()

            LaunchedEffect(groupState.members, paymentDetails) {
                groupDetailViewModel.syncLocalUserPaymentDetails(paymentDetails)
            }

            var showInviteSheet by remember { mutableStateOf(false) }

            GroupDetailScreen(
                group = groupState.trip,
                members = groupState.members,
                expenses = groupState.expenses,
                paymentDetails = paymentDetails,
                isSignedIn = dashboardState.isSignedIn,
                isCloudTrip = groupState.trip?.isCloud == true,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { tId ->
                    navController.navigate(Screen.AddEditExpense.createRoute(tId))
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
                onUpdateMember = { memberData ->
                    groupDetailViewModel.updateMember(memberData)
                },
                onRecordSettlement = { payerId, receiverId, amountCents ->
                    groupDetailViewModel.recordSettlement(
                        payerMemberId = payerId,
                        receiverMemberId = receiverId,
                        amountCents = amountCents
                    )
                },
                onFinishTrip = { groupDetailViewModel.finishTrip() },
                onDeleteTrip = {
                    groupDetailViewModel.deleteTrip {
                        navController.popBackStack()
                    }
                },
                onEditExpense = { tId, expId ->
                    navController.navigate(Screen.AddEditExpense.createRoute(tId, expId))
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

            var existingExpense by remember { mutableStateOf<ExpenseData?>(null) }

            LaunchedEffect(expenseId) {
                if (!expenseId.isNullOrBlank()) {
                    existingExpense = activeRepo.getExpenseById(tripId, expenseId)
                }
            }

            AddEditExpenseScreen(
                tripId = tripId,
                currency = trip?.currency ?: defaultCurrency,
                members = members,
                existingExpense = existingExpense,
                onBackClick = { navController.popBackStack() },
                onDeleteExpense = { expId ->
                    scope.launch {
                        activeRepo.deleteExpense(tripId, expId)
                        navController.popBackStack()
                    }
                },
                onSaveExpense = { expenseData ->
                    scope.launch {
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
                }
            )
        }
    }
}


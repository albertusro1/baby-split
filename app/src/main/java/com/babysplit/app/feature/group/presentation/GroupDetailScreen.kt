package com.babysplit.app.feature.group.presentation

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.repository.ExpenseData
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.TripData
import com.babysplit.app.core.ui.theme.*
import com.babysplit.app.core.whatsapp.BillSummaryFormatter
import com.babysplit.app.core.whatsapp.HostPaymentDetails
import com.babysplit.app.core.whatsapp.WhatsAppShareHelper
import com.babysplit.app.feature.balance.domain.engine.BalanceCalculator
import com.babysplit.app.feature.balance.domain.engine.DebtSimplificationEngine
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.members.presentation.AddMemberDialog
import com.babysplit.app.feature.settlement.presentation.RecordSettlementDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: TripData?,
    members: List<MemberData>,
    expenses: List<ExpenseData>,
    paymentDetails: HostPaymentDetails?,
    isSignedIn: Boolean = false,
    isCloudTrip: Boolean = false,
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onAddMember: (name: String, type: String, email: String?, phone: String?, bankName: String?, holderName: String?, bankAcc: String?, walletName: String?, walletHandle: String?) -> Unit,
    onUpdateMember: (MemberData) -> Unit = {},
    onRecordSettlement: (paidByMemberId: String, paidToMemberId: String, amountCents: Long) -> Unit,
    onFinishTrip: () -> Unit,
    onDeleteTrip: () -> Unit = {},
    onEditExpense: (tripId: String, expenseId: String) -> Unit = { _, _ -> },
    onDeleteExpense: (expenseId: String) -> Unit = {},
    onInviteClick: () -> Unit = {}
) {
    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ChickAmber)
        }
        return
    }

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingPaymentMember by remember { mutableStateOf<MemberData?>(null) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var prefillSettlementPayerId by remember { mutableStateOf<String?>(null) }
    var prefillSettlementReceiverId by remember { mutableStateOf<String?>(null) }
    var prefillSettlementAmountCents by remember { mutableStateOf<Long?>(null) }
    var showFinishTripDialog by remember { mutableStateOf(false) }
    var showDeleteTripDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Expenses", "Balances", "Totals")
    val memberMap = remember(members) { members.associate { it.id to it.name } }

    val memberBalances = remember(expenses, memberMap) {
        BalanceCalculator.calculateBalances(expenses, memberMap)
    }

    val netBalanceMap = remember(memberBalances) {
        memberBalances.associate { it.memberId to it.netBalanceCents }
    }

    val simplifiedTransactions = remember(netBalanceMap, memberMap) {
        DebtSimplificationEngine.simplifyDebts(netBalanceMap, memberMap)
    }

    val totalGroupSpending = remember(expenses) {
        expenses.filter { !it.isSettlement }.sumOf { it.totalAmountCents }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${group.emoji} ${group.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text(
                            text = "${members.size} Members • ${group.currency}${if (group.isFinished) " • Settled ✅" else ""}",
                            fontSize = 12.sp,
                            color = if (group.isFinished) SettledGreen else ChickAmber
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isCloudTrip && isSignedIn) {
                        IconButton(onClick = onInviteClick) {
                            Icon(Icons.Filled.Link, contentDescription = "Share Invite Code", tint = ChickAmber)
                        }
                    }
                    if (!group.isFinished) {
                        IconButton(onClick = { showFinishTripDialog = true }) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Finish Trip", tint = SettledGreen)
                        }
                    }
                    IconButton(onClick = { showDeleteTripDialog = true }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Trip", tint = DebtRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 && !group.isFinished) {
                FloatingActionButton(
                    onClick = { onAddExpenseClick(group.id) },
                    containerColor = ChickAmber,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLight,
                contentColor = ChickAmber,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ChickAmber,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = SurfaceBorderLight) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = (selectedTab == index),
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                maxLines = 1,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) ChickAmber else TextSecondary
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ExpensesTab(
                    expenses = expenses,
                    currency = group.currency,
                    onEditExpense = { expId -> onEditExpense(group.id, expId) },
                    onDeleteExpense = onDeleteExpense,
                    onShareSingleExpense = { exp ->
                        val sb = java.lang.StringBuilder()
                        sb.appendLine("🧾 *${group.name} - Expense Split*")
                        sb.appendLine("Item: *${exp.title}*")
                        sb.appendLine("Total: ${BillSummaryFormatter.formatCents(exp.totalAmountCents, exp.currency)}")
                        sb.appendLine("Paid by: *${exp.paidByMemberName}*")
                        sb.appendLine("Shares:")
                        exp.participants.forEach {
                            sb.appendLine("• ${it.memberName}: ${BillSummaryFormatter.formatCents(it.amountCents, exp.currency)}")
                        }
                        sb.appendLine("----------------------------------------")
                        val isHostPayer = paymentDetails != null && (
                            exp.paidByMemberName.equals(paymentDetails.hostName, ignoreCase = true) ||
                            exp.paidByMemberName.contains("Host", ignoreCase = true) ||
                            exp.paidByMemberName.contains("You", ignoreCase = true)
                        )
                        if (isHostPayer && paymentDetails != null) {
                            sb.appendLine("💳 *Please transfer to ${exp.paidByMemberName} (Host):*")
                            if (!paymentDetails.bankAccountNumber.isNullOrBlank()) {
                                val bank = paymentDetails.bankName ?: "Bank"
                                val holder = if (!paymentDetails.accountHolderName.isNullOrBlank()) paymentDetails.accountHolderName else paymentDetails.hostName
                                sb.appendLine("• *$bank*: ${paymentDetails.bankAccountNumber} (a.n. $holder)")
                            }
                            if (!paymentDetails.eWalletHandle.isNullOrBlank()) {
                                val wallet = paymentDetails.eWalletName ?: "E-Wallet"
                                sb.appendLine("• *$wallet*: ${paymentDetails.eWalletHandle}")
                            }
                            if (!paymentDetails.customNote.isNullOrBlank()) {
                                sb.appendLine("• Note: ${paymentDetails.customNote}")
                            }
                        } else {
                            sb.appendLine("💳 *Please transfer to ${exp.paidByMemberName}:*")
                            sb.appendLine("• ℹ️ Please contact *${exp.paidByMemberName}* directly for their Bank / E-Wallet transfer details.")
                        }
                        WhatsAppShareHelper.shareToWhatsApp(context, sb.toString().trim())
                    }
                )
                1 -> BalancesTab(
                    groupName = group.name,
                    currency = group.currency,
                    members = members,
                    balances = memberBalances,
                    simplifiedTransactions = simplifiedTransactions,
                    expenses = expenses,
                    paymentDetails = paymentDetails,
                    onAddMemberClick = { showAddMemberDialog = true },
                    onSettleUpClick = { payerId, receiverId, amountCents ->
                        prefillSettlementPayerId = payerId
                        prefillSettlementReceiverId = receiverId
                        prefillSettlementAmountCents = amountCents
                        showSettlementDialog = true
                    },
                    onEditMemberPayment = { editingPaymentMember = it }
                )
                2 -> TotalsTab(
                    currency = group.currency,
                    totalSpendingCents = totalGroupSpending,
                    expenses = expenses,
                    balances = memberBalances
                )
            }
        }
    }

    if (showDeleteTripDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTripDialog = false },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Delete Trip? 🗑️", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${group.emoji} ${group.name}'? All expenses, member records, and settlements in this trip will be permanently removed.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteTripDialog = false
                        onDeleteTrip()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Trip", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTripDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, type, email, phone, bankName, holderName, bankAcc, walletName, walletHandle ->
                onAddMember(name, type.name, email, phone, bankName, holderName, bankAcc, walletName, walletHandle)
                showAddMemberDialog = false
            }
        )
    }

    if (editingPaymentMember != null) {
        EditMemberPaymentDialog(
            member = editingPaymentMember!!,
            onDismiss = { editingPaymentMember = null },
            onConfirm = { updatedMember ->
                onUpdateMember(updatedMember)
                editingPaymentMember = null
            }
        )
    }

    if (showSettlementDialog) {
        RecordSettlementDialog(
            members = members,
            currency = group.currency,
            initialPayerId = prefillSettlementPayerId,
            initialReceiverId = prefillSettlementReceiverId,
            initialAmountCents = prefillSettlementAmountCents,
            onDismiss = {
                showSettlementDialog = false
                prefillSettlementPayerId = null
                prefillSettlementReceiverId = null
                prefillSettlementAmountCents = null
            },
            onConfirm = { payerId, receiverId, amountCents ->
                onRecordSettlement(payerId, receiverId, amountCents)
                showSettlementDialog = false
                prefillSettlementPayerId = null
                prefillSettlementReceiverId = null
                prefillSettlementAmountCents = null
            }
        )
    }

    if (showFinishTripDialog) {
        AlertDialog(
            onDismissRequest = { showFinishTripDialog = false },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(20.dp),
            title = { Text("🏁 Finish & Settle Trip", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "This will finalize all expenses and balances for '${group.name}'.\n\n" +
                    "• All balances and settlements will be marked as complete.\n" +
                    "• You can still view full expense histories, receipts, and share WhatsApp summaries anytime.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFinishTrip()
                        showFinishTripDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SettledGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Finish Trip ✅", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishTripDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ExpensesTab(
    expenses: List<ExpenseData>,
    currency: String,
    onEditExpense: (String) -> Unit = {},
    onDeleteExpense: (String) -> Unit = {},
    onShareSingleExpense: (ExpenseData) -> Unit
) {
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseData?>(null) }

    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No expenses added yet.\nTap '+ Add Expense' below!", color = TextSecondary, textAlign = TextAlign.Center)
        }
        return
    }

    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(expenses) { expense ->
            val cat = ExpenseCategory.fromName(expense.categoryName)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, SurfaceBorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = ChickYellowLight,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(cat.emoji, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text(
                                    "Paid by ${expense.paidByMemberName} • ${dateFormatter.format(Date(expense.createdAtEpochMs))}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                BillSummaryFormatter.formatCents(expense.totalAmountCents, expense.currency),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ChickAmber
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(onClick = { onEditExpense(expense.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = ChickAmber, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { expenseToDelete = expense }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = DebtRed, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onShareSingleExpense(expense) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // If receipt photo is attached, display interactive view receipt chip
                    if (!expense.receiptImagePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingReceiptPath = expense.receiptImagePath },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ChickYellowSubtle,
                                border = BorderStroke(1.dp, ChickGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = ChickAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Receipt Photo 📸", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TurquoiseDark)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // Receipt Photo Viewer Dialog
    if (viewingReceiptPath != null) {
        AlertDialog(
            onDismissRequest = { viewingReceiptPath = null },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Receipt Photo 🧾", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { viewingReceiptPath = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    contentAlignment = Alignment.Center
                ) {
                    coil3.compose.AsyncImage(
                        model = java.io.File(viewingReceiptPath!!),
                        contentDescription = "Receipt Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewingReceiptPath = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White)
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text("Delete Expense? 🗑️", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${expenseToDelete?.title}' (${expenseToDelete?.let { BillSummaryFormatter.formatCents(it.totalAmountCents, it.currency) }})? This will recalculate all group balances.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expId = expenseToDelete?.id
                        if (expId != null) {
                            onDeleteExpense(expId)
                        }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun BalancesTab(
    groupName: String,
    currency: String,
    members: List<MemberData>,
    balances: List<com.babysplit.app.feature.balance.domain.engine.MemberBalanceSummary>,
    simplifiedTransactions: List<DebtSimplificationEngine.SimplifiedTransaction>,
    expenses: List<ExpenseData>,
    paymentDetails: HostPaymentDetails?,
    onAddMemberClick: () -> Unit = {},
    onSettleUpClick: (payerId: String?, receiverId: String?, amountCents: Long?) -> Unit,
    onEditMemberPayment: (MemberData) -> Unit = {}
) {
    val context = LocalContext.current
    val totalDebtCents = remember(simplifiedTransactions) {
        simplifiedTransactions.sumOf { it.amountCents }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Settlement Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalDebtCents > 0) Color(0xFFFFFDF7) else Color(0xFFF1F8E9)
                ),
                border = BorderStroke(
                    1.dp,
                    if (totalDebtCents > 0) ChickGold.copy(alpha = 0.8f) else Color(0xFFC8E6C9)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row: Label + Status Pill Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (totalDebtCents > 0) "⚖️ Remaining Debts" else "🎉 Settlement Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDebtCents > 0) Color(0xFF7A4F00) else SettledGreen
                        )

                        Surface(
                            color = if (totalDebtCents > 0) ChickYellowSubtle else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, if (totalDebtCents > 0) ChickGold else Color(0xFF81C784))
                        ) {
                            Text(
                                text = if (totalDebtCents > 0) {
                                    "${simplifiedTransactions.size} payment${if (simplifiedTransactions.size > 1) "s" else ""} left"
                                } else {
                                    "All Settled Up"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDebtCents > 0) Color(0xFF7A4F00) else SettledGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Amount Display
                    Column {
                        Text(
                            text = if (totalDebtCents > 0) {
                                BillSummaryFormatter.formatCents(totalDebtCents, currency)
                            } else {
                                "Rp 0"
                            },
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalDebtCents > 0) ChickAmber else SettledGreen
                        )
                        if (totalDebtCents > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total outstanding amount between group members",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Main Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSettleUpClick(null, null, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Settlement", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val totalSpend = expenses.filter { !it.isSettlement }.sumOf { it.totalAmountCents }
                                val msg = BillSummaryFormatter.formatGroupWhatsAppSummary(
                                    tripName = groupName,
                                    totalSpendingCents = totalSpend,
                                    currency = currency,
                                    simplifiedTransactions = simplifiedTransactions,
                                    paymentDetails = paymentDetails
                                )
                                WhatsAppShareHelper.shareToWhatsApp(context, msg)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppDarkGreen),
                            border = BorderStroke(1.dp, WhatsAppGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = WhatsAppDarkGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share WA", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Simplified Debts Section (Who Pays Whom)
        if (simplifiedTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "⚡ Who Pays Whom (${simplifiedTransactions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            items(simplifiedTransactions) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top: Visual Transfer Route (Debtor ➔ Arrow ➔ Creditor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Debtor (Sender)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFFEBEE),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = tx.debtorName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = DebtRed
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = tx.debtorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = TextPrimary
                                    )
                                    Text("Owes / Pays", fontSize = 10.sp, color = DebtRed, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Center Transfer Direction Icon
                            Surface(
                                shape = CircleShape,
                                color = ChickYellowSubtle,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "pays to",
                                        tint = ChickAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            // Creditor (Recipient)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = tx.creditorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = TextPrimary
                                    )
                                    Text("Receives", fontSize = 10.sp, color = SettledGreen, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE8F5E9),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = tx.creditorName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SettledGreen
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = SurfaceBorderLight.copy(alpha = 0.6f))

                        // Bottom: Amount and Quick Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Transfer Amount", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = BillSummaryFormatter.formatCents(tx.amountCents, currency),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = ChickAmber
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Quick WhatsApp Reminder Button
                                OutlinedButton(
                                    onClick = {
                                        val creditor = members.firstOrNull { it.name == tx.creditorName }
                                        val isHostCreditor = creditor?.memberType == "HOST" ||
                                            creditor?.name?.contains("Host", ignoreCase = true) == true ||
                                            (paymentDetails != null && creditor?.name.equals(paymentDetails.hostName, ignoreCase = true))

                                        val bankName = creditor?.bankName ?: if (isHostCreditor) paymentDetails?.bankName else null
                                        val bankAcc = creditor?.bankAccountNumber ?: if (isHostCreditor) paymentDetails?.bankAccountNumber else null
                                        val walletName = creditor?.eWalletName ?: if (isHostCreditor) paymentDetails?.eWalletName else null
                                        val walletHandle = creditor?.eWalletHandle ?: if (isHostCreditor) paymentDetails?.eWalletHandle else null

                                        val sb = java.lang.StringBuilder()
                                        sb.appendLine("👋 Hi *${tx.debtorName}*, here is a quick settlement reminder for *${groupName}*:")
                                        sb.appendLine("💵 Amount to transfer: *${BillSummaryFormatter.formatCents(tx.amountCents, currency)}*")
                                        sb.appendLine("👤 Transfer to: *${tx.creditorName}*")
                                        if (!bankAcc.isNullOrBlank()) {
                                            sb.appendLine("💳 *${bankName ?: "Bank"}*: $bankAcc (a.n. ${creditor?.accountHolderName ?: tx.creditorName})")
                                        } else if (!walletHandle.isNullOrBlank()) {
                                            sb.appendLine("📱 *${walletName ?: "E-Wallet"}*: $walletHandle")
                                        }
                                        sb.appendLine("----------------------------------------")
                                        sb.appendLine("Thank you! 🙏")

                                        val debtorMember = members.firstOrNull { it.name == tx.debtorName }
                                        WhatsAppShareHelper.shareToWhatsApp(context, sb.toString().trim(), debtorMember?.phoneNumber)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppDarkGreen),
                                    border = BorderStroke(1.dp, WhatsAppGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, tint = WhatsAppDarkGreen, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remind", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Quick Settle Button
                                Button(
                                    onClick = {
                                        onSettleUpClick(tx.debtorId, tx.creditorId, tx.amountCents)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Settle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Bank / Payment Info Tag if Creditor Has Bank Account
                        val creditor = members.firstOrNull { it.name == tx.creditorName }
                        val isHostCreditor = creditor?.memberType == "HOST" ||
                            creditor?.name?.contains("Host", ignoreCase = true) == true ||
                            (paymentDetails != null && creditor?.name.equals(paymentDetails.hostName, ignoreCase = true))

                        val bankName = creditor?.bankName ?: if (isHostCreditor) paymentDetails?.bankName else null
                        val bankAcc = creditor?.bankAccountNumber ?: if (isHostCreditor) paymentDetails?.bankAccountNumber else null
                        val walletName = creditor?.eWalletName ?: if (isHostCreditor) paymentDetails?.eWalletName else null
                        val walletHandle = creditor?.eWalletHandle ?: if (isHostCreditor) paymentDetails?.eWalletHandle else null

                        if (!bankAcc.isNullOrBlank() || !walletHandle.isNullOrBlank()) {
                            val transferText = if (!bankAcc.isNullOrBlank()) {
                                "${bankName ?: "Bank"}: $bankAcc (a.n. ${creditor?.accountHolderName ?: tx.creditorName})"
                            } else {
                                "${walletName ?: "E-Wallet"}: $walletHandle"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ChickYellowSubtle,
                                border = BorderStroke(1.dp, ChickGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Bank Account", bankAcc ?: walletHandle))
                                        android.widget.Toast.makeText(context, "Account details copied!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = ChickAmber, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "💳 $transferText",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF7A4F00),
                                            maxLines = 1
                                        )
                                    }
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color(0xFF7A4F00), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Members & Net Balances Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 Member Net Balances (${members.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                TextButton(
                    onClick = onAddMemberClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = ChickAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Member", fontSize = 13.sp, color = ChickAmber, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(balances) { balance ->
            val member = members.firstOrNull { it.id == balance.memberId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, SurfaceBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (balance.netBalanceCents > 0) Color(0xFFE8F5E9) else if (balance.netBalanceCents < 0) Color(0xFFFFEBEE) else BackgroundLight,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = balance.memberName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (balance.netBalanceCents > 0) SettledGreen else if (balance.netBalanceCents < 0) DebtRed else TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = balance.memberName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                val statusText = when {
                                    balance.netBalanceCents > 0 -> "Gets back ${BillSummaryFormatter.formatCents(balance.netBalanceCents, currency)}"
                                    balance.netBalanceCents < 0 -> "Owes ${BillSummaryFormatter.formatCents(-balance.netBalanceCents, currency)}"
                                    else -> "All settled up ✅"
                                }
                                val statusColor = when {
                                    balance.netBalanceCents > 0 -> SettledGreen
                                    balance.netBalanceCents < 0 -> DebtRed
                                    else -> TextSecondary
                                }
                                Text(statusText, fontSize = 13.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // WhatsApp Member Bill Button
                        IconButton(
                            onClick = {
                                val memberExpenses = mutableListOf<Pair<ExpenseData, Long>>()
                                for (exp in expenses) {
                                    val part = exp.participants.firstOrNull { it.memberId == balance.memberId }
                                    if (part != null) memberExpenses.add(exp to part.amountCents)
                                }
                                val debtorTxs = simplifiedTransactions.filter { it.debtorId == balance.memberId }
                                val creditorMap = members.associateBy { it.name }
                                val msg = BillSummaryFormatter.formatMemberWhatsAppMessage(
                                    tripName = groupName,
                                    memberName = balance.memberName,
                                    memberExpenses = memberExpenses,
                                    totalOwedCents = -balance.netBalanceCents,
                                    currency = currency,
                                    paymentDetails = paymentDetails,
                                    debtorTransactions = debtorTxs,
                                    creditorMembers = creditorMap,
                                    hostMemberName = paymentDetails?.hostName ?: "Host"
                                )
                                WhatsAppShareHelper.shareToWhatsApp(context, msg, member?.phoneNumber)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send WhatsApp Bill", tint = WhatsAppDarkGreen, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Bank / Payment info tag
                    if (balance.netBalanceCents > 0 && member != null) {
                        val isHost = member.memberType == "HOST" ||
                            member.name.contains("Host", ignoreCase = true) ||
                            member.name.contains("You", ignoreCase = true) ||
                            (paymentDetails != null && member.name.equals(paymentDetails.hostName, ignoreCase = true))

                        val effBank = member.bankName ?: if (isHost) paymentDetails?.bankName else null
                        val effAccount = member.bankAccountNumber ?: if (isHost) paymentDetails?.bankAccountNumber else null
                        val effWallet = member.eWalletName ?: if (isHost) paymentDetails?.eWalletName else null
                        val effHandle = member.eWalletHandle ?: if (isHost) paymentDetails?.eWalletHandle else null

                        val hasBank = !effAccount.isNullOrBlank() || !effHandle.isNullOrBlank()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasBank) ChickYellowSubtle else BackgroundLight,
                            border = BorderStroke(1.dp, if (hasBank) ChickGold else SurfaceBorderLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditMemberPayment(member) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = ChickAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasBank) {
                                            val bankText = if (!effAccount.isNullOrBlank()) "${effBank ?: "Bank"}: $effAccount" else "${effWallet ?: "E-Wallet"}: $effHandle"
                                            "💳 $bankText"
                                        } else {
                                            "+ Add Transfer Info (Bank / QRIS)"
                                        },
                                        fontSize = 11.sp,
                                        color = if (hasBank) Color(0xFF7A4F00) else TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
private fun TotalsTab(
    currency: String,
    totalSpendingCents: Long,
    expenses: List<ExpenseData>,
    balances: List<com.babysplit.app.feature.balance.domain.engine.MemberBalanceSummary>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Total Spending Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChickYellowLight),
                border = BorderStroke(1.dp, ChickGold)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL TRIP EXPENSES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = BillSummaryFormatter.formatCents(totalSpendingCents, currency),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChickAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val nonSettlements = expenses.filter { !it.isSettlement }
                    Text(
                        text = "${nonSettlements.size} expense${if (nonSettlements.size != 1) "s" else ""} recorded",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Spending By Category
        item {
            Text("📊 Spending by Category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        }

        val categoryGroups = expenses.filter { !it.isSettlement }.groupBy { ExpenseCategory.fromName(it.categoryName) }
        if (categoryGroups.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No spending categories to display yet", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(categoryGroups.entries.sortedByDescending { it.value.sumOf { exp -> exp.totalAmountCents } }) { (cat, exps) ->
                val sum = exps.sumOf { it.totalAmountCents }
                val progress = if (totalSpendingCents > 0) (sum.toFloat() / totalSpendingCents) else 0f
                val percent = (progress * 100).toInt()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = ChickYellowLight,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(cat.emoji, fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(cat.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text("${exps.size} item${if (exps.size != 1) "s" else ""}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = BillSummaryFormatter.formatCents(sum, currency),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = ChickAmber
                                )
                                Text("$percent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ChickAmber,
                            trackColor = ChickYellowSubtle
                        )
                    }
                }
            }
        }

        // 3. Top Contributors
        if (balances.isNotEmpty() && totalSpendingCents > 0) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("🏆 Top Paid Upfront", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }

            items(balances.filter { it.totalPaidCents > 0 }.sortedByDescending { it.totalPaidCents }) { b ->
                val paidPercent = if (totalSpendingCents > 0) (b.totalPaidCents.toFloat() / totalSpendingCents * 100).toInt() else 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BackgroundLight,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(b.memberName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(b.memberName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                        }
                        Text(
                            "${BillSummaryFormatter.formatCents(b.totalPaidCents, currency)} ($paidPercent%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SettledGreen
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun EditMemberPaymentDialog(
    member: MemberData,
    onDismiss: () -> Unit,
    onConfirm: (MemberData) -> Unit
) {
    var bankName by remember(member) { mutableStateOf(member.bankName ?: "") }
    var holderName by remember(member) { mutableStateOf(member.accountHolderName ?: member.name) }
    var bankAcc by remember(member) { mutableStateOf(member.bankAccountNumber ?: "") }
    var walletHandle by remember(member) { mutableStateOf(member.eWalletHandle ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Edit ${member.name}'s Payment Info 💳", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "When other friends owe ${member.name}, these details will be automatically attached to their WhatsApp bills:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name") },
                    placeholder = { Text("e.g. BCA, Mandiri, BRI") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Account Holder Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bankAcc,
                    onValueChange = { bankAcc = it },
                    label = { Text("Bank Account Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = walletHandle,
                    onValueChange = { walletHandle = it },
                    label = { Text("E-Wallet / QRIS (Optional)") },
                    placeholder = { Text("e.g. GoPay: 0812-3456-7890") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        member.copy(
                            bankName = bankName.ifBlank { null },
                            accountHolderName = holderName.ifBlank { null },
                            bankAccountNumber = bankAcc.ifBlank { null },
                            eWalletHandle = walletHandle.ifBlank { null }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Details 💾", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}



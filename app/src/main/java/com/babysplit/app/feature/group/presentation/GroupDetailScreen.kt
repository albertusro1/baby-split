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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
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
import com.babysplit.app.core.database.dao.ExpenseWithParticipants
import com.babysplit.app.core.database.entity.GroupEntity
import com.babysplit.app.core.database.entity.MemberEntity
import com.babysplit.app.core.ui.theme.*
import com.babysplit.app.core.whatsapp.BillSummaryFormatter
import com.babysplit.app.core.whatsapp.HostPaymentDetails
import com.babysplit.app.core.whatsapp.WhatsAppShareHelper
import com.babysplit.app.feature.balance.domain.engine.BalanceCalculator
import com.babysplit.app.feature.balance.domain.engine.DebtSimplificationEngine
import com.babysplit.app.feature.expense.domain.model.Expense
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.ExpenseParticipant
import com.babysplit.app.feature.expense.domain.model.SplitType
import com.babysplit.app.feature.members.presentation.AddMemberDialog
import com.babysplit.app.feature.settlement.presentation.RecordSettlementDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: GroupEntity?,
    members: List<MemberEntity>,
    expensesWithParticipants: List<ExpenseWithParticipants>,
    paymentDetails: HostPaymentDetails?,
    onBackClick: () -> Unit,
    onAddExpenseClick: (Long) -> Unit,
    onAddMember: (name: String, type: String, email: String?, phone: String?, bankName: String?, holderName: String?, bankAcc: String?, walletName: String?, walletHandle: String?) -> Unit,
    onUpdateMember: (MemberEntity) -> Unit = {},
    onRecordSettlement: (paidByMemberId: Long, paidToMemberId: Long, amountCents: Long) -> Unit,
    onFinishTrip: () -> Unit,
    onDeleteTrip: () -> Unit = {},
    onEditExpense: (groupId: Long, expenseId: String) -> Unit = { _, _ -> },
    onDeleteExpense: (expenseId: String) -> Unit = {}
) {
    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingPaymentMember by remember { mutableStateOf<MemberEntity?>(null) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var showFinishTripDialog by remember { mutableStateOf(false) }
    var showDeleteTripDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Expenses", "Balances & Settle", "Totals")
    val memberMap = remember(members) { members.associate { it.id to it.name } }

    val expenses = remember(expensesWithParticipants) {
        expensesWithParticipants.map { expWithParts ->
            val exp = expWithParts.expense
            Expense(
                id = exp.id,
                groupId = exp.groupId,
                title = exp.title,
                totalAmountCents = exp.totalAmountCents,
                currency = exp.currency,
                category = ExpenseCategory.fromName(exp.categoryName),
                paidByMemberId = exp.paidByMemberId,
                paidByMemberName = exp.paidByMemberName,
                splitType = SplitType.valueOf(exp.splitType),
                participants = expWithParts.participants.map {
                    ExpenseParticipant(it.memberId, it.memberName, it.amountCents, it.rawShareValue)
                },
                receiptImagePath = exp.receiptImagePath,
                note = exp.note,
                createdAtEpochMs = exp.createdAtEpochMs,
                isSettlement = exp.isSettlement
            )
        }
    }

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
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Member", tint = ChickAmber)
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
                contentColor = ChickAmber
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = (selectedTab == index),
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
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
                    onSettleUpClick = { showSettlementDialog = true },
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
            onDismiss = { showSettlementDialog = false },
            onConfirm = { payerId, receiverId, amountCents ->
                onRecordSettlement(payerId, receiverId, amountCents)
                showSettlementDialog = false
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
                    "• Trip records and receipts will be safely archived to Google Drive (if connected).\n" +
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
    expenses: List<Expense>,
    currency: String,
    onEditExpense: (String) -> Unit = {},
    onDeleteExpense: (String) -> Unit = {},
    onShareSingleExpense: (Expense) -> Unit
) {
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

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
                                    Text(expense.category.emoji, fontSize = 20.sp)
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
    members: List<MemberEntity>,
    balances: List<com.babysplit.app.feature.balance.domain.engine.MemberBalanceSummary>,
    simplifiedTransactions: List<DebtSimplificationEngine.SimplifiedTransaction>,
    expenses: List<Expense>,
    paymentDetails: HostPaymentDetails?,
    onSettleUpClick: () -> Unit,
    onEditMemberPayment: (MemberEntity) -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Group WhatsApp Share Header Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WhatsAppLight),
                border = BorderStroke(1.dp, WhatsAppGreen)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📢 Share Entire Trip to WhatsApp Group", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F5132))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Sends full spending total, simplified who-pays-whom settlement list, and host bank/e-wallet details to group chat.",
                        fontSize = 12.sp,
                        color = Color(0xFF146C43)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
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
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Group Summary to WhatsApp 💬", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Individual Balances", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Button(
                    onClick = onSettleUpClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settle Up", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(balance.memberName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
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

                        // If member is owed money (creditor), show bank info chip with quick-edit
                        if (balance.netBalanceCents > 0 && member != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val hasBank = !member.bankAccountNumber.isNullOrBlank() || !member.eWalletHandle.isNullOrBlank()
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (hasBank) ChickYellowSubtle else BackgroundLight,
                                border = BorderStroke(1.dp, if (hasBank) ChickGold else SurfaceBorderLight),
                                modifier = Modifier.clickable { onEditMemberPayment(member) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = ChickAmber, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (hasBank) {
                                            val bankText = if (!member.bankAccountNumber.isNullOrBlank()) "${member.bankName ?: "Bank"}: ${member.bankAccountNumber}" else member.eWalletHandle ?: ""
                                            "💳 $bankText ✏️"
                                        } else {
                                            "+ Add Bank / QRIS ✏️"
                                        },
                                        fontSize = 11.sp,
                                        color = if (hasBank) Color(0xFF7A4F00) else TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Explicit Labeled Member WhatsApp Breakdown Share
                    OutlinedButton(
                        onClick = {
                            val memberExpenses = mutableListOf<Pair<Expense, Long>>()
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppDarkGreen),
                        border = BorderStroke(1.dp, WhatsAppGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = WhatsAppDarkGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send WA Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Simplified Repayments (${simplifiedTransactions.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }

        if (simplifiedTransactions.isEmpty()) {
            item {
                Text("No outstanding debts 🎉", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            items(simplifiedTransactions) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevatedLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${tx.debtorName} ➔ ${tx.creditorName}", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(
                            BillSummaryFormatter.formatCents(tx.amountCents, currency),
                            fontWeight = FontWeight.Bold,
                            color = ChickAmber
                        )
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
    expenses: List<Expense>,
    balances: List<com.babysplit.app.feature.balance.domain.engine.MemberBalanceSummary>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Group Spending", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        BillSummaryFormatter.formatCents(totalSpendingCents, currency),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Text("Spending By Category", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        val categoryGroups = expenses.filter { !it.isSettlement }.groupBy { it.category }
        items(categoryGroups.entries.toList()) { (cat, exps) ->
            val sum = exps.sumOf { it.totalAmountCents }
            val percent = if (totalSpendingCents > 0) (sum.toFloat() / totalSpendingCents * 100).toInt() else 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(cat.displayName, fontWeight = FontWeight.Medium)
                    }
                    Text("${BillSummaryFormatter.formatCents(sum, currency)} ($percent%)", fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun EditMemberPaymentDialog(
    member: MemberEntity,
    onDismiss: () -> Unit,
    onConfirm: (MemberEntity) -> Unit
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


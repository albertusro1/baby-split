package com.babysplit.app.feature.group.presentation

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onAddMember: (name: String, type: String, email: String?, phone: String?) -> Unit,
    onRecordSettlement: (paidByMemberId: Long, paidToMemberId: Long, amountCents: Long) -> Unit,
    onFinishTrip: () -> Unit
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
    var showSettlementDialog by remember { mutableStateOf(false) }
    var showFinishTripDialog by remember { mutableStateOf(false) }

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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 && !group.isFinished) {
                ExtendedFloatingActionButton(
                    onClick = { onAddExpenseClick(group.id) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White) },
                    text = { Text("Add Expense", fontWeight = FontWeight.Bold, color = Color.White) },
                    containerColor = ChickAmber,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                )
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
                    onShareSingleExpense = { exp ->
                        val msg = "🧾 *${group.name} - Expense Split*\n" +
                                "Item: *${exp.title}*\n" +
                                "Total: ${BillSummaryFormatter.formatCents(exp.totalAmountCents, exp.currency)}\n" +
                                "Paid by: ${exp.paidByMemberName}\n" +
                                "Shares:\n" +
                                exp.participants.joinToString("\n") { "• ${it.memberName}: ${BillSummaryFormatter.formatCents(it.amountCents, exp.currency)}" }
                        WhatsAppShareHelper.shareToWhatsApp(context, msg)
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
                    onSettleUpClick = { showSettlementDialog = true }
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

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, type, email, phone ->
                onAddMember(name, type.name, email, phone)
                showAddMemberDialog = false
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
    onShareSingleExpense: (Expense) -> Unit
) {
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }

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
                                fontSize = 15.sp,
                                color = ChickAmber
                            )
                            IconButton(onClick = { onShareSingleExpense(expense) }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = WhatsAppGreen)
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
                                    Text("View Receipt Photo 📸", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A4F00))
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
    onSettleUpClick: () -> Unit
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
                    }

                    // Explicit Labeled Member WhatsApp Breakdown Share
                    OutlinedButton(
                        onClick = {
                            val memberExpenses = mutableListOf<Pair<Expense, Long>>()
                            for (exp in expenses) {
                                val part = exp.participants.firstOrNull { it.memberId == balance.memberId }
                                if (part != null) memberExpenses.add(exp to part.amountCents)
                            }
                            val msg = BillSummaryFormatter.formatMemberWhatsAppMessage(
                                tripName = groupName,
                                memberName = balance.memberName,
                                memberExpenses = memberExpenses,
                                totalOwedCents = -balance.netBalanceCents,
                                currency = currency,
                                paymentDetails = paymentDetails
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


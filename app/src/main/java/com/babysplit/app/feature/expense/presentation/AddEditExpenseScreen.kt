package com.babysplit.app.feature.expense.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.camera.ReceiptCompressor
import com.babysplit.app.core.database.entity.MemberEntity
import com.babysplit.app.core.ui.theme.*
import com.babysplit.app.core.whatsapp.BillSummaryFormatter
import com.babysplit.app.feature.expense.domain.engine.SplitCalculator
import com.babysplit.app.feature.expense.domain.model.Expense
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.SplitType
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    groupId: Long,
    currency: String,
    members: List<MemberEntity>,
    onBackClick: () -> Unit,
    onSaveExpense: (Expense) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var paidByMemberId by remember { mutableStateOf(members.firstOrNull()?.id ?: 0L) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var note by remember { mutableStateOf("") }
    var receiptPath by remember { mutableStateOf<String?>(null) }

    // Equal split member selection map (id -> included)
    val equalSelectionMap = remember(members) {
        mutableStateMapOf<Long, Boolean>().apply {
            members.forEach { put(it.id, true) }
        }
    }

    // Input state map for custom split types
    val memberInputs = remember(members) {
        mutableStateMapOf<Long, String>().apply {
            members.forEach { put(it.id, "1") }
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = ReceiptCompressor.compressAndSaveReceipt(context, uri)
                receiptPath = savedPath
            }
        }
    }

    // Parse raw digits from amountInput into integer cents
    val totalAmountCents = remember(amountInput) {
        val clean = amountInput.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".")
        val doubleVal = clean.toDoubleOrNull() ?: 0.0
        (doubleVal * 100).roundToLong()
    }

    // Mode-switch helper to auto-populate smart defaults
    fun onSplitTypeChanged(newType: SplitType) {
        splitType = newType
        val n = members.size.coerceAtLeast(1)
        when (newType) {
            SplitType.EQUAL -> {
                members.forEach { equalSelectionMap[it.id] = true }
            }
            SplitType.PERCENTAGE -> {
                val defaultPct = (100.0 / n)
                members.forEach {
                    memberInputs[it.id] = if (defaultPct % 1.0 == 0.0) defaultPct.toInt().toString() else String.format(java.util.Locale.US, "%.1f", defaultPct)
                }
            }
            SplitType.EXACT -> {
                val defaultPerPerson = if (totalAmountCents > 0) (totalAmountCents / 100) / n else 0L
                members.forEach { memberInputs[it.id] = defaultPerPerson.toString() }
            }
            SplitType.SHARE -> {
                members.forEach { memberInputs[it.id] = "1" }
            }
            SplitType.ADJUSTMENT -> {
                members.forEach { memberInputs[it.id] = "0" }
            }
            else -> {}
        }
    }

    // Real-time calculation of participants and validation status
    val (calculatedParticipants, isValidSplit, validationMessage) = remember(
        totalAmountCents,
        splitType,
        memberInputs.toMap(),
        equalSelectionMap.toMap(),
        members
    ) {
        if (totalAmountCents <= 0) {
            return@remember Triple(emptyList(), false, "Please enter an expense amount.")
        }

        when (splitType) {
            SplitType.EQUAL -> {
                val activeMembers = members.filter { equalSelectionMap[it.id] == true }
                if (activeMembers.isEmpty()) {
                    Triple(emptyList(), false, "Select at least 1 person to split with.")
                } else {
                    val inputs = activeMembers.map { SplitCalculator.MemberInput(it.id, it.name, 1.0) }
                    val parts = SplitCalculator.calculateSplit(totalAmountCents, inputs, SplitType.EQUAL)
                    Triple(parts, true, null)
                }
            }
            SplitType.PERCENTAGE -> {
                val sumPct = members.sumOf { memberInputs[it.id]?.toDoubleOrNull() ?: 0.0 }
                val remainingPct = 100.0 - sumPct
                val isValid = abs(remainingPct) < 0.01
                val inputs = members.map {
                    SplitCalculator.MemberInput(it.id, it.name, memberInputs[it.id]?.toDoubleOrNull() ?: 0.0)
                }
                val parts = SplitCalculator.calculateSplit(totalAmountCents, inputs, SplitType.PERCENTAGE)
                val msg = if (!isValid) {
                    val formattedSum = if (sumPct % 1.0 == 0.0) sumPct.toInt().toString() else String.format(java.util.Locale.US, "%.1f", sumPct)
                    val formattedRem = if (remainingPct % 1.0 == 0.0) remainingPct.toInt().toString() else String.format(java.util.Locale.US, "%.1f", remainingPct)
                    "⚠️ Total is $formattedSum% ($formattedRem% remaining). Must equal 100%."
                } else null
                Triple(parts, isValid, msg)
            }
            SplitType.EXACT -> {
                val sumExactDollars = members.sumOf { memberInputs[it.id]?.toDoubleOrNull() ?: 0.0 }
                val sumExactCents = (sumExactDollars * 100).roundToLong()
                val diffCents = totalAmountCents - sumExactCents
                val isValid = diffCents == 0L
                val inputs = members.map {
                    val cents = ((memberInputs[it.id]?.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                    SplitCalculator.MemberInput(it.id, it.name, cents.toDouble())
                }
                val parts = SplitCalculator.calculateSplit(totalAmountCents, inputs, SplitType.EXACT)
                val msg = if (!isValid) {
                    "⚠️ Allocated: ${BillSummaryFormatter.formatCents(sumExactCents, currency)} (${BillSummaryFormatter.formatCents(diffCents, currency)} remaining)."
                } else null
                Triple(parts, isValid, msg)
            }
            SplitType.SHARE -> {
                val sumShares = members.sumOf { memberInputs[it.id]?.toDoubleOrNull() ?: 0.0 }
                val isValid = sumShares > 0.0
                val inputs = members.map {
                    SplitCalculator.MemberInput(it.id, it.name, memberInputs[it.id]?.toDoubleOrNull() ?: 0.0)
                }
                val parts = SplitCalculator.calculateSplit(totalAmountCents, inputs, SplitType.SHARE)
                val msg = if (!isValid) "⚠️ Total shares must be greater than 0." else null
                Triple(parts, isValid, msg)
            }
            SplitType.ADJUSTMENT -> {
                val inputs = members.map {
                    val adjCents = ((memberInputs[it.id]?.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                    SplitCalculator.MemberInput(it.id, it.name, adjCents.toDouble())
                }
                val parts = SplitCalculator.calculateSplit(totalAmountCents, inputs, SplitType.ADJUSTMENT)
                Triple(parts, true, null)
            }
            else -> Triple(emptyList(), false, null)
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Add Expense 💸", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceLight,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (validationMessage != null && totalAmountCents > 0) {
                        Text(
                            text = validationMessage,
                            color = DebtRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Button(
                        onClick = {
                            val paidByName = members.firstOrNull { it.id == paidByMemberId }?.name ?: "Host"
                            val expense = Expense(
                                groupId = groupId,
                                title = title.ifBlank { selectedCategory.displayName },
                                totalAmountCents = totalAmountCents,
                                currency = currency,
                                category = selectedCategory,
                                paidByMemberId = paidByMemberId,
                                paidByMemberName = paidByName,
                                splitType = splitType,
                                participants = calculatedParticipants,
                                receiptImagePath = receiptPath,
                                note = note.ifBlank { null }
                            )
                            onSaveExpense(expense)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = totalAmountCents > 0 && isValidSplit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChickAmber,
                            disabledContainerColor = SurfaceBorderLight,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isValidSplit && totalAmountCents > 0) "💾 Save Expense (${BillSummaryFormatter.formatCents(totalAmountCents, currency)})" else "Save Expense",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Amount Input Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ChickYellowLight),
                    border = BorderStroke(1.dp, ChickGold)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL AMOUNT ($currency)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A4F00))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
                                amountInput = filtered
                            },
                            placeholder = {
                                Text(
                                    "0",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceLight,
                                unfocusedContainerColor = SurfaceLight,
                                focusedBorderColor = ChickAmber,
                                unfocusedBorderColor = SurfaceBorderLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Title / Description
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description / What for?") },
                    placeholder = { Text("e.g. Seafood Dinner, Villa, Grab Taxi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 3. Category Selector
            item {
                Text("Category", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpenseCategory.entries.filter { it != ExpenseCategory.SETTLEMENT }) { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.emoji} ${cat.displayName}", fontSize = 13.sp) }
                        )
                    }
                }
            }

            // 4. Paid By Selector
            item {
                Text("Paid By", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members) { member ->
                        FilterChip(
                            selected = (paidByMemberId == member.id),
                            onClick = { paidByMemberId = member.id },
                            label = { Text(member.name, fontWeight = if (paidByMemberId == member.id) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // 5. Split Method Selector
            item {
                Text("Split Method", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        SplitType.EQUAL to "=",
                        SplitType.PERCENTAGE to "%",
                        SplitType.EXACT to "$",
                        SplitType.SHARE to "Shares",
                        SplitType.ADJUSTMENT to "+/-"
                    ).forEach { (st, label) ->
                        FilterChip(
                            selected = (splitType == st),
                            onClick = { onSplitTypeChanged(st) },
                            label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Split Breakdown Card with Real-time Calculations
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Split Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text(
                                text = when (splitType) {
                                    SplitType.EQUAL -> "Equally divided"
                                    SplitType.PERCENTAGE -> "Percentage (%)"
                                    SplitType.EXACT -> "Exact amounts ($currency)"
                                    SplitType.SHARE -> "Weighted shares"
                                    SplitType.ADJUSTMENT -> "Base ± adjustments"
                                    else -> ""
                                },
                                fontSize = 12.sp,
                                color = ChickAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Member rows
                        members.forEach { member ->
                            val participant = calculatedParticipants.firstOrNull { it.memberId == member.id }
                            val allocatedCents = participant?.amountCents ?: 0L

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (splitType == SplitType.EQUAL) {
                                        Checkbox(
                                            checked = equalSelectionMap[member.id] == true,
                                            onCheckedChange = { checked ->
                                                equalSelectionMap[member.id] = checked
                                            }
                                        )
                                    }
                                    Text(
                                        text = member.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (equalSelectionMap[member.id] == false && splitType == SplitType.EQUAL) TextTertiary else TextPrimary
                                    )
                                }

                                if (splitType != SplitType.EQUAL) {
                                    OutlinedTextField(
                                        value = memberInputs[member.id] ?: "",
                                        onValueChange = { newVal ->
                                            val filtered = newVal.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".")
                                            if (splitType == SplitType.PERCENTAGE) {
                                                val pctVal = filtered.toDoubleOrNull() ?: 0.0
                                                if (pctVal <= 100.0) {
                                                    memberInputs[member.id] = filtered
                                                }
                                            } else {
                                                memberInputs[member.id] = filtered
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .width(90.dp)
                                            .padding(end = 10.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.End)
                                    )
                                }

                                Text(
                                    text = BillSummaryFormatter.formatCents(allocatedCents, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (allocatedCents > 0) ChickAmber else TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // 7. Receipt Photo Attachment
            item {
                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (receiptPath != null) SettledGreen else SurfaceBorderLight)
                ) {
                    Icon(
                        if (receiptPath != null) Icons.Filled.CheckCircle else Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = if (receiptPath != null) SettledGreen else ChickAmber
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (receiptPath != null) "Receipt Attached ✅ (Tap to Change)" else "Attach Receipt Photo (Optional)",
                        color = if (receiptPath != null) SettledGreen else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}


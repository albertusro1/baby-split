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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.camera.ReceiptCompressor
import com.babysplit.app.core.repository.ExpenseData
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.ParticipantData
import com.babysplit.app.core.ui.theme.*
import com.babysplit.app.core.whatsapp.BillSummaryFormatter
import com.babysplit.app.feature.expense.domain.engine.SplitCalculator
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.ExpenseParticipant
import com.babysplit.app.feature.expense.domain.model.SplitType
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToLong

private fun sanitizeNumberInput(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".")
    val parts = filtered.split(".")
    val normalized = if (parts.size > 2) parts[0] + "." + parts.drop(1).joinToString("") else filtered
    return if (normalized.length > 1 && normalized.startsWith("0") && normalized[1] != '.') {
        normalized.trimStart('0').ifEmpty { "0" }
    } else {
        normalized
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    tripId: String,
    currency: String = "USD",
    members: List<MemberData>,
    existingExpense: ExpenseData? = null,
    onBackClick: () -> Unit,
    onSaveExpense: (ExpenseData) -> Unit,
    onDeleteExpense: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var title by remember(existingExpense) {
        mutableStateOf(existingExpense?.title ?: "")
    }
    var amountInput by remember(existingExpense) {
        mutableStateOf(
            if (existingExpense != null) {
                val cents = existingExpense.totalAmountCents
                if (currency in listOf("IDR", "VND", "JPY")) (cents / 100).toString() else String.format(java.util.Locale.US, "%.2f", cents / 100.0)
            } else ""
        )
    }
    var selectedCategory by remember(existingExpense) {
        mutableStateOf(
            if (existingExpense != null) ExpenseCategory.fromName(existingExpense.categoryName) else ExpenseCategory.FOOD
        )
    }
    var paidByMemberId by remember(members, existingExpense) {
        mutableStateOf(existingExpense?.paidByMemberId ?: members.firstOrNull()?.id ?: "")
    }
    var splitType by remember(existingExpense) {
        mutableStateOf(
            if (existingExpense != null) {
                try { SplitType.valueOf(existingExpense.splitType) } catch (e: Exception) { SplitType.EQUAL }
            } else SplitType.EQUAL
        )
    }
    var note by remember(existingExpense) {
        mutableStateOf(existingExpense?.note ?: "")
    }
    var receiptPath by remember(existingExpense) {
        mutableStateOf<String?>(existingExpense?.receiptImagePath)
    }

    LaunchedEffect(members) {
        if (paidByMemberId.isBlank() && members.isNotEmpty()) {
            paidByMemberId = members.first().id
        }
    }

    val equalSelectionMap = remember(members, existingExpense) {
        mutableStateMapOf<String, Boolean>().apply {
            members.forEach { m ->
                put(m.id, existingExpense == null || existingExpense.participants.any { it.memberId == m.id })
            }
        }
    }

    val memberInputs = remember(members, existingExpense) {
        mutableStateMapOf<String, String>().apply {
            val n = members.size.coerceAtLeast(1)
            members.forEach { m ->
                val p = existingExpense?.participants?.firstOrNull { it.memberId == m.id }
                if (existingExpense != null && p != null) {
                    val initialVal = when (existingExpense.splitType) {
                        "EXACT" -> {
                            val major = p.amountCents / 100.0
                            if (currency in listOf("IDR", "VND", "JPY")) major.toLong().toString() else String.format(java.util.Locale.US, "%.2f", major)
                        }
                        "ADJUSTMENT" -> {
                            val major = p.rawShareValue / 100.0
                            if (currency in listOf("IDR", "VND", "JPY")) major.toLong().toString() else String.format(java.util.Locale.US, "%.2f", major)
                        }
                        "PERCENTAGE" -> {
                            if (p.rawShareValue % 1.0 == 0.0) p.rawShareValue.toInt().toString() else p.rawShareValue.toString()
                        }
                        "SHARE" -> {
                            if (p.rawShareValue % 1.0 == 0.0) p.rawShareValue.toInt().toString() else p.rawShareValue.toString()
                        }
                        else -> "1"
                    }
                    put(m.id, initialVal)
                } else {
                    val defaultVal = when (splitType) {
                        SplitType.PERCENTAGE -> (100.0 / n).toInt().toString()
                        SplitType.EXACT -> {
                            val base = if (totalAmountCents > 0) (totalAmountCents / 100.0) / n else 0.0
                            if (currency in listOf("IDR", "VND", "JPY")) base.toLong().toString() else String.format(java.util.Locale.US, "%.2f", base)
                        }
                        SplitType.SHARE -> "1"
                        SplitType.ADJUSTMENT -> "0"
                        else -> "1"
                    }
                    put(m.id, defaultVal)
                }
            }
        }
    }

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

    val totalAmountCents = remember(amountInput) {
        val clean = amountInput.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".")
        val doubleVal = clean.toDoubleOrNull() ?: 0.0
        (doubleVal * 100).roundToLong()
    }

    fun onSplitTypeChanged(newType: SplitType) {
        splitType = newType
        val n = members.size.coerceAtLeast(1)
        when (newType) {
            SplitType.EQUAL -> {
                members.forEach { equalSelectionMap[it.id] = true }
            }
            SplitType.PERCENTAGE -> {
                val base = 100.0 / n
                val roundedBase = String.format(java.util.Locale.US, "%.1f", base)
                members.forEach { memberInputs[it.id] = roundedBase }
            }
            SplitType.EXACT -> {
                val base = if (totalAmountCents > 0) (totalAmountCents / 100.0) / n else 0.0
                val formatted = if (currency in listOf("IDR", "VND", "JPY")) base.toLong().toString() else String.format(java.util.Locale.US, "%.2f", base)
                members.forEach { memberInputs[it.id] = formatted }
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

    val memberInputList = remember(members, equalSelectionMap.toMap(), memberInputs.toMap(), splitType) {
        members.map { m ->
            val isSelected = equalSelectionMap[m.id] ?: true
            val rawStr = memberInputs[m.id] ?: "0"
            val rawVal = when (splitType) {
                SplitType.EQUAL -> if (isSelected) 1.0 else 0.0
                SplitType.EXACT -> {
                    val clean = rawStr.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".")
                    val num = clean.toDoubleOrNull() ?: 0.0
                    (num * 100).roundToLong().toDouble()
                }
                SplitType.ADJUSTMENT -> {
                    val clean = rawStr.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }.replace(",", ".")
                    val num = clean.toDoubleOrNull() ?: 0.0
                    (num * 100).roundToLong().toDouble()
                }
                else -> rawStr.toDoubleOrNull() ?: 0.0
            }
            SplitCalculator.MemberInput(
                memberId = m.id,
                memberName = m.name,
                inputValue = rawVal
            )
        }
    }

    val calculatedParticipants = remember(totalAmountCents, memberInputList, splitType) {
        if (totalAmountCents <= 0) emptyList()
        else SplitCalculator.calculateSplit(totalAmountCents, memberInputList, splitType)
    }

    val totalSplitCents = remember(calculatedParticipants) {
        calculatedParticipants.sumOf { it.amountCents }
    }
    val splitDifferenceCents = remember(totalAmountCents, totalSplitCents) {
        abs(totalAmountCents - totalSplitCents)
    }
    val isValidSplit = remember(totalAmountCents, totalSplitCents, calculatedParticipants, splitType) {
        if (totalAmountCents <= 0) false
        else when (splitType) {
            SplitType.EQUAL -> calculatedParticipants.any { it.amountCents > 0 }
            SplitType.PERCENTAGE -> {
                val totalPercent = memberInputs.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                abs(totalPercent - 100.0) < 0.1
            }
            SplitType.EXACT -> totalSplitCents == totalAmountCents
            SplitType.SHARE -> memberInputs.values.any { (it.toDoubleOrNull() ?: 0.0) > 0 }
            SplitType.ADJUSTMENT -> totalSplitCents == totalAmountCents
            else -> true
        }
    }

    if (showDeleteConfirmDialog && existingExpense != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Delete Expense? 🗑️", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${existingExpense.title}'?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExpense?.invoke(existingExpense.id)
                    showDeleteConfirmDialog = false
                    onBackClick()
                }) { Text("Delete", color = DebtRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundLight,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (existingExpense != null) "Edit Expense ✏️" else "Add Expense 💸", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existingExpense != null && onDeleteExpense != null) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Expense", tint = DebtRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceLight,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (!isValidSplit && totalAmountCents > 0) {
                        Text(
                            text = when (splitType) {
                                SplitType.PERCENTAGE -> "Total percentage must equal 100% (currently ${memberInputs.values.sumOf { it.toDoubleOrNull() ?: 0.0 }}%)"
                                SplitType.EXACT -> {
                                    val diff = totalAmountCents - totalSplitCents
                                    if (diff > 0) "Remaining unallocated: ${BillSummaryFormatter.formatCents(diff, currency)}"
                                    else "Over-allocated by ${BillSummaryFormatter.formatCents(-diff, currency)}"
                                }
                                SplitType.SHARE -> "At least one member must have > 0 shares"
                                SplitType.ADJUSTMENT -> "Total adjustments must balance the total amount"
                                else -> "Please select at least one member"
                            },
                            color = DebtRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Button(
                        onClick = {
                            val paidByName = members.firstOrNull { it.id == paidByMemberId }?.name ?: "Host"
                            val expense = ExpenseData(
                                id = existingExpense?.id ?: java.util.UUID.randomUUID().toString(),
                                tripId = tripId,
                                title = title.ifBlank { selectedCategory.displayName },
                                totalAmountCents = totalAmountCents,
                                currency = currency,
                                categoryName = selectedCategory.name,
                                paidByMemberId = paidByMemberId,
                                paidByMemberName = paidByName,
                                splitType = splitType.name,
                                participants = calculatedParticipants.map {
                                    ParticipantData(
                                        memberId = it.memberId,
                                        memberName = it.memberName,
                                        amountCents = it.amountCents,
                                        rawShareValue = it.rawShareValue
                                    )
                                },
                                receiptImagePath = receiptPath,
                                note = note.ifBlank { null }
                            )
                            onSaveExpense(expense)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = totalAmountCents > 0 && isValidSplit && paidByMemberId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChickAmber,
                            disabledContainerColor = SurfaceBorderLight,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (existingExpense != null) "Update Expense 💾" else "Save Expense 💸", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                                amountInput = sanitizeNumberInput(input)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && (amountInput == "0" || amountInput == "0.00" || amountInput == "0.0")) {
                                        amountInput = ""
                                    }
                                }
                        )
                    }
                }
            }

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

            // 5. Split Method Selector - Clean, spacious, scrollable
            item {
                Text("Split Method", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val splitMethods = listOf(
                        SplitType.EQUAL to "= Equal",
                        SplitType.PERCENTAGE to "% Percent",
                        SplitType.EXACT to "$ Exact",
                        SplitType.SHARE to "➗ Shares",
                        SplitType.ADJUSTMENT to "± Adjust"
                    )
                    items(splitMethods) { (st, label) ->
                        FilterChip(
                            selected = (splitType == st),
                            onClick = { onSplitTypeChanged(st) },
                            label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 6. Split Breakdown Card with Locked Column Alignments
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

                        // Member rows with locked column widths so inputs never shift position
                        members.forEach { member ->
                            val participant = calculatedParticipants.firstOrNull { it.memberId == member.id }
                            val allocatedCents = participant?.amountCents ?: 0L

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Member Name (expands to take available space)
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
                                        maxLines = 1,
                                        color = if (equalSelectionMap[member.id] == false && splitType == SplitType.EQUAL) TextTertiary else TextPrimary
                                    )
                                }

                                // 2. Input Box (Fixed width and position for every member)
                                if (splitType != SplitType.EQUAL) {
                                    OutlinedTextField(
                                        value = memberInputs[member.id] ?: "",
                                        onValueChange = { newVal ->
                                            val cleaned = sanitizeNumberInput(newVal)
                                            if (splitType == SplitType.PERCENTAGE) {
                                                val pctVal = cleaned.toDoubleOrNull() ?: 0.0
                                                if (pctVal <= 100.0) {
                                                    memberInputs[member.id] = cleaned
                                                }
                                            } else {
                                                memberInputs[member.id] = cleaned
                                            }
                                        },
                                        placeholder = { Text("0", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .width(95.dp)
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    val current = memberInputs[member.id]
                                                    if (current == "0" || (splitType == SplitType.SHARE && current == "1")) {
                                                        memberInputs[member.id] = ""
                                                    }
                                                }
                                            },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.End)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                // 3. Formatted Total (Fixed width and end-aligned)
                                Text(
                                    text = BillSummaryFormatter.formatCents(allocatedCents, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(95.dp),
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


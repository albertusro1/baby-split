package com.babysplit.app.feature.expense.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.camera.ReceiptCompressor
import com.babysplit.app.core.database.entity.MemberEntity
import com.babysplit.app.core.whatsapp.BillSummaryFormatter
import com.babysplit.app.feature.expense.domain.engine.SplitCalculator
import com.babysplit.app.feature.expense.domain.model.Expense
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.ExpenseParticipant
import com.babysplit.app.feature.expense.domain.model.SplitType
import kotlinx.coroutines.launch
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

    // Map of member inputs for Exact/Percent/Share/Adj
    val memberInputs = remember(members) {
        mutableStateMapOf<Long, String>().apply {
            members.forEach { put(it.id, "1.0") }
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

    val totalAmountCents = remember(amountInput) {
        val cleaned = amountInput.replace(",", ".")
        val doubleVal = cleaned.toDoubleOrNull() ?: 0.0
        (doubleVal * 100).roundToLong()
    }

    val calculatedParticipants = remember(totalAmountCents, splitType, memberInputs.toMap(), members) {
        val calcInputs = members.map { member ->
            val inputVal = memberInputs[member.id]?.toDoubleOrNull() ?: 0.0
            SplitCalculator.MemberInput(member.id, member.name, inputVal)
        }
        SplitCalculator.calculateSplit(totalAmountCents, calcInputs, splitType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Amount Input Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Amount in $currency", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            placeholder = { Text("0.00", fontSize = 28.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description / What for?") },
                    placeholder = { Text("e.g. Dinner, Taxi, Villa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpenseCategory.entries.filter { it != ExpenseCategory.SETTLEMENT }) { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.emoji} ${cat.displayName}") }
                        )
                    }
                }
            }

            item {
                Text("Paid By", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members) { member ->
                        FilterChip(
                            selected = (paidByMemberId == member.id),
                            onClick = { paidByMemberId = member.id },
                            label = { Text(member.name) }
                        )
                    }
                }
            }

            item {
                Text("Split Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        SplitType.EQUAL to "=",
                        SplitType.EXACT to "$",
                        SplitType.PERCENTAGE to "%",
                        SplitType.SHARE to "Shares",
                        SplitType.ADJUSTMENT to "+/-"
                    ).forEach { (st, label) ->
                        FilterChip(
                            selected = (splitType == st),
                            onClick = { splitType = st },
                            label = { Text(label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Split Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        calculatedParticipants.forEach { participant ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(participant.memberName, fontWeight = FontWeight.Medium)

                                if (splitType != SplitType.EQUAL) {
                                    OutlinedTextField(
                                        value = memberInputs[participant.memberId] ?: "",
                                        onValueChange = { memberInputs[participant.memberId] = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(100.dp),
                                        singleLine = true
                                    )
                                }

                                Text(
                                    BillSummaryFormatter.formatCents(participant.amountCents, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Receipt Attachment
                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (receiptPath != null) "Receipt Attached ✅ (Change)" else "Attach Receipt Photo")
                }
            }

            item {
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = totalAmountCents > 0
                ) {
                    Text("Save Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

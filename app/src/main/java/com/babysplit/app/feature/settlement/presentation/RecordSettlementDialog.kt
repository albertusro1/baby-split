package com.babysplit.app.feature.settlement.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.database.entity.MemberEntity
import kotlin.math.roundToLong

@Composable
fun RecordSettlementDialog(
    members: List<MemberEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (payerId: Long, receiverId: Long, amountCents: Long) -> Unit
) {
    if (members.size < 2) return

    var payerId by remember { mutableStateOf(members.first().id) }
    var receiverId by remember { mutableStateOf(members.last().id) }
    var amountInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Settlement Payment 💸") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Who Paid?", fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members) { member ->
                        FilterChip(
                            selected = (payerId == member.id),
                            onClick = { payerId = member.id },
                            label = { Text(member.name) }
                        )
                    }
                }

                Text("Who Received?", fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members.filter { it.id != payerId }) { member ->
                        FilterChip(
                            selected = (receiverId == member.id),
                            onClick = { receiverId = member.id },
                            label = { Text(member.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount ($currency)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleaned = amountInput.replace(",", ".")
                    val amountVal = (cleaned.toDoubleOrNull() ?: 0.0) * 100
                    if (amountVal > 0) {
                        onConfirm(payerId, receiverId, amountVal.roundToLong())
                    }
                },
                enabled = amountInput.isNotBlank() && payerId != receiverId
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

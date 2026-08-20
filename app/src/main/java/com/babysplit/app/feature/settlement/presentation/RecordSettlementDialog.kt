package com.babysplit.app.feature.settlement.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.ui.theme.ChickAmber
import com.babysplit.app.core.ui.theme.SurfaceLight
import com.babysplit.app.core.ui.theme.TextPrimary
import com.babysplit.app.core.ui.theme.TextSecondary
import kotlin.math.roundToLong

@Composable
fun RecordSettlementDialog(
    members: List<MemberData>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (payerId: String, receiverId: String, amountCents: Long) -> Unit
) {
    if (members.size < 2) return

    var payerId by remember { mutableStateOf(members.first().id) }
    var receiverId by remember { mutableStateOf(members.last().id) }
    var amountInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Record Settlement Payment 💸", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Who Paid?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members) { member ->
                        FilterChip(
                            selected = (payerId == member.id),
                            onClick = { payerId = member.id },
                            label = { Text(member.name, fontWeight = if (payerId == member.id) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Text("Who Received?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members.filter { it.id != payerId }) { member ->
                        FilterChip(
                            selected = (receiverId == member.id),
                            onClick = { receiverId = member.id },
                            label = { Text(member.name, fontWeight = if (receiverId == member.id) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".")
                        val parts = filtered.split(".")
                        val normalized = if (parts.size > 2) parts[0] + "." + parts.drop(1).joinToString("") else filtered
                        amountInput = if (normalized.length > 1 && normalized.startsWith("0") && normalized[1] != '.') {
                            normalized.trimStart('0').ifEmpty { "0" }
                        } else {
                            normalized
                        }
                    },
                    label = { Text("Amount ($currency)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && (amountInput == "0" || amountInput == "0.00" || amountInput == "0.0")) {
                                amountInput = ""
                            }
                        }
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
                enabled = amountInput.isNotBlank() && payerId != receiverId,
                colors = ButtonDefaults.buttonColors(containerColor = ChickAmber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Record Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

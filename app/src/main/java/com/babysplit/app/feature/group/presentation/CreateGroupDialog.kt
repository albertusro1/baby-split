package com.babysplit.app.feature.group.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.ui.theme.*

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, currency: String, simplifyDebts: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🏖️") }
    var currency by remember { mutableStateOf("IDR") }
    var simplifyDebts by remember { mutableStateOf(true) }

    val emojis = listOf("🏖️", "🍕", "🏠", "🎉", "🚗", "✈️", "🛍️", "💡", "🍻", "☕")
    val currencies = listOf("IDR", "USD", "EUR", "SGD", "GBP", "JPY", "AUD")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏖️", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Trip / Group", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip / Group Name") },
                    placeholder = { Text("e.g. Bali Vacation 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pick an Icon:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.take(5).forEach { e ->
                        FilterChip(
                            selected = (emoji == e),
                            onClick = { emoji = e },
                            label = { Text(e, fontSize = 18.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.drop(5).forEach { e ->
                        FilterChip(
                            selected = (emoji == e),
                            onClick = { emoji = e },
                            label = { Text(e, fontSize = 18.sp) }
                        )
                    }
                }

                Text("Default Currency:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    currencies.take(4).forEach { c ->
                        FilterChip(
                            selected = (currency == c),
                            onClick = { currency = c },
                            label = { Text(c, fontSize = 12.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    currencies.drop(4).forEach { c ->
                        FilterChip(
                            selected = (currency == c),
                            onClick = { currency = c },
                            label = { Text(c, fontSize = 12.sp) }
                        )
                    }
                }

                // 🤝 Simplify Debts with Detailed Clear Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (simplifyDebts) ChickYellowLight else SurfaceElevatedLight),
                    border = BorderStroke(1.dp, if (simplifyDebts) ChickGold else SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🤝 Simplify Group Debts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    text = if (simplifyDebts)
                                        "Minimizes transactions (e.g. if A owes B $10 and B owes C $10, A pays C $10 directly)."
                                    else
                                        "Disabled: Everyone pays back each expense creator separately.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = simplifyDebts,
                                onCheckedChange = { simplifyDebts = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ChickAmber, checkedTrackColor = ChickYellowSubtle)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), emoji, currency, simplifyDebts)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Create Trip", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

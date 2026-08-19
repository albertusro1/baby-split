package com.babysplit.app.feature.group.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.database.entity.GroupEntity

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, currency: String, simplifyDebts: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🏖️") }
    var currency by remember { mutableStateOf("USD") }
    var simplifyDebts by remember { mutableStateOf(true) }

    val emojis = listOf("🏖️", "🍕", "🏠", "🎉", "🚗", "✈️", "🛍️", "💡")
    val currencies = listOf("USD", "IDR", "EUR", "GBP", "SGD", "JPY", "AUD")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Trip / Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip / Group Name") },
                    placeholder = { Text("e.g. Bali Vacation 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pick an Icon:", fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.take(4).forEach { e ->
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
                    emojis.drop(4).forEach { e ->
                        FilterChip(
                            selected = (emoji == e),
                            onClick = { emoji = e },
                            label = { Text(e, fontSize = 18.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Currency: $currency", fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        currencies.take(3).forEach { c ->
                            FilterChip(
                                selected = (currency == c),
                                onClick = { currency = c },
                                label = { Text(c) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Simplify Debts", fontSize = 14.sp)
                    Switch(
                        checked = simplifyDebts,
                        onCheckedChange = { simplifyDebts = it }
                    )
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
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.babysplit.app.feature.members.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.feature.members.domain.model.MemberType

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, memberType: MemberType, email: String?, phone: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(MemberType.OFFLINE_TAGGED) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Group Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Member Option:", fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = (selectedType == MemberType.OFFLINE_TAGGED),
                        onClick = { selectedType = MemberType.OFFLINE_TAGGED },
                        label = { Text("👤 Offline Tag") }
                    )
                    FilterChip(
                        selected = (selectedType == MemberType.GMAIL_INVITED),
                        onClick = { selectedType = MemberType.GMAIL_INVITED },
                        label = { Text("✉️ Gmail Invite") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Member Name") },
                    placeholder = { Text("e.g. Alice") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == MemberType.GMAIL_INVITED) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Gmail Address") },
                        placeholder = { Text("alice@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "ℹ️ An itemized receipt will be automatically emailed to this address when 'Finish Trip' is clicked.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("WhatsApp Phone (Optional)") },
                    placeholder = { Text("+62 812-3456-7890") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Enables 1-tap direct WhatsApp breakdown sharing.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            selectedType,
                            if (selectedType == MemberType.GMAIL_INVITED && email.isNotBlank()) email.trim() else null,
                            if (phone.isNotBlank()) phone.trim() else null
                        )
                    }
                },
                enabled = name.isNotBlank() && (selectedType != MemberType.GMAIL_INVITED || email.isNotBlank())
            ) {
                Text("Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

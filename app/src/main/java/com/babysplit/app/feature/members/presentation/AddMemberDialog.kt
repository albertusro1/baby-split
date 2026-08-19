package com.babysplit.app.feature.members.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.ui.theme.*
import com.babysplit.app.feature.members.domain.model.MemberType

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, memberType: MemberType, email: String?, phone: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Add Group Member 👤", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Member Name") },
                    placeholder = { Text("e.g. Alice, Bob, Jessica") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("WhatsApp Phone (Optional)") },
                    placeholder = { Text("e.g. +62 812-3456-7890") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "💬 Optional phone number enables 1-tap direct WhatsApp bill sharing.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            MemberType.OFFLINE_TAGGED,
                            null,
                            if (phone.isNotBlank()) phone.trim() else null
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Member", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}


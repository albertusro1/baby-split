package com.babysplit.app.feature.members.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    onConfirm: (
        name: String,
        memberType: MemberType,
        email: String?,
        phone: String?,
        bankName: String?,
        holderName: String?,
        bankAcc: String?,
        walletName: String?,
        walletHandle: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showPaymentDetails by remember { mutableStateOf(false) }
    var bankName by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var bankAcc by remember { mutableStateOf("") }
    var walletName by remember { mutableStateOf("") }
    var walletHandle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Add Group Member 👤", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Expandable Card for Creditor Bank / E-Wallet Info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ChickYellowSubtle,
                    border = BorderStroke(1.dp, ChickGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = ChickAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bank / E-Wallet Info (Optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A4F00))
                            }
                            IconButton(onClick = { showPaymentDetails = !showPaymentDetails }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    if (showPaymentDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = ChickAmber
                                )
                            }
                        }

                        AnimatedVisibility(visible = showPaymentDetails) {
                            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("If this member pays for group expenses, their bank info will be attached to debt repayment bills:", fontSize = 11.sp, color = TextSecondary)
                                OutlinedTextField(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    label = { Text("Bank Name (e.g. BCA, Mandiri)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = holderName,
                                    onValueChange = { holderName = it },
                                    label = { Text("Account Holder Name") },
                                    placeholder = { Text("Nama Pemilik Rekening") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = bankAcc,
                                    onValueChange = { bankAcc = it },
                                    label = { Text("Bank Account Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = walletHandle,
                                    onValueChange = { walletHandle = it },
                                    label = { Text("E-Wallet / QRIS (e.g. GoPay: 0812...)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
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
                            if (phone.isNotBlank()) phone.trim() else null,
                            if (bankName.isNotBlank()) bankName.trim() else null,
                            if (holderName.isNotBlank()) holderName.trim() else null,
                            if (bankAcc.isNotBlank()) bankAcc.trim() else null,
                            if (walletName.isNotBlank()) walletName.trim() else null,
                            if (walletHandle.isNotBlank()) walletHandle.trim() else null
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


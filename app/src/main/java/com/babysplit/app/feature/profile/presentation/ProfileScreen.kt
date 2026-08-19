package com.babysplit.app.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.whatsapp.HostPaymentDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentPaymentDetails: HostPaymentDetails?,
    currentCurrency: String,
    userEmail: String?,
    onBackClick: () -> Unit,
    onSavePaymentDetails: (bank: String?, account: String?, wallet: String?, handle: String?, note: String?, currency: String) -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    var bankName by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.bankName ?: "") }
    var bankAccount by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.bankAccountNumber ?: "") }
    var walletName by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.eWalletName ?: "") }
    var walletHandle by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.eWalletHandle ?: "") }
    var customNote by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.customNote ?: "") }
    var currency by remember(currentCurrency) { mutableStateOf(currentCurrency) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Payment Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                // Google Account & Cloud Backup Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Google Drive & Cloud Sync ☁️", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (userEmail != null) {
                            Text("Signed in as: $userEmail", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Trips & receipts automatically sync to your personal Google Drive folder.", fontSize = 11.sp, color = Color.DarkGray)
                        } else {
                            Text("Sign in with Google to enable automatic Google Drive backup and automated receipt emails.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = onGoogleSignInClick) {
                                Text("Sign In with Google")
                            }
                        }
                    }
                }
            }

            item {
                Text("Your Payment Details (Embedded in WhatsApp/Gmail)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (e.g. BCA, Chase, HSBC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = bankAccount,
                    onValueChange = { bankAccount = it },
                    label = { Text("Bank Account Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = walletName,
                    onValueChange = { walletName = it },
                    label = { Text("E-Wallet / App (e.g. PayPal, Venmo, GoPay)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = walletHandle,
                    onValueChange = { walletHandle = it },
                    label = { Text("E-Wallet Handle / Link (e.g. paypal.me/name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("Custom Transfer Note") },
                    placeholder = { Text("e.g. Please add your name in transfer description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        onSavePaymentDetails(
                            bankName.ifBlank { null },
                            bankAccount.ifBlank { null },
                            walletName.ifBlank { null },
                            walletHandle.ifBlank { null },
                            customNote.ifBlank { null },
                            currency
                        )
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


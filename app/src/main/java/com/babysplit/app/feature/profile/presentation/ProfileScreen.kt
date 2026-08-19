package com.babysplit.app.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import com.babysplit.app.core.whatsapp.HostPaymentDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentPaymentDetails: HostPaymentDetails?,
    currentCurrency: String,
    userEmail: String?,
    userName: String = "Guest",
    onBackClick: () -> Unit,
    onSavePaymentDetails: (bank: String?, holder: String?, account: String?, wallet: String?, handle: String?, note: String?, currency: String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    var bankName by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.bankName ?: "") }
    var accountHolderName by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.accountHolderName ?: "") }
    var bankAccount by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.bankAccountNumber ?: "") }
    var walletName by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.eWalletName ?: "") }
    var walletHandle by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.eWalletHandle ?: "") }
    var customNote by remember(currentPaymentDetails) { mutableStateOf(currentPaymentDetails?.customNote ?: "") }
    var currency by remember(currentCurrency) { mutableStateOf(currentCurrency) }

    val popularCurrencies = listOf("IDR", "USD", "EUR", "SGD", "GBP", "JPY", "AUD")
    val popularWallets = listOf("GoPay", "OVO", "Dana", "ShopeePay", "PayPal")

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Profile & Payment Info 🐥", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Google Account & Cloud Backup Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (userEmail != null) SettledGreenLight else ChickYellowLight),
                    border = BorderStroke(1.dp, if (userEmail != null) Color(0xFFC8E6C9) else ChickGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (userEmail != null) "☁️ Google Account Linked" else "👤 Guest Mode (Local Storage)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (userEmail != null) SettledGreen else TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (userEmail != null) {
                            Text("Signed in as: $userEmail", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Your trip records, summaries & receipts are automatically archived to Google Drive.", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onSignOutClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DebtRed),
                                border = BorderStroke(1.dp, DebtRed)
                            ) {
                                Text("Sign Out / Switch to Guest")
                            }
                        } else {
                            Text(
                                text = "You are currently using Baby Split offline as Guest. Link your Google account so your trip history and receipts are safely backed up to Google Drive and receipts are emailed automatically.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onGoogleSignInClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🔗 Sign In with Google", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. 🏦 Bank Transfer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏦 Bank Transfer Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("This info will be embedded in WhatsApp & Gmail bills for friends to transfer to.", fontSize = 12.sp, color = TextSecondary)

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            placeholder = { Text("e.g. BCA, Mandiri, BRI, BNI, Chase") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = accountHolderName,
                            onValueChange = { accountHolderName = it },
                            label = { Text("Account Holder Name (Nama Pemilik Rekening)") },
                            placeholder = { Text("e.g. Rowan Alexander") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bankAccount,
                            onValueChange = { bankAccount = it },
                            label = { Text("Bank Account Number (Nomor Rekening)") },
                            placeholder = { Text("e.g. 5410123456") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. 📱 E-Wallet & QRIS Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📱 E-Wallet & QRIS Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            popularWallets.take(3).forEach { w ->
                                FilterChip(
                                    selected = (walletName == w),
                                    onClick = { walletName = w },
                                    label = { Text(w, fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = walletName,
                            onValueChange = { walletName = it },
                            label = { Text("E-Wallet Provider") },
                            placeholder = { Text("e.g. GoPay, OVO, Dana, PayPal") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = walletHandle,
                            onValueChange = { walletHandle = it },
                            label = { Text("E-Wallet Phone / Handle / ID") },
                            placeholder = { Text("e.g. 08123456789 or paypal.me/name") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. 📝 Transfer Note & Currency Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📝 Payment Note & Default Currency", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("Custom Transfer Note") },
                            placeholder = { Text("e.g. Please send transfer receipt screenshot via WA") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Default Currency: $currency", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            popularCurrencies.take(4).forEach { c ->
                                FilterChip(
                                    selected = (currency == c),
                                    onClick = { currency = c },
                                    label = { Text(c) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Save Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSavePaymentDetails(
                            bankName.ifBlank { null },
                            accountHolderName.ifBlank { null },
                            bankAccount.ifBlank { null },
                            walletName.ifBlank { null },
                            walletHandle.ifBlank { null },
                            customNote.ifBlank { null },
                            currency
                        )
                        onBackClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White)
                ) {
                    Text("💾 Save Payment Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


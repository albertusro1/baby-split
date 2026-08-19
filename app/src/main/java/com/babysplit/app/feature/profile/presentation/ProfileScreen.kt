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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentPaymentDetails: HostPaymentDetails?,
    currentCurrency: String,
    onBackClick: () -> Unit,
    onSavePaymentDetails: (bank: String?, holder: String?, account: String?, wallet: String?, handle: String?, note: String?, currency: String) -> Unit,
    onRestoreBackups: (List<com.babysplit.app.core.gdrive.DriveBackupItem>) -> Unit = {}
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

    var discoveredBackups by remember { mutableStateOf<List<com.babysplit.app.core.gdrive.DriveBackupItem>>(emptyList()) }
    var showRestorePromptDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val documentPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                    if (!content.isNullOrBlank()) {
                        val json = org.json.JSONObject(content)
                        val name = json.optString("name", "Imported Trip")
                        val emoji = json.optString("emoji", "✈️")
                        val membersCount = json.optJSONArray("members")?.length() ?: 1
                        val expensesCount = json.optJSONArray("expenses")?.length() ?: 0
                        val timestamp = json.optLong("updatedAtEpochMs", json.optLong("createdAtEpochMs", System.currentTimeMillis()))

                        val backupItem = com.babysplit.app.core.gdrive.DriveBackupItem(
                            id = uri.toString(),
                            tripName = name,
                            emoji = emoji,
                            timestampMs = timestamp,
                            membersCount = membersCount,
                            expensesCount = expensesCount,
                            rawJson = content
                        )
                        discoveredBackups = listOf(backupItem)
                        showRestorePromptDialog = true
                    } else {
                        android.widget.Toast.makeText(context, "Selected backup file is empty.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Failed to read backup file: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
            // 1. 💾 Offline Data Backup & Restore Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💾 Backup & Restore Archives", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        }
                        Text(
                            "Baby Split keeps your trip records 100% private and stored locally on your device. You can import or export trip archives (.json) at any time.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        OutlinedButton(
                            onClick = { documentPickerLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ChickGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChickAmber)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📂 Browse & Import Backup File (.json)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        Text("This info will be embedded in WhatsApp bills for friends to transfer to.", fontSize = 12.sp, color = TextSecondary)

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

            // 3. 📱 E-Wallet Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📱 E-Wallet / QRIS Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Optional: For members who prefer paying via e-wallets.", fontSize = 12.sp, color = TextSecondary)

                        Text("Quick Select E-Wallet:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            popularWallets.take(3).forEach { w ->
                                FilterChip(
                                    selected = (walletName.equals(w, ignoreCase = true)),
                                    onClick = { walletName = w },
                                    label = { Text(w, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            popularWallets.drop(3).forEach { w ->
                                FilterChip(
                                    selected = (walletName.equals(w, ignoreCase = true)),
                                    onClick = { walletName = w },
                                    label = { Text(w, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = walletName,
                            onValueChange = { walletName = it },
                            label = { Text("E-Wallet Provider") },
                            placeholder = { Text("e.g. GoPay, OVO, Dana, ShopeePay") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = walletHandle,
                            onValueChange = { walletHandle = it },
                            label = { Text("E-Wallet Phone Number / ID") },
                            placeholder = { Text("e.g. 081234567890") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. 📝 Custom Payment Note
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📝 Custom Payment Instructions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Extra message appended to all split receipts (e.g. 'Please send proof of transfer').", fontSize = 12.sp, color = TextSecondary)

                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("Custom Note") },
                            placeholder = { Text("e.g. Tolong transfer sebelum besok siang ya guys!") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 5. 🌐 Default Currency Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🌐 Default Trip Currency", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            popularCurrencies.take(4).forEach { c ->
                                FilterChip(
                                    selected = (currency == c),
                                    onClick = { currency = c },
                                    label = { Text(c, fontSize = 12.sp) }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            popularCurrencies.drop(4).forEach { c ->
                                FilterChip(
                                    selected = (currency == c),
                                    onClick = { currency = c },
                                    label = { Text(c, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Save Button
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

    // Found Existing Backups Dialog
    if (showRestorePromptDialog) {
        AlertDialog(
            onDismissRequest = { /* Keep dialog visible until explicit user action */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("📥 Import Backup File", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Found valid Baby Split trip archive:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    discoveredBackups.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ChickYellowSubtle,
                            border = BorderStroke(1.dp, ChickGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${item.emoji} ${item.tripName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text("${item.membersCount} Members • ${item.expensesCount} Expenses", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = SettledGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Text("Would you like to restore this trip into your app?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackups(discoveredBackups)
                        showRestorePromptDialog = false
                        android.widget.Toast.makeText(context, "Restored ${discoveredBackups.size} trip(s) successfully! 🎉", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SettledGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Restore Trip 📥", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestorePromptDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}


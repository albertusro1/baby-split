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
    onSaveUserProfile: (name: String, email: String) -> Unit,
    onRestoreBackups: (List<com.babysplit.app.core.gdrive.DriveBackupItem>) -> Unit = {},
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

    var showGoogleAuthDialog by remember { mutableStateOf(false) }
    var inputGoogleEmail by remember { mutableStateOf("") }
    var inputGoogleName by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                                text = "You are currently using Baby Split offline as Guest. Link your Google account so your trip history and receipts are safely backed up to your Google Drive folder.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showGoogleAuthDialog = true },
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

    var isScanningDrive by remember { mutableStateOf(false) }
    var discoveredBackups by remember { mutableStateOf<List<com.babysplit.app.core.gdrive.DriveBackupItem>>(emptyList()) }
    var showRestorePromptDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showGoogleAuthDialog) {
        AlertDialog(
            onDismissRequest = { if (!isScanningDrive) showGoogleAuthDialog = false },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("☁️ Google Account Sign-In", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Sign in to automatically sync your trip expenses and search for previous Google Drive backups.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://accounts.google.com/")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = ChickAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Google in Browser 🌐", fontSize = 13.sp, color = TextPrimary)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = SurfaceBorderLight)

                    Text("Enter your Google Account email to link:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

                    OutlinedTextField(
                        value = inputGoogleEmail,
                        onValueChange = { inputGoogleEmail = it },
                        label = { Text("Google Email") },
                        placeholder = { Text("yourname@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputGoogleName,
                        onValueChange = { inputGoogleName = it },
                        label = { Text("Display Name (Optional)") },
                        placeholder = { Text("e.g. Rowan") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isScanningDrive) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ChickAmber, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching Google Drive for backups...", fontSize = 12.sp, color = ChickAmber, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputGoogleEmail.isNotBlank() && !isScanningDrive) {
                            isScanningDrive = true
                            val name = inputGoogleName.ifBlank { inputGoogleEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
                            val email = inputGoogleEmail.trim()

                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                val backups = com.babysplit.app.core.gdrive.GoogleDriveBackupEngine.searchForDriveBackups(context, email)
                                isScanningDrive = false
                                showGoogleAuthDialog = false
                                onSaveUserProfile(name, email)

                                if (backups.isNotEmpty()) {
                                    discoveredBackups = backups
                                    showRestorePromptDialog = true
                                }
                            }
                        }
                    },
                    enabled = inputGoogleEmail.isNotBlank() && inputGoogleEmail.contains("@") && !isScanningDrive,
                    colors = ButtonDefaults.buttonColors(containerColor = ChickAmber, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isScanningDrive) "Searching Drive..." else "Link & Search Backups 🔍", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isScanningDrive) showGoogleAuthDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Found Existing Google Drive Backups Dialog
    if (showRestorePromptDialog) {
        AlertDialog(
            onDismissRequest = { showRestorePromptDialog = false },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("☁️ Backups Found on Google Drive!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "We discovered previous Baby Split trip backup(s) linked to your Google Account:",
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
                                    Text("✈️ ${item.tripName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text("Trip Archive Backup", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = SettledGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Text("Would you like to import and restore these trips into your app?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackups(discoveredBackups)
                        showRestorePromptDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SettledGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Restore Trips 📥", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestorePromptDialog = false }) {
                    Text("Skip / Start Fresh", color = TextSecondary)
                }
            }
        )
    }
}


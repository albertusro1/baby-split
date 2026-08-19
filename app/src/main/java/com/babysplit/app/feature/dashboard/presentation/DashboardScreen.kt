package com.babysplit.app.feature.dashboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.database.entity.GroupEntity
import com.babysplit.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    groups: List<GroupEntity>,
    userEmail: String? = null,
    userName: String = "Guest",
    onGroupClick: (Long) -> Unit,
    onCreateGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDeleteGroup: (Long) -> Unit = {},
    onRestoreDiscoveredBackups: (List<com.babysplit.app.core.gdrive.DriveBackupItem>) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }
    var discoveredBackups by remember { mutableStateOf<List<com.babysplit.app.core.gdrive.DriveBackupItem>>(emptyList()) }

    LaunchedEffect(groups.isEmpty()) {
        if (groups.isEmpty()) {
            val backups = com.babysplit.app.core.gdrive.GoogleDriveBackupEngine.searchForDriveBackups(context, userEmail ?: "")
            discoveredBackups = backups
        } else {
            discoveredBackups = emptyList()
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐥", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Baby Split", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                            Text("Fair & Easy Group Expenses", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Profile & Settings",
                            tint = ChickAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGroupClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White) },
                text = { Text("New Trip / Group", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White) },
                containerColor = ChickAmber,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
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
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // ☁️ Guest / Google Sync Status Bar Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (userEmail != null) SettledGreenLight else ChickYellowLight
                    ),
                    border = BorderStroke(1.dp, if (userEmail != null) Color(0xFFC8E6C9) else ChickGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(if (userEmail != null) "☁️" else "👤", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (userEmail != null) "Google Drive Linked" else "Guest Mode (Offline)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (userEmail != null) SettledGreen else TextPrimary
                                )
                                Text(
                                    text = if (userEmail != null) userEmail else "Tap to connect Google Drive backup",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Profile",
                            tint = if (userEmail != null) SettledGreen else ChickAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Discovered Cloud Backups Card (if app was reinstalled or empty)
            if (discoveredBackups.isNotEmpty() && groups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ChickYellowSubtle),
                        border = BorderStroke(1.dp, ChickAmber)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("☁️", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${discoveredBackups.size} Cloud Backup Trip(s) Found!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Previous trips found on device & Drive. Tap below to restore everything.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onRestoreDiscoveredBackups(discoveredBackups) },
                                colors = ButtonDefaults.buttonColors(containerColor = SettledGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore All Trips Now 📥", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Trips & Groups (${groups.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            if (groups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = BorderStroke(1.dp, SurfaceBorderLight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏖️", fontSize = 52.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No active trips or groups yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tap '+ New Trip / Group' to start splitting expenses with friends!",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(groups) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGroupClick(group.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = BorderStroke(1.dp, SurfaceBorderLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ChickYellowLight,
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(group.emoji, fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = group.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (group.isFinished) SettledGreenLight else ChickYellowSubtle,
                                            border = BorderStroke(1.dp, if (group.isFinished) SettledGreen else ChickGold)
                                        ) {
                                            Text(
                                                text = if (group.isFinished) "Settled ✅" else "Active • ${group.currency}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (group.isFinished) SettledGreen else Color(0xFF8A5B00),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { groupToDelete = group },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete Trip",
                                        tint = DebtRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            containerColor = SurfaceLight,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Delete Trip? 🗑️", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${groupToDelete?.emoji} ${groupToDelete?.name}'? All expenses, member records, and receipts in this trip will be permanently removed.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gId = groupToDelete?.id
                        if (gId != null) {
                            onDeleteGroup(gId)
                        }
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Trip", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

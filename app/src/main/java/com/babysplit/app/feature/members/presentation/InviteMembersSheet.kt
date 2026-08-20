package com.babysplit.app.feature.members.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babysplit.app.core.ui.theme.*
import com.babysplit.app.core.whatsapp.WhatsAppShareHelper

private const val APP_DOWNLOAD_LINK = "https://drive.google.com/file/d/1bIcGEMizeV_bNiQs3iLcItDcTk89IRHT/view?usp=drive_link"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMembersSheet(
    inviteCode: String,
    tripName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    fun buildShareMessage(): String {
        return buildString {
            appendLine("👶 *Join my trip \"$tripName\" on Baby Split!*")
            appendLine()
            appendLine("🔑 *Trip Invite Code:* $inviteCode")
            appendLine()
            appendLine("📲 *Download Baby Split App:*")
            appendLine(APP_DOWNLOAD_LINK)
            appendLine()
            appendLine("👉 *How to join:*")
            appendLine("1. Download & open Baby Split")
            appendLine("2. Sign in with Google")
            appendLine("3. Tap \"*Join Trip by Code*\" on the dashboard")
            appendLine("4. Enter code: *$inviteCode*")
        }.trim()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "👥 Invite Friends to Trip",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share this code so your friends can join \"$tripName\" and view & split expenses in real-time.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Large invite code display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ChickYellowLight),
                border = BorderStroke(1.dp, ChickGold)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "INVITE CODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7A4F00),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = inviteCode,
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp,
                        color = ChickAmber
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", inviteCode))
                        showCopiedSnackbar = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Code", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Button(
                    onClick = {
                        val msg = buildShareMessage()
                        WhatsAppShareHelper.shareToWhatsApp(context, msg)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Share WA 📲", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // General Share Button (for other apps / Telegram / SMS)
            OutlinedButton(
                onClick = {
                    val shareText = buildShareMessage()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Trip Invite"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceBorderLight)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share via Other Apps...", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }

            if (showCopiedSnackbar) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SettledGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SettledGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "✅ Invite code copied to clipboard!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettledGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showCopiedSnackbar = false
                }
            }
        }
    }
}


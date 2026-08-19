package com.babysplit.app.core.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppShareHelper {

    /**
     * Shares text directly to a specific phone number via WhatsApp (wa.me URI),
     * or opens the system share sheet if no phone number is provided or WhatsApp is not installed.
     */
    fun shareToWhatsApp(
        context: Context,
        message: String,
        phoneNumber: String? = null
    ) {
        val cleanPhone = phoneNumber?.replace(Regex("[^0-9+]"), "")

        if (!cleanPhone.isNullOrBlank()) {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val directUri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, directUri)
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback to universal share sheet
            }
        }

        // Universal Share Sheet
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // If WhatsApp package fails, open generic chooser
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(genericIntent, "Share Bill Summary"))
        }
    }
}

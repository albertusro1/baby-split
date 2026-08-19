package com.babysplit.app.core.whatsapp

import com.babysplit.app.feature.balance.domain.engine.DebtSimplificationEngine
import com.babysplit.app.feature.expense.domain.model.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HostPaymentDetails(
    val hostName: String,
    val bankName: String? = null,
    val accountHolderName: String? = null,
    val bankAccountNumber: String? = null,
    val eWalletName: String? = null,
    val eWalletHandle: String? = null,
    val customNote: String? = null
)

object BillSummaryFormatter {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun formatCents(cents: Long, currency: String = "USD"): String {
        val absCents = kotlin.math.abs(cents)
        val dollars = absCents / 100
        val remainder = absCents % 100
        val sign = if (cents < 0) "-" else ""
        val symbol = when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "IDR" -> "Rp "
            "JPY" -> "¥"
            "SGD" -> "S$"
            else -> "$currency "
        }
        return if (currency.uppercase() == "IDR" || currency.uppercase() == "JPY") {
            "$sign$symbol$dollars"
        } else {
            "$sign$symbol$dollars.${remainder.toString().padStart(2, '0')}"
        }
    }

    private fun isHostName(name: String?, hostMemberName: String?, paymentDetails: HostPaymentDetails?): Boolean {
        if (name.isNullOrBlank()) return false
        if (name.contains("Host", ignoreCase = true) || name.contains("You", ignoreCase = true)) return true
        if (!hostMemberName.isNullOrBlank() && name.equals(hostMemberName, ignoreCase = true)) return true
        if (paymentDetails != null && name.equals(paymentDetails.hostName, ignoreCase = true)) return true
        return false
    }

    /**
     * Formats an itemized breakdown for a specific member in WhatsApp Markdown.
     * Only displays host bank details if the creditor is actually the host.
     */
    fun formatMemberWhatsAppMessage(
        tripName: String,
        memberName: String,
        memberExpenses: List<Pair<Expense, Long>>, // Pair of (Expense, member's share cents)
        totalOwedCents: Long,
        currency: String,
        paymentDetails: HostPaymentDetails?,
        debtorTransactions: List<DebtSimplificationEngine.SimplifiedTransaction> = emptyList(),
        hostMemberName: String? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("🧾 *Baby Split - $tripName*")
        sb.appendLine("📅 Date: ${dateFormatter.format(Date())}")
        sb.appendLine()
        sb.appendLine("Hi *$memberName*! Here is your itemized expense breakdown:")
        sb.appendLine()

        memberExpenses.forEachIndexed { index, (expense, shareCents) ->
            val emoji = expense.category.emoji
            sb.appendLine("${index + 1}. $emoji *${expense.title}*")
            sb.appendLine("   • Total: ${formatCents(expense.totalAmountCents, currency)}")
            sb.appendLine("   • Your Share: *${formatCents(shareCents, currency)}*")
            sb.appendLine()
        }

        sb.appendLine("----------------------------------------")
        if (totalOwedCents > 0) {
            sb.appendLine("💰 *TOTAL AMOUNT YOU OWE: ${formatCents(totalOwedCents, currency)}*")
        } else if (totalOwedCents < 0) {
            sb.appendLine("💰 *YOU ARE OWED: ${formatCents(-totalOwedCents, currency)}*")
        } else {
            sb.appendLine("💰 *STATUS: All Settled Up ✅*")
        }
        sb.appendLine("----------------------------------------")

        if (totalOwedCents > 0) {
            sb.appendLine()
            if (debtorTransactions.isNotEmpty()) {
                sb.appendLine("💳 *Settlement Instructions (Who to Transfer to):*")
                debtorTransactions.forEach { tx ->
                    val isCreditorHost = isHostName(tx.creditorName, hostMemberName, paymentDetails)
                    sb.appendLine()
                    sb.appendLine("• Pay *${tx.creditorName}*: *${formatCents(tx.amountCents, currency)}*")
                    if (isCreditorHost && paymentDetails != null) {
                        if (!paymentDetails.bankAccountNumber.isNullOrBlank()) {
                            val bank = paymentDetails.bankName ?: "Bank"
                            val holder = if (!paymentDetails.accountHolderName.isNullOrBlank()) paymentDetails.accountHolderName else paymentDetails.hostName
                            sb.appendLine("  - *$bank*: ${paymentDetails.bankAccountNumber} (a.n. $holder)")
                        }
                        if (!paymentDetails.eWalletHandle.isNullOrBlank()) {
                            val wallet = paymentDetails.eWalletName ?: "E-Wallet"
                            sb.appendLine("  - *$wallet*: ${paymentDetails.eWalletHandle}")
                        }
                        if (!paymentDetails.customNote.isNullOrBlank()) {
                            sb.appendLine("  - Note: ${paymentDetails.customNote}")
                        }
                    } else {
                        sb.appendLine("  - ℹ️ Please contact *${tx.creditorName}* directly for their Bank / E-Wallet transfer details.")
                    }
                }
            } else if (paymentDetails != null) {
                // If debtor owes the host directly
                sb.appendLine("💳 *Please transfer to:*")
                if (!paymentDetails.bankAccountNumber.isNullOrBlank()) {
                    val bank = paymentDetails.bankName ?: "Bank"
                    val holder = if (!paymentDetails.accountHolderName.isNullOrBlank()) paymentDetails.accountHolderName else paymentDetails.hostName
                    sb.appendLine("• *$bank*: ${paymentDetails.bankAccountNumber} (a.n. $holder)")
                }
                if (!paymentDetails.eWalletHandle.isNullOrBlank()) {
                    val wallet = paymentDetails.eWalletName ?: "E-Wallet"
                    sb.appendLine("• *$wallet*: ${paymentDetails.eWalletHandle}")
                }
                if (!paymentDetails.customNote.isNullOrBlank()) {
                    sb.appendLine("• Note: ${paymentDetails.customNote}")
                }
            }
        }

        sb.appendLine()
        sb.appendLine("Thank you! Generated with Baby Split 👶")
        return sb.toString().trim()
    }

    /**
     * Formats an itemized breakdown for a specific member in clean HTML for automated Gmail dispatch.
     */
    fun formatMemberHtmlReceipt(
        tripName: String,
        memberName: String,
        memberExpenses: List<Pair<Expense, Long>>,
        totalOwedCents: Long,
        currency: String,
        paymentDetails: HostPaymentDetails?
    ): String {
        val totalFormatted = formatCents(totalOwedCents, currency)
        val dateFormatted = dateFormatter.format(Date())

        val itemsHtml = memberExpenses.mapIndexed { index, (expense, shareCents) ->
            """
            <tr style="border-bottom: 1px solid #E0E0E0;">
                <td style="padding: 10px 0;">
                    <strong>${index + 1}. ${expense.category.emoji} ${expense.title}</strong><br/>
                    <span style="font-size: 12px; color: #757575;">Total: ${formatCents(expense.totalAmountCents, currency)}</span>
                </td>
                <td style="padding: 10px 0; text-align: right; font-weight: bold; color: #1E88E5;">
                    ${formatCents(shareCents, currency)}
                </td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        val bankInfo = if (paymentDetails != null && !paymentDetails.bankAccountNumber.isNullOrBlank()) {
            val holder = if (!paymentDetails.accountHolderName.isNullOrBlank()) paymentDetails.accountHolderName else paymentDetails.hostName
            "<p style='margin: 4px 0;'><strong>${paymentDetails.bankName ?: "Bank"}:</strong> ${paymentDetails.bankAccountNumber} (a.n. $holder)</p>"
        } else ""

        val walletInfo = if (paymentDetails != null && !paymentDetails.eWalletHandle.isNullOrBlank()) {
            "<p style='margin: 4px 0;'><strong>${paymentDetails.eWalletName ?: "E-Wallet"}:</strong> ${paymentDetails.eWalletHandle}</p>"
        } else ""

        val noteInfo = if (paymentDetails != null && !paymentDetails.customNote.isNullOrBlank()) {
            "<p style='margin: 4px 0; color: #666;'>${paymentDetails.customNote}</p>"
        } else ""

        val paymentHtml = if (totalOwedCents > 0 && paymentDetails != null) {
            """
            <div style="background-color: #F5F5F5; padding: 15px; border-radius: 8px; margin-top: 20px;">
                <h4 style="margin-top: 0; margin-bottom: 8px; color: #333;">💳 Payment Information</h4>
                $bankInfo
                $walletInfo
                $noteInfo
            </div>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"/></head>
        <body style="font-family: Arial, sans-serif; line-height: 1.5; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background: linear-gradient(135deg, #FFB800, #FF9100); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;">
                <h2 style="margin: 0;">Baby Split 🐥</h2>
                <h3 style="margin: 5px 0 0 0; font-weight: normal;">$tripName</h3>
                <p style="margin: 5px 0 0 0; font-size: 13px; opacity: 0.9;">$dateFormatted</p>
            </div>
            <div style="border: 1px solid #E0E0E0; border-top: none; padding: 20px; border-radius: 0 0 8px 8px;">
                <p>Hi <strong>$memberName</strong>,</p>
                <p>Here is your finalized itemized expense summary for <strong>$tripName</strong>:</p>
                <table style="width: 100%; border-collapse: collapse; margin-top: 15px;">
                    <thead>
                        <tr style="border-bottom: 2px solid #333;">
                            <th style="text-align: left; padding: 8px 0;">Expense Item</th>
                            <th style="text-align: right; padding: 8px 0;">Your Share</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>
                <div style="text-align: right; margin-top: 20px; padding-top: 10px; border-top: 2px solid #333;">
                    <span style="font-size: 18px; font-weight: bold; color: ${if (totalOwedCents > 0) "#D32F2F" else "#388E3C"};">
                        Total Amount Due: $totalFormatted
                    </span>
                </div>
                $paymentHtml
                <p style="margin-top: 30px; font-size: 12px; color: #9E9E9E; text-align: center;">
                    Generated automatically with Baby Split App 🐥
                </p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Formats overall group summary table for WhatsApp group chats.
     */
    fun formatGroupWhatsAppSummary(
        tripName: String,
        totalSpendingCents: Long,
        currency: String,
        simplifiedTransactions: List<DebtSimplificationEngine.SimplifiedTransaction>,
        paymentDetails: HostPaymentDetails?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("📊 *Baby Split - $tripName (Group Summary)*")
        sb.appendLine("📅 Date: ${dateFormatter.format(Date())}")
        sb.appendLine("💵 Total Group Spending: *${formatCents(totalSpendingCents, currency)}*")
        sb.appendLine()
        sb.appendLine("🤝 *Settlement Breakdown (Simplified Debts):*")
        sb.appendLine("----------------------------------------")

        if (simplifiedTransactions.isEmpty()) {
            sb.appendLine("All debts are settled! 🎉")
        } else {
            simplifiedTransactions.forEach { tx ->
                sb.appendLine("• *${tx.debtorName}* pays *${tx.creditorName}*: ${formatCents(tx.amountCents, currency)}")
            }
        }

        sb.appendLine("----------------------------------------")
        if (paymentDetails != null && (!paymentDetails.bankAccountNumber.isNullOrBlank() || !paymentDetails.eWalletHandle.isNullOrBlank())) {
            sb.appendLine()
            sb.appendLine("💳 *Host Payment Info:*")
            if (!paymentDetails.bankAccountNumber.isNullOrBlank()) {
                val bank = paymentDetails.bankName ?: "Bank"
                val holder = if (!paymentDetails.accountHolderName.isNullOrBlank()) paymentDetails.accountHolderName else paymentDetails.hostName
                sb.appendLine("• $bank: ${paymentDetails.bankAccountNumber} (a.n. $holder)")
            }
            if (!paymentDetails.eWalletHandle.isNullOrBlank()) {
                val wallet = paymentDetails.eWalletName ?: "E-Wallet"
                sb.appendLine("• $wallet: ${paymentDetails.eWalletHandle}")
            }
        }

        sb.appendLine()
        sb.appendLine("Generated with Baby Split 🐥")
        return sb.toString().trim()
    }
}

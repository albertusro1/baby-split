package com.babysplit.app.feature.group.domain

import android.content.Context
import com.babysplit.app.core.database.BabySplitDatabase
import com.babysplit.app.core.gdrive.GoogleDriveBackupEngine
import com.babysplit.app.core.gmail.GmailReceiptDispatcher
import com.babysplit.app.core.whatsapp.HostPaymentDetails
import com.babysplit.app.feature.expense.domain.model.Expense
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.ExpenseParticipant
import com.babysplit.app.feature.expense.domain.model.SplitType
import com.babysplit.app.feature.members.domain.model.MemberType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trip Lifecycle Manager.
 * Orchestrates finishing a trip: marks finished in DB, sends automated Gmail receipts to invited members,
 * and backs up trip archive to Google Drive.
 */
class TripLifecycleManager(private val database: BabySplitDatabase) {

    suspend fun finishTrip(
        context: Context,
        groupId: Long,
        paymentDetails: HostPaymentDetails?
    ): Int = withContext(Dispatchers.IO) {
        val group = database.groupDao().getGroupByIdDirect(groupId) ?: return@withContext 0
        val members = database.memberDao().getMembersForGroupDirect(groupId)
        val expenseEntities = database.expenseDao().getExpensesWithParticipantsDirect(groupId)

        val expenses = expenseEntities.map { expWithParts ->
            val exp = expWithParts.expense
            Expense(
                id = exp.id,
                groupId = exp.groupId,
                title = exp.title,
                totalAmountCents = exp.totalAmountCents,
                currency = exp.currency,
                category = ExpenseCategory.fromName(exp.categoryName),
                paidByMemberId = exp.paidByMemberId,
                paidByMemberName = exp.paidByMemberName,
                splitType = SplitType.valueOf(exp.splitType),
                participants = expWithParts.participants.map {
                    ExpenseParticipant(
                        memberId = it.memberId,
                        memberName = it.memberName,
                        amountCents = it.amountCents,
                        rawShareValue = it.rawShareValue
                    )
                },
                receiptImagePath = exp.receiptImagePath,
                note = exp.note,
                createdAtEpochMs = exp.createdAtEpochMs,
                isSettlement = exp.isSettlement
            )
        }

        // 1. Mark group as finished in DB
        database.groupDao().updateGroup(group.copy(isFinished = true))

        // 2. Dispatch automated Gmail receipts to all Gmail-invited members
        var dispatchedEmailsCount = 0
        val gmailMembers = members.filter { it.memberType == MemberType.GMAIL_INVITED.name && !it.email.isNullOrBlank() }

        for (member in gmailMembers) {
            val memberExpenses = mutableListOf<Pair<Expense, Long>>()
            var totalOwed = 0L

            for (expense in expenses) {
                val part = expense.participants.firstOrNull { it.memberId == member.id }
                if (part != null) {
                    memberExpenses.add(expense to part.amountCents)
                    totalOwed += part.amountCents
                }
                if (expense.paidByMemberId == member.id) {
                    totalOwed -= expense.totalAmountCents
                }
            }

            val success = GmailReceiptDispatcher.sendReceiptEmail(
                context = context,
                recipientEmail = member.email!!,
                recipientName = member.name,
                tripName = group.name,
                memberExpenses = memberExpenses,
                totalOwedCents = totalOwed,
                currency = group.currency,
                paymentDetails = paymentDetails
            )
            if (success) dispatchedEmailsCount++
        }

        // 3. Backup to Google Drive
        val receiptImages = expenses.mapNotNull { it.receiptImagePath }
        GoogleDriveBackupEngine.backupTripToDrive(
            context = context,
            tripName = group.name,
            tripSummaryJson = "{\"tripId\": $groupId, \"name\": \"${group.name}\", \"expensesCount\": ${expenses.size}}",
            receiptPaths = receiptImages
        )

        dispatchedEmailsCount
    }
}

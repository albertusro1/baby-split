package com.babysplit.app.feature.group.domain

import android.content.Context
import com.babysplit.app.core.database.BabySplitDatabase
import com.babysplit.app.core.gdrive.GoogleDriveBackupEngine
import com.babysplit.app.core.whatsapp.HostPaymentDetails
import com.babysplit.app.feature.expense.domain.model.Expense
import com.babysplit.app.feature.expense.domain.model.ExpenseCategory
import com.babysplit.app.feature.expense.domain.model.ExpenseParticipant
import com.babysplit.app.feature.expense.domain.model.SplitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trip Lifecycle Manager.
 * Orchestrates finishing a trip: marks finished in DB and backs up trip archive to Google Drive.
 */
class TripLifecycleManager(private val database: BabySplitDatabase) {

    suspend fun finishTrip(
        context: Context,
        groupId: Long,
        paymentDetails: HostPaymentDetails?
    ): Boolean = withContext(Dispatchers.IO) {
        val group = database.groupDao().getGroupByIdDirect(groupId) ?: return@withContext false
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

        // 2. Backup full trip snapshot to persistent external and cloud locations
        GoogleDriveBackupEngine.autoExportTripSnapshot(context, database, groupId)

        true
    }
}

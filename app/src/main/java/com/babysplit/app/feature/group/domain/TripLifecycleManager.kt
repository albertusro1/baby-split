package com.babysplit.app.feature.group.domain

import android.content.Context
import com.babysplit.app.core.database.BabySplitDatabase
import com.babysplit.app.core.whatsapp.HostPaymentDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trip Lifecycle Manager.
 * Orchestrates finishing a trip: marks finished in DB.
 */
class TripLifecycleManager(private val database: BabySplitDatabase) {

    suspend fun finishTrip(
        context: Context,
        groupId: Long,
        paymentDetails: HostPaymentDetails? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val group = database.groupDao().getGroupByIdDirect(groupId) ?: return@withContext false
        database.groupDao().updateGroup(group.copy(isFinished = true))
        true
    }
}


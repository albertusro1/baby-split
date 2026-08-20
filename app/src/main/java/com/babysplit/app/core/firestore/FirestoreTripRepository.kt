package com.babysplit.app.core.firestore

import com.babysplit.app.core.repository.ExpenseData
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.ParticipantData
import com.babysplit.app.core.repository.TripData
import com.babysplit.app.core.repository.TripRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreTripRepository(
    private val db: FirebaseFirestore,
    private val userId: String
) : TripRepository {

    override fun getTripsStream(): Flow<List<TripData>> = callbackFlow {
        val listener = db.collection("trips")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents?.mapNotNull { doc ->
                    doc.toTripData()
                } ?: emptyList()

                trySend(trips)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getTripById(tripId: String): TripData? {
        val doc = db.collection("trips").document(tripId).get().await()
        return doc.toTripData()
    }

    override fun getTripStream(tripId: String): Flow<TripData?> = callbackFlow {
        val listener = db.collection("trips").document(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toTripData())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createTrip(trip: TripData): String {
        val tripId = UUID.randomUUID().toString()
        val inviteCode = generateRandomInviteCode()

        val tripMap = mapOf(
            "name" to trip.name,
            "currency" to trip.currency,
            "emoji" to trip.emoji,
            "simplifyDebts" to trip.simplifyDebts,
            "isFinished" to trip.isFinished,
            "createdBy" to userId,
            "inviteCode" to inviteCode,
            "memberIds" to listOf(userId),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("trips").document(tripId).set(tripMap).await()
        return tripId
    }

    override suspend fun updateTrip(
        tripId: String,
        name: String?,
        emoji: String?,
        currency: String?,
        simplifyDebts: Boolean?,
        isFinished: Boolean?
    ) {
        val updates = mutableMapOf<String, Any>()
        name?.let { updates["name"] = it }
        emoji?.let { updates["emoji"] = it }
        currency?.let { updates["currency"] = it }
        simplifyDebts?.let { updates["simplifyDebts"] = it }
        isFinished?.let { updates["isFinished"] = it }
        updates["updatedAt"] = FieldValue.serverTimestamp()

        if (updates.isNotEmpty()) {
            db.collection("trips").document(tripId).update(updates).await()
        }
    }

    override suspend fun deleteTrip(tripId: String) {
        // Fetch subcollections
        val members = db.collection("trips").document(tripId).collection("members").get().await()
        val expenses = db.collection("trips").document(tripId).collection("expenses").get().await()

        db.runBatch { batch ->
            for (doc in members) {
                batch.delete(doc.reference)
            }
            for (doc in expenses) {
                batch.delete(doc.reference)
            }
            batch.delete(db.collection("trips").document(tripId))
        }.await()
    }

    override fun getMembersStream(tripId: String): Flow<List<MemberData>> = callbackFlow {
        val listener = db.collection("trips").document(tripId).collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val members = snapshot?.documents?.mapNotNull { doc ->
                    doc.toMemberData()
                } ?: emptyList()

                trySend(members)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getMembersDirect(tripId: String): List<MemberData> {
        val snapshot = db.collection("trips").document(tripId).collection("members").get().await()
        return snapshot.documents.mapNotNull { it.toMemberData() }
    }

    override suspend fun addMember(tripId: String, member: MemberData): String {
        val memberId = UUID.randomUUID().toString()
        val memberMap = mapOf(
            "name" to member.name,
            "memberType" to member.memberType,
            "firebaseUid" to member.firebaseUid,
            "email" to member.email,
            "phoneNumber" to member.phoneNumber,
            "avatarColorHex" to member.avatarColorHex,
            "role" to member.role,
            "bankName" to member.bankName,
            "accountHolderName" to member.accountHolderName,
            "bankAccountNumber" to member.bankAccountNumber,
            "eWalletName" to member.eWalletName,
            "eWalletHandle" to member.eWalletHandle
        )

        db.runBatch { batch ->
            val memberRef = db.collection("trips").document(tripId).collection("members").document(memberId)
            batch.set(memberRef, memberMap)

            if (member.firebaseUid != null) {
                val tripRef = db.collection("trips").document(tripId)
                batch.update(tripRef, "memberIds", FieldValue.arrayUnion(member.firebaseUid))
            }
        }.await()

        return memberId
    }

    override suspend fun updateMember(tripId: String, member: MemberData) {
        val memberMap = mutableMapOf<String, Any?>(
            "name" to member.name,
            "memberType" to member.memberType,
            "firebaseUid" to member.firebaseUid,
            "email" to member.email,
            "phoneNumber" to member.phoneNumber,
            "avatarColorHex" to member.avatarColorHex,
            "role" to member.role,
            "bankName" to member.bankName,
            "accountHolderName" to member.accountHolderName,
            "bankAccountNumber" to member.bankAccountNumber,
            "eWalletName" to member.eWalletName,
            "eWalletHandle" to member.eWalletHandle
        )

        db.collection("trips").document(tripId).collection("members")
            .document(member.id).update(memberMap).await()
    }

    override suspend fun removeMember(tripId: String, memberId: String) {
        val memberDoc = db.collection("trips").document(tripId).collection("members").document(memberId).get().await()
        val firebaseUid = memberDoc.getString("firebaseUid")

        db.runBatch { batch ->
            batch.delete(db.collection("trips").document(tripId).collection("members").document(memberId))
            if (firebaseUid != null) {
                batch.update(db.collection("trips").document(tripId), "memberIds", FieldValue.arrayRemove(firebaseUid))
            }
        }.await()
    }

    override fun getExpensesStream(tripId: String): Flow<List<ExpenseData>> = callbackFlow {
        val listener = db.collection("trips").document(tripId).collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toExpenseData(tripId)
                } ?: emptyList()

                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getExpensesDirect(tripId: String): List<ExpenseData> {
        val snapshot = db.collection("trips").document(tripId).collection("expenses").get().await()
        return snapshot.documents.mapNotNull { it.toExpenseData(tripId) }
    }

    override suspend fun getExpenseById(tripId: String, expenseId: String): ExpenseData? {
        val doc = db.collection("trips").document(tripId).collection("expenses").document(expenseId).get().await()
        return doc.toExpenseData(tripId)
    }

    override suspend fun addExpense(tripId: String, expense: ExpenseData): String {
        val expenseId = UUID.randomUUID().toString()
        val expenseMap = expenseToMap(expense).toMutableMap()
        expenseMap["createdAt"] = FieldValue.serverTimestamp()
        expenseMap["updatedAt"] = FieldValue.serverTimestamp()

        db.collection("trips").document(tripId).collection("expenses").document(expenseId).set(expenseMap).await()
        return expenseId
    }

    override suspend fun updateExpense(tripId: String, expense: ExpenseData) {
        val expenseMap = expenseToMap(expense).toMutableMap()
        expenseMap["updatedAt"] = FieldValue.serverTimestamp()

        db.collection("trips").document(tripId).collection("expenses").document(expense.id).update(expenseMap).await()
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String) {
        db.collection("trips").document(tripId).collection("expenses").document(expenseId).delete().await()
    }

    override suspend fun joinTripByInviteCode(
        code: String,
        userId: String,
        userName: String,
        bankName: String?,
        accountHolderName: String?,
        bankAccountNumber: String?,
        eWalletName: String?,
        eWalletHandle: String?
    ): Result<String> {
        return try {
            val snapshot = db.collection("trips").whereEqualTo("inviteCode", code).get().await()
            if (snapshot.isEmpty) {
                return Result.failure(Exception("Trip not found"))
            }

            val tripDoc = snapshot.documents.first()
            val tripId = tripDoc.id

            // Check if user has saved payment details in user doc if not passed
            var finalBankName = bankName
            var finalHolderName = accountHolderName
            var finalBankAcc = bankAccountNumber
            var finalWalletName = eWalletName
            var finalWalletHandle = eWalletHandle

            if (finalBankAcc.isNullOrBlank() && finalWalletHandle.isNullOrBlank()) {
                try {
                    val userDoc = db.collection("users").document(userId).get().await()
                    if (userDoc.exists()) {
                        finalBankName = userDoc.getString("bankName")
                        finalHolderName = userDoc.getString("accountHolderName") ?: userName
                        finalBankAcc = userDoc.getString("bankAccountNumber")
                        finalWalletName = userDoc.getString("eWalletName")
                        finalWalletHandle = userDoc.getString("eWalletHandle")
                    }
                } catch (_: Exception) {}
            }

            val memberId = UUID.randomUUID().toString()
            val memberMap = mutableMapOf<String, Any?>(
                "name" to userName,
                "memberType" to "CLOUD_USER",
                "firebaseUid" to userId,
                "role" to "member",
                "avatarColorHex" to "#3F51B5",
                "bankName" to finalBankName,
                "accountHolderName" to (finalHolderName ?: userName),
                "bankAccountNumber" to finalBankAcc,
                "eWalletName" to finalWalletName,
                "eWalletHandle" to finalWalletHandle
            )

            db.runBatch { batch ->
                batch.update(tripDoc.reference, "memberIds", FieldValue.arrayUnion(userId))
                batch.set(db.collection("trips").document(tripId).collection("members").document(memberId), memberMap)
            }.await()

            Result.success(tripId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateInviteCode(tripId: String): String {
        val code = generateRandomInviteCode()
        db.collection("trips").document(tripId).update("inviteCode", code).await()
        return code
    }

    private fun generateRandomInviteCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..8).map { chars.random() }.joinToString("")
    }

    private fun DocumentSnapshot.toTripData(): TripData? {
        if (!exists()) return null
        return TripData(
            id = id,
            name = getString("name") ?: "",
            currency = getString("currency") ?: "USD",
            emoji = getString("emoji") ?: "✈️",
            simplifyDebts = getBoolean("simplifyDebts") ?: true,
            isFinished = getBoolean("isFinished") ?: false,
            createdBy = getString("createdBy") ?: "",
            inviteCode = getString("inviteCode") ?: "",
            isCloud = true,
            createdAtEpochMs = getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toMemberData(): MemberData? {
        if (!exists()) return null
        val tripId = reference.parent.parent?.id ?: ""
        return MemberData(
            id = id,
            tripId = tripId,
            name = getString("name") ?: "",
            memberType = getString("memberType") ?: "OFFLINE_TAGGED",
            firebaseUid = getString("firebaseUid"),
            email = getString("email"),
            phoneNumber = getString("phoneNumber"),
            avatarColorHex = getString("avatarColorHex") ?: "#3F51B5",
            role = getString("role") ?: "member",
            bankName = getString("bankName"),
            accountHolderName = getString("accountHolderName"),
            bankAccountNumber = getString("bankAccountNumber"),
            eWalletName = getString("eWalletName"),
            eWalletHandle = getString("eWalletHandle")
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toExpenseData(tripId: String): ExpenseData? {
        if (!exists()) return null

        val participantsList = get("participants") as? List<Map<String, Any>> ?: emptyList()
        val participants = participantsList.map { p ->
            ParticipantData(
                memberId = p["memberId"] as? String ?: "",
                memberName = p["memberName"] as? String ?: "",
                amountCents = (p["amountCents"] as? Number)?.toLong() ?: 0L,
                rawShareValue = (p["rawShareValue"] as? Number)?.toDouble() ?: 0.0
            )
        }

        return ExpenseData(
            id = id,
            tripId = tripId,
            title = getString("title") ?: "",
            totalAmountCents = getLong("totalAmountCents") ?: 0L,
            currency = getString("currency") ?: "USD",
            categoryName = getString("categoryName") ?: "GENERAL",
            paidByMemberId = getString("paidByMemberId") ?: "",
            paidByMemberName = getString("paidByMemberName") ?: "",
            splitType = getString("splitType") ?: "EQUAL",
            receiptImagePath = getString("receiptImagePath"),
            note = getString("note"),
            isSettlement = getBoolean("isSettlement") ?: false,
            createdBy = getString("createdBy") ?: "",
            isPendingSync = metadata.hasPendingWrites(),
            createdAtEpochMs = getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
            participants = participants
        )
    }

    private fun expenseToMap(expense: ExpenseData): Map<String, Any?> {
        val participantsMap = expense.participants.map { p ->
            mapOf(
                "memberId" to p.memberId,
                "memberName" to p.memberName,
                "amountCents" to p.amountCents,
                "rawShareValue" to p.rawShareValue
            )
        }

        return mapOf(
            "title" to expense.title,
            "totalAmountCents" to expense.totalAmountCents,
            "currency" to expense.currency,
            "categoryName" to expense.categoryName,
            "paidByMemberId" to expense.paidByMemberId,
            "paidByMemberName" to expense.paidByMemberName,
            "splitType" to expense.splitType,
            "receiptImagePath" to expense.receiptImagePath,
            "note" to expense.note,
            "isSettlement" to expense.isSettlement,
            "createdBy" to expense.createdBy,
            "participants" to participantsMap
        )
    }
}

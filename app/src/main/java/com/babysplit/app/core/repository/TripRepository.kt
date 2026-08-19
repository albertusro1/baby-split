package com.babysplit.app.core.repository

import kotlinx.coroutines.flow.Flow

/**
 * Unified domain models used by the repository interface.
 * These decouple the UI/ViewModel layer from specific storage implementations (Room or Firestore).
 */
data class TripData(
    val id: String,                     // UUID string (for both Room and Firestore)
    val name: String,
    val currency: String = "USD",
    val emoji: String = "✈️",
    val simplifyDebts: Boolean = true,
    val isFinished: Boolean = false,
    val createdBy: String = "",         // Firebase UID of creator (empty for local trips)
    val inviteCode: String = "",        // 8-char invite code for sharing
    val isCloud: Boolean = false,       // true if stored in Firestore
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class MemberData(
    val id: String,                     // UUID string
    val tripId: String,
    val name: String,
    val memberType: String = "OFFLINE_TAGGED",  // HOST, LINKED, OFFLINE_TAGGED
    val firebaseUid: String? = null,    // Firebase UID if this member is a signed-in user
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarColorHex: String = "#3F51B5",
    val role: String = "member",        // "admin" or "member"
    val bankName: String? = null,
    val accountHolderName: String? = null,
    val bankAccountNumber: String? = null,
    val eWalletName: String? = null,
    val eWalletHandle: String? = null
)

data class ExpenseData(
    val id: String,                     // UUID string
    val tripId: String,
    val title: String,
    val totalAmountCents: Long,
    val currency: String = "USD",
    val categoryName: String = "GENERAL",
    val paidByMemberId: String,
    val paidByMemberName: String,
    val splitType: String = "EQUAL",
    val receiptImagePath: String? = null,
    val note: String? = null,
    val isSettlement: Boolean = false,
    val createdBy: String = "",         // Firebase UID of the person who created this expense
    val isPendingSync: Boolean = false, // true when offline edit hasn't synced yet
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val participants: List<ParticipantData> = emptyList()
)

data class ParticipantData(
    val memberId: String,
    val memberName: String,
    val amountCents: Long,
    val rawShareValue: Double = 0.0
)

/**
 * Abstraction over trip data storage.
 * Implemented by LocalTripRepository (Room) and FirestoreTripRepository (Cloud Firestore).
 */
interface TripRepository {
    // --- Trips ---
    fun getTripsStream(): Flow<List<TripData>>
    suspend fun getTripById(tripId: String): TripData?
    fun getTripStream(tripId: String): Flow<TripData?>
    suspend fun createTrip(trip: TripData): String
    suspend fun updateTrip(tripId: String, name: String? = null, emoji: String? = null, currency: String? = null, simplifyDebts: Boolean? = null, isFinished: Boolean? = null)
    suspend fun deleteTrip(tripId: String)

    // --- Members ---
    fun getMembersStream(tripId: String): Flow<List<MemberData>>
    suspend fun getMembersDirect(tripId: String): List<MemberData>
    suspend fun addMember(tripId: String, member: MemberData): String
    suspend fun updateMember(tripId: String, member: MemberData)
    suspend fun removeMember(tripId: String, memberId: String)

    // --- Expenses ---
    fun getExpensesStream(tripId: String): Flow<List<ExpenseData>>
    suspend fun getExpensesDirect(tripId: String): List<ExpenseData>
    suspend fun getExpenseById(tripId: String, expenseId: String): ExpenseData?
    suspend fun addExpense(tripId: String, expense: ExpenseData): String
    suspend fun updateExpense(tripId: String, expense: ExpenseData)
    suspend fun deleteExpense(tripId: String, expenseId: String)

    // --- Sharing (only applicable for cloud repository, local returns failure) ---
    suspend fun joinTripByInviteCode(code: String, userId: String, userName: String): Result<String>
    suspend fun generateInviteCode(tripId: String): String
}

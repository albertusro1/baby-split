package com.babysplit.app.core.database

import com.babysplit.app.core.database.dao.ExpenseDao
import com.babysplit.app.core.database.dao.GroupDao
import com.babysplit.app.core.database.dao.MemberDao
import com.babysplit.app.core.database.entity.ExpenseEntity
import com.babysplit.app.core.database.entity.ExpenseParticipantEntity
import com.babysplit.app.core.database.entity.GroupEntity
import com.babysplit.app.core.database.entity.MemberEntity
import com.babysplit.app.core.repository.ExpenseData
import com.babysplit.app.core.repository.MemberData
import com.babysplit.app.core.repository.ParticipantData
import com.babysplit.app.core.repository.TripData
import com.babysplit.app.core.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalTripRepository(
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    private val expenseDao: ExpenseDao
) : TripRepository {

    override fun getTripsStream(): Flow<List<TripData>> {
        return groupDao.getAllGroups().map { groups ->
            groups.map { it.toTripData() }
        }
    }

    override suspend fun getTripById(tripId: String): TripData? {
        val id = tripId.toLongOrNull() ?: return null
        return groupDao.getGroupByIdDirect(id)?.toTripData()
    }

    override fun getTripStream(tripId: String): Flow<TripData?> {
        val id = tripId.toLongOrNull() ?: 0L
        return groupDao.getGroupById(id).map { it?.toTripData() }
    }

    override suspend fun createTrip(trip: TripData): String {
        val entity = GroupEntity(
            name = trip.name,
            currency = trip.currency,
            emoji = trip.emoji,
            simplifyDebts = trip.simplifyDebts,
            isFinished = trip.isFinished,
            createdAtEpochMs = trip.createdAtEpochMs
        )
        val id = groupDao.insertGroup(entity)
        return id.toString()
    }

    override suspend fun updateTrip(
        tripId: String,
        name: String?,
        emoji: String?,
        currency: String?,
        simplifyDebts: Boolean?,
        isFinished: Boolean?
    ) {
        val id = tripId.toLongOrNull() ?: return
        val currentGroup = groupDao.getGroupByIdDirect(id) ?: return
        
        val updatedGroup = currentGroup.copy(
            name = name ?: currentGroup.name,
            emoji = emoji ?: currentGroup.emoji,
            currency = currency ?: currentGroup.currency,
            simplifyDebts = simplifyDebts ?: currentGroup.simplifyDebts,
            isFinished = isFinished ?: currentGroup.isFinished
        )
        groupDao.updateGroup(updatedGroup)
    }

    override suspend fun deleteTrip(tripId: String) {
        val id = tripId.toLongOrNull() ?: return
        groupDao.deleteFullGroup(id)
    }

    override fun getMembersStream(tripId: String): Flow<List<MemberData>> {
        val id = tripId.toLongOrNull() ?: 0L
        return memberDao.getMembersForGroup(id).map { members ->
            members.map { it.toMemberData(tripId) }
        }
    }

    override suspend fun getMembersDirect(tripId: String): List<MemberData> {
        val id = tripId.toLongOrNull() ?: return emptyList()
        return memberDao.getMembersForGroupDirect(id).map { it.toMemberData(tripId) }
    }

    override suspend fun addMember(tripId: String, member: MemberData): String {
        val groupId = tripId.toLongOrNull() ?: return ""
        val entity = MemberEntity(
            groupId = groupId,
            name = member.name,
            memberType = member.memberType,
            email = member.email,
            phoneNumber = member.phoneNumber,
            avatarColorHex = member.avatarColorHex,
            bankName = member.bankName,
            accountHolderName = member.accountHolderName,
            bankAccountNumber = member.bankAccountNumber,
            eWalletName = member.eWalletName,
            eWalletHandle = member.eWalletHandle
        )
        val newId = memberDao.insertMember(entity)
        return newId.toString()
    }

    override suspend fun updateMember(tripId: String, member: MemberData) {
        val groupId = tripId.toLongOrNull() ?: return
        val memberId = member.id.toLongOrNull() ?: return
        
        val entity = MemberEntity(
            id = memberId,
            groupId = groupId,
            name = member.name,
            memberType = member.memberType,
            email = member.email,
            phoneNumber = member.phoneNumber,
            avatarColorHex = member.avatarColorHex,
            bankName = member.bankName,
            accountHolderName = member.accountHolderName,
            bankAccountNumber = member.bankAccountNumber,
            eWalletName = member.eWalletName,
            eWalletHandle = member.eWalletHandle
        )
        memberDao.updateMember(entity)
    }

    override suspend fun removeMember(tripId: String, memberId: String) {
        val groupId = tripId.toLongOrNull() ?: return
        val id = memberId.toLongOrNull() ?: return
        val members = memberDao.getMembersForGroupDirect(groupId)
        val member = members.find { it.id == id }
        if (member != null) {
            memberDao.deleteMember(member)
        }
    }

    override fun getExpensesStream(tripId: String): Flow<List<ExpenseData>> {
        val groupId = tripId.toLongOrNull() ?: 0L
        return expenseDao.getExpensesWithParticipants(groupId).map { expenses ->
            expenses.map { 
                ExpenseData(
                    id = it.expense.id,
                    tripId = tripId,
                    title = it.expense.title,
                    totalAmountCents = it.expense.totalAmountCents,
                    currency = it.expense.currency,
                    categoryName = it.expense.categoryName,
                    paidByMemberId = it.expense.paidByMemberId.toString(),
                    paidByMemberName = it.expense.paidByMemberName,
                    splitType = it.expense.splitType,
                    receiptImagePath = it.expense.receiptImagePath,
                    note = it.expense.note,
                    isSettlement = it.expense.isSettlement,
                    createdBy = "",
                    isPendingSync = false,
                    createdAtEpochMs = it.expense.createdAtEpochMs,
                    participants = it.participants.map { p ->
                        ParticipantData(
                            memberId = p.memberId.toString(),
                            memberName = p.memberName,
                            amountCents = p.amountCents,
                            rawShareValue = p.rawShareValue
                        )
                    }
                )
            }
        }
    }

    override suspend fun getExpensesDirect(tripId: String): List<ExpenseData> {
        val groupId = tripId.toLongOrNull() ?: return emptyList()
        return expenseDao.getExpensesWithParticipantsDirect(groupId).map {
            ExpenseData(
                id = it.expense.id,
                tripId = tripId,
                title = it.expense.title,
                totalAmountCents = it.expense.totalAmountCents,
                currency = it.expense.currency,
                categoryName = it.expense.categoryName,
                paidByMemberId = it.expense.paidByMemberId.toString(),
                paidByMemberName = it.expense.paidByMemberName,
                splitType = it.expense.splitType,
                receiptImagePath = it.expense.receiptImagePath,
                note = it.expense.note,
                isSettlement = it.expense.isSettlement,
                createdBy = "",
                isPendingSync = false,
                createdAtEpochMs = it.expense.createdAtEpochMs,
                participants = it.participants.map { p ->
                    ParticipantData(
                        memberId = p.memberId.toString(),
                        memberName = p.memberName,
                        amountCents = p.amountCents,
                        rawShareValue = p.rawShareValue
                    )
                }
            )
        }
    }

    override suspend fun getExpenseById(tripId: String, expenseId: String): ExpenseData? {
        val expenseWithParticipants = expenseDao.getExpenseWithParticipantsDirect(expenseId) ?: return null
        return ExpenseData(
            id = expenseWithParticipants.expense.id,
            tripId = tripId,
            title = expenseWithParticipants.expense.title,
            totalAmountCents = expenseWithParticipants.expense.totalAmountCents,
            currency = expenseWithParticipants.expense.currency,
            categoryName = expenseWithParticipants.expense.categoryName,
            paidByMemberId = expenseWithParticipants.expense.paidByMemberId.toString(),
            paidByMemberName = expenseWithParticipants.expense.paidByMemberName,
            splitType = expenseWithParticipants.expense.splitType,
            receiptImagePath = expenseWithParticipants.expense.receiptImagePath,
            note = expenseWithParticipants.expense.note,
            isSettlement = expenseWithParticipants.expense.isSettlement,
            createdBy = "",
            isPendingSync = false,
            createdAtEpochMs = expenseWithParticipants.expense.createdAtEpochMs,
            participants = expenseWithParticipants.participants.map { p ->
                ParticipantData(
                    memberId = p.memberId.toString(),
                    memberName = p.memberName,
                    amountCents = p.amountCents,
                    rawShareValue = p.rawShareValue
                )
            }
        )
    }

    override suspend fun addExpense(tripId: String, expense: ExpenseData): String {
        val groupId = tripId.toLongOrNull() ?: return ""
        val expenseId = if (expense.id.isEmpty() || expense.id == "0") UUID.randomUUID().toString() else expense.id
        
        val expenseEntity = ExpenseEntity(
            id = expenseId,
            groupId = groupId,
            title = expense.title,
            totalAmountCents = expense.totalAmountCents,
            currency = expense.currency,
            categoryName = expense.categoryName,
            paidByMemberId = expense.paidByMemberId.toLongOrNull() ?: 0L,
            paidByMemberName = expense.paidByMemberName,
            splitType = expense.splitType,
            receiptImagePath = expense.receiptImagePath,
            note = expense.note,
            createdAtEpochMs = expense.createdAtEpochMs,
            isSettlement = expense.isSettlement
        )
        
        val participants = expense.participants.map {
            ExpenseParticipantEntity(
                expenseId = expenseId,
                memberId = it.memberId.toLongOrNull() ?: 0L,
                memberName = it.memberName,
                amountCents = it.amountCents,
                rawShareValue = it.rawShareValue
            )
        }
        
        expenseDao.insertFullExpense(expenseEntity, participants)
        return expenseId
    }

    override suspend fun updateExpense(tripId: String, expense: ExpenseData) {
        val groupId = tripId.toLongOrNull() ?: return
        val expenseEntity = ExpenseEntity(
            id = expense.id,
            groupId = groupId,
            title = expense.title,
            totalAmountCents = expense.totalAmountCents,
            currency = expense.currency,
            categoryName = expense.categoryName,
            paidByMemberId = expense.paidByMemberId.toLongOrNull() ?: 0L,
            paidByMemberName = expense.paidByMemberName,
            splitType = expense.splitType,
            receiptImagePath = expense.receiptImagePath,
            note = expense.note,
            createdAtEpochMs = expense.createdAtEpochMs,
            isSettlement = expense.isSettlement
        )
        
        val participants = expense.participants.map {
            ExpenseParticipantEntity(
                expenseId = expense.id,
                memberId = it.memberId.toLongOrNull() ?: 0L,
                memberName = it.memberName,
                amountCents = it.amountCents,
                rawShareValue = it.rawShareValue
            )
        }
        
        expenseDao.deleteFullExpense(expense.id)
        expenseDao.insertFullExpense(expenseEntity, participants)
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String) {
        expenseDao.deleteFullExpense(expenseId)
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
        return Result.failure(UnsupportedOperationException("Sharing is not available in offline mode"))
    }

    override suspend fun generateInviteCode(tripId: String): String {
        return ""
    }
    
    private fun GroupEntity.toTripData(): TripData {
        return TripData(
            id = id.toString(),
            name = name,
            currency = currency,
            emoji = emoji,
            simplifyDebts = simplifyDebts,
            isFinished = isFinished,
            createdBy = "",
            inviteCode = "",
            isCloud = false,
            createdAtEpochMs = createdAtEpochMs
        )
    }
    
    private fun MemberEntity.toMemberData(tripId: String): MemberData {
        return MemberData(
            id = id.toString(),
            tripId = tripId,
            name = name,
            memberType = memberType,
            firebaseUid = null,
            email = email,
            phoneNumber = phoneNumber,
            avatarColorHex = avatarColorHex,
            role = "member",
            bankName = bankName,
            accountHolderName = accountHolderName,
            bankAccountNumber = bankAccountNumber,
            eWalletName = eWalletName,
            eWalletHandle = eWalletHandle
        )
    }
}

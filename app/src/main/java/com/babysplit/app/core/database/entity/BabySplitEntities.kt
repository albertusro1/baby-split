package com.babysplit.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val defaultCurrency: String = "USD",
    val isGoogleUser: Boolean = false
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String = "USD",
    val emoji: String = "✈️",
    val simplifyDebts: Boolean = true,
    val isFinished: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val memberType: String = "OFFLINE_TAGGED", // HOST, OFFLINE_TAGGED, GMAIL_INVITED
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarColorHex: String = "#3F51B5"
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: Long,
    val title: String,
    val totalAmountCents: Long,
    val currency: String = "USD",
    val categoryName: String = "GENERAL",
    val paidByMemberId: Long,
    val paidByMemberName: String,
    val splitType: String = "EQUAL",
    val receiptImagePath: String? = null,
    val note: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val isSettlement: Boolean = false
)

@Entity(
    tableName = "expense_participants",
    primaryKeys = ["expenseId", "memberId"]
)
data class ExpenseParticipantEntity(
    val expenseId: String,
    val memberId: Long,
    val memberName: String,
    val amountCents: Long,
    val rawShareValue: Double = 0.0
)

@Entity(
    tableName = "cached_currency_rates",
    primaryKeys = ["fromCurrency", "toCurrency"]
)
data class CachedCurrencyRateEntity(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val lastFetchedEpochMs: Long = System.currentTimeMillis()
)

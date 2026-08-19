package com.babysplit.app.core.database.dao

import androidx.room.*
import com.babysplit.app.core.database.entity.ExpenseEntity
import com.babysplit.app.core.database.entity.ExpenseParticipantEntity
import kotlinx.coroutines.flow.Flow

data class ExpenseWithParticipants(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val participants: List<ExpenseParticipantEntity>
)

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAtEpochMs DESC")
    fun getExpensesWithParticipants(groupId: Long): Flow<List<ExpenseWithParticipants>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAtEpochMs DESC")
    suspend fun getExpensesWithParticipantsDirect(groupId: Long): List<ExpenseWithParticipants>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ExpenseParticipantEntity>)

    @Transaction
    suspend fun insertFullExpense(
        expense: ExpenseEntity,
        participants: List<ExpenseParticipantEntity>
    ) {
        insertExpense(expense)
        deleteParticipantsForExpense(expense.id)
        insertParticipants(participants)
    }

    @Query("DELETE FROM expense_participants WHERE expenseId = :expenseId")
    suspend fun deleteParticipantsForExpense(expenseId: String)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

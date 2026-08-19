package com.babysplit.app.core.database.dao

import androidx.room.*
import com.babysplit.app.core.database.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAtEpochMs DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun getGroupById(groupId: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupByIdDirect(groupId: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}

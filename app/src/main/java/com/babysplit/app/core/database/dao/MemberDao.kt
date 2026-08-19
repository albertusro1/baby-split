package com.babysplit.app.core.database.dao

import androidx.room.*
import com.babysplit.app.core.database.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY id ASC")
    fun getMembersForGroup(groupId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY id ASC")
    suspend fun getMembersForGroupDirect(groupId: Long): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)
}

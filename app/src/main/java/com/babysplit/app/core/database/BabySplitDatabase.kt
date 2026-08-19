package com.babysplit.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.babysplit.app.core.database.dao.ExpenseDao
import com.babysplit.app.core.database.dao.GroupDao
import com.babysplit.app.core.database.dao.MemberDao
import com.babysplit.app.core.database.entity.*

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        ExpenseParticipantEntity::class,
        CachedCurrencyRateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BabySplitDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: BabySplitDatabase? = null

        fun getDatabase(context: Context): BabySplitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BabySplitDatabase::class.java,
                    "babysplit_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

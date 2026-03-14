package com.example.personalwealthmanager.core.sms.queue

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SmsQueueEntity::class], version = 2, exportSchema = false)
abstract class SmsQueueDatabase : RoomDatabase() {
    abstract fun dao(): SmsQueueDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sms_queue ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE sms_queue ADD COLUMN errorMessage TEXT")
                database.execSQL("ALTER TABLE sms_queue ADD COLUMN processedAt INTEGER")
            }
        }
    }
}

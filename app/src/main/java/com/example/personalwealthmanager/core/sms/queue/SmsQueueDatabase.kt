package com.example.personalwealthmanager.core.sms.queue

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SmsQueueEntity::class], version = 1, exportSchema = false)
abstract class SmsQueueDatabase : RoomDatabase() {
    abstract fun dao(): SmsQueueDao
}

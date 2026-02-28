package com.example.personalwealthmanager.core.sms.queue

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SmsQueueDao {
    @Insert
    suspend fun insert(item: SmsQueueEntity): Long

    @Delete
    suspend fun delete(item: SmsQueueEntity)

    @Query("SELECT * FROM sms_queue ORDER BY enqueuedAt ASC")
    suspend fun getAll(): List<SmsQueueEntity>

    @Query("SELECT COUNT(*) FROM sms_queue")
    suspend fun count(): Int
}

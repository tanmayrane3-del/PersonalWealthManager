package com.example.personalwealthmanager.core.sms.queue

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_queue")
data class SmsQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val timestampMs: Long,
    val enqueuedAt: Long = System.currentTimeMillis()
)

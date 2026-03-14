package com.example.personalwealthmanager.core.sms.queue

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Status values:
 * PENDING  — received, waiting to be sent to backend
 * SUCCESS  — backend recorded the transaction
 * SKIPPED  — backend confirmed not a transaction (OTP, promo, etc.)
 * FAILED   — permanent failure (session expired, etc.) — won't retry
 */
@Entity(tableName = "sms_queue")
data class SmsQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val timestampMs: Long,
    val enqueuedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val errorMessage: String? = null,
    val processedAt: Long? = null
)

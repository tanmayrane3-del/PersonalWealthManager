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

    /** Only returns items that still need processing */
    @Query("SELECT * FROM sms_queue WHERE status = 'PENDING' ORDER BY enqueuedAt ASC")
    suspend fun getPending(): List<SmsQueueEntity>

    @Query("SELECT COUNT(*) FROM sms_queue WHERE status = 'PENDING'")
    suspend fun count(): Int

    @Query("UPDATE sms_queue SET status = :status, errorMessage = :error, processedAt = :processedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String?, processedAt: Long)

    /** For the SMS Log screen — recent 100 entries across all statuses */
    @Query("SELECT * FROM sms_queue ORDER BY enqueuedAt DESC LIMIT 100")
    suspend fun getRecent(): List<SmsQueueEntity>

    /** Reset a FAILED item back to PENDING so it gets retried */
    @Query("UPDATE sms_queue SET status = 'PENDING', errorMessage = NULL, processedAt = NULL WHERE id = :id")
    suspend fun resetToPending(id: Long)

    /** Purge all non-pending entries older than 15 days to keep the table bounded */
    @Query("DELETE FROM sms_queue WHERE status IN ('SUCCESS', 'SKIPPED', 'FAILED') AND enqueuedAt < :cutoffMs")
    suspend fun purgeOldResolved(cutoffMs: Long)

    /** Dedup check — ±5s tolerance to handle PDU ms vs content://sms/inbox date differences. */
    @Query("SELECT COUNT(*) FROM sms_queue WHERE sender = :sender AND body = :body AND ABS(timestampMs / 1000 - :timestampMs / 1000) <= 5")
    suspend fun existsBySenderBodyTimestamp(sender: String, body: String, timestampMs: Long): Int
}

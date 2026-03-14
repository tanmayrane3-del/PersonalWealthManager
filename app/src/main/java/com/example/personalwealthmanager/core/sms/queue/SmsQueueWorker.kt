package com.example.personalwealthmanager.core.sms.queue

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.core.sms.SmsReceiver
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.api.AdminApi
import com.example.personalwealthmanager.data.remote.api.QueueStatsRequest
import com.example.personalwealthmanager.data.remote.api.SmsApi
import com.example.personalwealthmanager.data.remote.dto.SmsParseRequest
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SmsQueueWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val dao: SmsQueueDao,
    private val smsApi: SmsApi,
    private val adminApi: AdminApi,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsQueueWorker"
        const val WORK_NAME = "sms_queue_process"
        private val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }

    override suspend fun doWork(): Result {
        val token = sessionManager.getSessionToken()
        if (token == null) {
            Log.d(TAG, "User not logged in — skipping queue processing")
            return Result.failure()
        }

        // Purge all resolved/failed entries older than 30 days
        dao.purgeOldResolved(System.currentTimeMillis() - THIRTY_DAYS_MS)

        val items = dao.getPending()
        if (items.isEmpty()) {
            Log.d(TAG, "Queue empty — nothing to process")
            return Result.success()
        }

        Log.d(TAG, "Processing ${items.size} pending SMS(es)")

        for (item in items) {
            try {
                val response = smsApi.parseSms(
                    sessionToken = token,
                    request = SmsParseRequest(
                        sender = item.sender,
                        body = item.body,
                        timestampMs = item.timestampMs
                    )
                )

                val now = System.currentTimeMillis()

                when {
                    response.isSuccessful -> {
                        val result = response.body()?.data
                        if (result?.recorded == true) {
                            dao.updateStatus(item.id, "SUCCESS", null, now)
                            showSuccessNotification(
                                type = result.type ?: "expense",
                                amount = result.amount ?: "?",
                                party = if (result.type == "income") result.source ?: "Unknown"
                                        else result.recipient ?: "Unknown",
                                bank = result.bank ?: item.sender,
                                isUnmatched = result.isUnmatched == true
                            )
                            Log.d(TAG, "SMS recorded successfully — id=${item.id}")
                        } else {
                            // Not a transaction (OTP, promo, etc.) — mark SKIPPED, don't retry
                            dao.updateStatus(item.id, "SKIPPED", result?.reason ?: "Not a transaction", now)
                            Log.d(TAG, "SMS skipped (not a transaction) — id=${item.id}")
                        }
                    }
                    response.code() == 401 -> {
                        Log.w(TAG, "Session expired — marking FAILED, stopping")
                        dao.updateStatus(item.id, "FAILED", "Session expired (401)", now)
                        reportQueueCount()
                        return Result.failure()
                    }
                    else -> {
                        // 5xx / network issue — keep PENDING, WorkManager retries via backoff
                        Log.w(TAG, "Backend returned ${response.code()} for id=${item.id} — will retry")
                        reportQueueCount()
                        return Result.retry()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error processing SMS id=${item.id}: ${e.message}")
                reportQueueCount()
                return Result.retry()
            }
        }

        reportQueueCount()

        // Re-enqueue if new SMS arrived while we were processing (race condition guard)
        if (dao.count() > 0) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SmsQueueWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .build()
            )
            Log.d(TAG, "New SMS arrived during processing — re-enqueueing worker")
        }

        return Result.success()
    }

    private suspend fun reportQueueCount() {
        try {
            val count = dao.count()
            adminApi.reportQueueCount(QueueStatsRequest(pending = count))
            Log.d(TAG, "Queue count reported: $count pending")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to report queue count: ${e.message}")
        }
    }

    private fun showSuccessNotification(
        type: String,
        amount: String,
        party: String,
        bank: String,
        isUnmatched: Boolean
    ) {
        val isIncome = type == "income"
        val title = if (isIncome) "Income Recorded — $bank" else "Expense Recorded — $bank"
        val message = when {
            isUnmatched && isIncome  -> "Rs.$amount from $party (unmatched — tap to review)"
            isUnmatched && !isIncome -> "Rs.$amount to $party (unmatched — tap to review)"
            isIncome                 -> "Rs.$amount from $party added"
            else                     -> "Rs.$amount to $party added"
        }

        val tapIntent = Intent(context, TransactionsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SmsReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

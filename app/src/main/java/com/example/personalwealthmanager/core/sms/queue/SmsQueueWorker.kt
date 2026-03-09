package com.example.personalwealthmanager.core.sms.queue

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
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
        private const val POLL_INTERVAL_MINUTES = 5L
    }

    override suspend fun doWork(): Result {
        val token = sessionManager.getSessionToken()
        if (token == null) {
            Log.d(TAG, "User not logged in — skipping queue processing")
            return Result.failure()
        }

        val items = dao.getAll()
        if (items.isEmpty()) return Result.success()

        Log.d(TAG, "Processing ${items.size} queued SMS(es)")

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

                when {
                    response.isSuccessful -> {
                        dao.delete(item)
                        val result = response.body()?.data
                        if (result?.recorded == true) {
                            showSuccessNotification(
                                type = result.type ?: "expense",
                                amount = result.amount ?: "?",
                                party = if (result.type == "income") result.source ?: "Unknown"
                                        else result.recipient ?: "Unknown",
                                bank = result.bank ?: item.sender,
                                isUnmatched = result.isUnmatched == true
                            )
                        }
                        Log.d(TAG, "SMS processed successfully — id=${item.id}")
                    }
                    response.code() == 401 -> {
                        Log.w(TAG, "Session expired — stopping queue processing")
                        reportQueueCount()
                        return Result.failure()
                    }
                    else -> {
                        Log.w(TAG, "Backend returned ${response.code()} — will retry")
                        reportQueueCount()
                        scheduleNextRun()
                        return Result.retry()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error processing SMS id=${item.id}: ${e.message}")
                reportQueueCount()
                scheduleNextRun()
                return Result.retry()
            }
        }

        reportQueueCount()
        scheduleNextRun()
        return Result.success()
    }

    private fun scheduleNextRun() {
        val next = OneTimeWorkRequestBuilder<SmsQueueWorker>()
            .setInitialDelay(POLL_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            next
        )
        Log.d(TAG, "Next queue check scheduled in ${POLL_INTERVAL_MINUTES}m")
    }

    private suspend fun reportQueueCount() {
        try {
            val count = dao.count()
            adminApi.reportQueueCount(QueueStatsRequest(pending = count))
            Log.d(TAG, "Queue count reported to backend: $count pending")
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

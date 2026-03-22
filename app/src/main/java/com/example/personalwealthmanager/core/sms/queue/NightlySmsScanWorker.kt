package com.example.personalwealthmanager.core.sms.queue

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.personalwealthmanager.core.sms.SmsReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class NightlySmsScanWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val dao: SmsQueueDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "nightly_sms_scan"
        private const val TAG = "NightlySmsScanWorker"
        private const val HOURS_BACK = 24L
    }

    override suspend fun doWork(): Result {
        val cutoffMs = System.currentTimeMillis() - HOURS_BACK * 60 * 60 * 1000L
        var queued = 0

        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                "date > ?",
                arrayOf(cutoffMs.toString()),
                "date DESC"
            ) ?: return Result.success()

            cursor.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val sender = it.getString(addrIdx) ?: continue
                    val body   = it.getString(bodyIdx) ?: continue
                    val ts     = it.getLong(dateIdx)

                    if (SmsReceiver.ALLOWED_SENDERS.none { s -> sender.contains(s, ignoreCase = true) }) continue
                    if (dao.existsBySenderBodyTimestamp(sender, body, ts) > 0) continue

                    dao.insert(SmsQueueEntity(sender = sender, body = body, timestampMs = ts))
                    queued++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SMS inbox: ${e.message}")
            return Result.retry()
        }

        Log.d(TAG, "Nightly scan complete — $queued new SMS queued")

        if (queued > 0) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                SmsQueueWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SmsQueueWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                    .build()
            )
        }

        return Result.success()
    }
}

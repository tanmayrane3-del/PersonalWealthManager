package com.example.personalwealthmanager.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.personalwealthmanager.core.sms.queue.SmsQueueEntity
import com.example.personalwealthmanager.core.sms.queue.SmsQueueWorker
import com.example.personalwealthmanager.core.utils.SessionManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        const val CHANNEL_ID = "sms_transaction_channel"
        const val CHANNEL_NAME = "SMS Transactions"

        val ALLOWED_SENDERS = listOf(
            "ICICIT-S",
            "BOBSMS-S",
            "KOTAKB-S",
            "SBICRD-S",
            "INDUSB-S"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called — action=${intent.action}")

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                SmsReceiverEntryPoint::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, "Hilt not ready, dropping SMS: ${e.message}")
            return
        }
        val sessionManager: SessionManager = entryPoint.sessionManager()
        val dao = entryPoint.smsQueueDao()

        val token = sessionManager.getSessionToken()
        if (token == null) {
            Log.d(TAG, "User not logged in, skipping SMS")
            return
        }

        // Group multi-part SMS by sender
        val grouped = messages.groupBy { it.originatingAddress ?: "" }

        grouped.forEach { (sender, parts) ->
            if (ALLOWED_SENDERS.none { sender.contains(it, ignoreCase = true) }) {
                Log.d(TAG, "Skipping SMS from $sender — not a known bank sender")
                return@forEach
            }

            val body = parts.joinToString("") { it.messageBody }
            val timestampMs = parts.first().timestampMillis

            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            scope.launch {
                try {
                    // Save to persistent queue first — guaranteed not to lose the SMS
                    dao.insert(SmsQueueEntity(sender = sender, body = body, timestampMs = timestampMs))
                    Log.d(TAG, "SMS queued from $sender")

                    // Enqueue WorkManager to process when network is available
                    val workRequest = OneTimeWorkRequestBuilder<SmsQueueWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                        .build()

                    WorkManager.getInstance(context).enqueueUniqueWork(
                        "sms_queue_process",
                        ExistingWorkPolicy.KEEP, // don't cancel in-flight worker
                        workRequest
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to queue SMS: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

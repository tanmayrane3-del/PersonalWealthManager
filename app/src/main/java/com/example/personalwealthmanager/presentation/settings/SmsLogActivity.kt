package com.example.personalwealthmanager.presentation.settings

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.core.sms.queue.SmsQueueDao
import com.example.personalwealthmanager.core.sms.queue.SmsQueueEntity
import com.example.personalwealthmanager.core.sms.queue.SmsQueueWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SmsLogActivity : AppCompatActivity() {

    @Inject lateinit var dao: SmsQueueDao

    private lateinit var adapter: SmsLogAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var pendingBanner: LinearLayout
    private lateinit var tvPendingCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_log)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        tvEmpty = findViewById(R.id.tvEmpty)
        pendingBanner = findViewById(R.id.pendingBanner)
        tvPendingCount = findViewById(R.id.tvPendingCount)

        adapter = SmsLogAdapter(emptyList(), onRetry = ::retryItem)
        val rv = findViewById<RecyclerView>(R.id.rvSmsLog)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadLogs()
    }

    private fun retryItem(item: SmsQueueEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            // Reset to PENDING so the worker picks it up
            dao.resetToPending(item.id)

            // Trigger worker immediately
            WorkManager.getInstance(this@SmsLogActivity).enqueueUniqueWork(
                SmsQueueWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SmsQueueWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SmsLogActivity, "Retrying…", Toast.LENGTH_SHORT).show()
                loadLogs()
            }
        }
    }

    private fun loadLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            val items = dao.getRecent()
            val pendingCount = dao.count()

            withContext(Dispatchers.Main) {
                adapter.updateItems(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

                if (pendingCount > 0) {
                    tvPendingCount.text = "$pendingCount SMS pending processing"
                    pendingBanner.visibility = View.VISIBLE
                } else {
                    pendingBanner.visibility = View.GONE
                }
            }
        }
    }
}

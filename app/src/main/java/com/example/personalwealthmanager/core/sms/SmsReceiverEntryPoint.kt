package com.example.personalwealthmanager.core.sms

import com.example.personalwealthmanager.core.sms.queue.SmsQueueDao
import com.example.personalwealthmanager.core.utils.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsReceiverEntryPoint {
    fun sessionManager(): SessionManager
    fun smsQueueDao(): SmsQueueDao
}

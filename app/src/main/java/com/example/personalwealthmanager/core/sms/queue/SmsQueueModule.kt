package com.example.personalwealthmanager.core.sms.queue

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmsQueueModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmsQueueDatabase =
        Room.databaseBuilder(context, SmsQueueDatabase::class.java, "sms_queue.db").build()

    @Provides
    fun provideDao(db: SmsQueueDatabase): SmsQueueDao = db.dao()
}

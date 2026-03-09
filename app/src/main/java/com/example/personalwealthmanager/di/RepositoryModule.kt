package com.example.personalwealthmanager.di

import com.example.personalwealthmanager.data.repository.AuthRepositoryImpl
import com.example.personalwealthmanager.data.repository.CategoryRepositoryImpl
import com.example.personalwealthmanager.data.repository.HoldingsRepositoryImpl
import com.example.personalwealthmanager.data.repository.RecipientRepositoryImpl
import com.example.personalwealthmanager.data.repository.SourceRepositoryImpl
import com.example.personalwealthmanager.data.repository.TransactionRepositoryImpl
import com.example.personalwealthmanager.data.repository.ZerodhaRepositoryImpl
import com.example.personalwealthmanager.domain.repository.AuthRepository
import com.example.personalwealthmanager.domain.repository.CategoryRepository
import com.example.personalwealthmanager.domain.repository.HoldingsRepository
import com.example.personalwealthmanager.domain.repository.RecipientRepository
import com.example.personalwealthmanager.domain.repository.SourceRepository
import com.example.personalwealthmanager.domain.repository.TransactionRepository
import com.example.personalwealthmanager.domain.repository.ZerodhaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSourceRepository(
        sourceRepositoryImpl: SourceRepositoryImpl
    ): SourceRepository

    @Binds
    @Singleton
    abstract fun bindRecipientRepository(
        recipientRepositoryImpl: RecipientRepositoryImpl
    ): RecipientRepository

    @Binds
    @Singleton
    abstract fun bindZerodhaRepository(
        zerodhaRepositoryImpl: ZerodhaRepositoryImpl
    ): ZerodhaRepository

    @Binds
    @Singleton
    abstract fun bindHoldingsRepository(
        holdingsRepositoryImpl: HoldingsRepositoryImpl
    ): HoldingsRepository
}

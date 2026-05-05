package com.pwm.personalwealthmanager.di

import com.pwm.personalwealthmanager.data.repository.AuthRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.BudgetRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.MacroRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.CategoryRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.HoldingsRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.MetalsRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.MutualFundRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.PhysicalAssetRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.LiabilityRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.NetWorthRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.RecipientRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.SourceRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.TransactionRepositoryImpl
import com.pwm.personalwealthmanager.data.repository.ZerodhaRepositoryImpl
import com.pwm.personalwealthmanager.domain.repository.AuthRepository
import com.pwm.personalwealthmanager.domain.repository.BudgetRepository
import com.pwm.personalwealthmanager.domain.repository.MacroRepository
import com.pwm.personalwealthmanager.domain.repository.CategoryRepository
import com.pwm.personalwealthmanager.domain.repository.HoldingsRepository
import com.pwm.personalwealthmanager.domain.repository.MetalsRepository
import com.pwm.personalwealthmanager.domain.repository.MutualFundRepository
import com.pwm.personalwealthmanager.domain.repository.PhysicalAssetRepository
import com.pwm.personalwealthmanager.domain.repository.LiabilityRepository
import com.pwm.personalwealthmanager.domain.repository.NetWorthRepository
import com.pwm.personalwealthmanager.domain.repository.RecipientRepository
import com.pwm.personalwealthmanager.domain.repository.SourceRepository
import com.pwm.personalwealthmanager.domain.repository.TransactionRepository
import com.pwm.personalwealthmanager.domain.repository.ZerodhaRepository
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

    @Binds
    @Singleton
    abstract fun bindMetalsRepository(
        metalsRepositoryImpl: MetalsRepositoryImpl
    ): MetalsRepository

    @Binds
    @Singleton
    abstract fun bindMutualFundRepository(
        mutualFundRepositoryImpl: MutualFundRepositoryImpl
    ): MutualFundRepository

    @Binds
    @Singleton
    abstract fun bindPhysicalAssetRepository(
        physicalAssetRepositoryImpl: PhysicalAssetRepositoryImpl
    ): PhysicalAssetRepository

    @Binds
    @Singleton
    abstract fun bindLiabilityRepository(
        liabilityRepositoryImpl: LiabilityRepositoryImpl
    ): LiabilityRepository

    @Binds
    @Singleton
    abstract fun bindNetWorthRepository(
        netWorthRepositoryImpl: NetWorthRepositoryImpl
    ): NetWorthRepository

    @Binds
    @Singleton
    abstract fun bindMacroRepository(
        macroRepositoryImpl: MacroRepositoryImpl
    ): MacroRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository
}

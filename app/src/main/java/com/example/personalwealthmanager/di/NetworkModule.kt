package com.example.personalwealthmanager.di

import android.content.Context
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.api.AdminApi
import com.example.personalwealthmanager.data.remote.api.AuthApi
import com.example.personalwealthmanager.data.remote.api.HoldingsApi
import com.example.personalwealthmanager.data.remote.api.MetadataApi
import com.example.personalwealthmanager.data.remote.api.MetalsApi
import com.example.personalwealthmanager.data.remote.api.MutualFundApi
import com.example.personalwealthmanager.data.remote.api.PhysicalAssetApiService
import com.example.personalwealthmanager.data.remote.api.LiabilityApiService
import com.example.personalwealthmanager.data.remote.api.NetWorthApiService
import com.example.personalwealthmanager.data.remote.api.SmsApi
import com.example.personalwealthmanager.data.remote.api.TransactionApi
import com.example.personalwealthmanager.data.remote.api.ZerodhaApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://wealth-backend-demo.onrender.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val token = sessionManager.getSessionToken()

            // Add Authorization header if token exists and not a login/register request
            val newRequest = if (token != null &&
                !originalRequest.url.encodedPath.contains("/validate-login") &&
                !originalRequest.url.encodedPath.contains("/api/users") &&
                originalRequest.method != "POST") {
                originalRequest.newBuilder()
                    .header("Authorization", token)
                    .build()
            } else {
                originalRequest
            }

            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)  // Add auth interceptor first
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTransactionApi(retrofit: Retrofit): TransactionApi {
        return retrofit.create(TransactionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMetadataApi(retrofit: Retrofit): MetadataApi {
        return retrofit.create(MetadataApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSmsApi(retrofit: Retrofit): SmsApi {
        return retrofit.create(SmsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi {
        return retrofit.create(AdminApi::class.java)
    }

    @Provides
    @Singleton
    fun provideZerodhaApi(retrofit: Retrofit): ZerodhaApi {
        return retrofit.create(ZerodhaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideHoldingsApi(retrofit: Retrofit): HoldingsApi {
        return retrofit.create(HoldingsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMetalsApi(retrofit: Retrofit): MetalsApi {
        return retrofit.create(MetalsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMutualFundApi(retrofit: Retrofit): MutualFundApi {
        return retrofit.create(MutualFundApi::class.java)
    }

    @Provides
    @Singleton
    fun providePhysicalAssetApi(retrofit: Retrofit): PhysicalAssetApiService {
        return retrofit.create(PhysicalAssetApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLiabilityApi(retrofit: Retrofit): LiabilityApiService {
        return retrofit.create(LiabilityApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNetWorthApi(retrofit: Retrofit): NetWorthApiService {
        return retrofit.create(NetWorthApiService::class.java)
    }
}
package com.example.personalwealthmanager.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AdminApi {
    @POST("api/admin/sms-queue")
    suspend fun reportQueueCount(@Body body: QueueStatsRequest): Response<Unit>
}

data class QueueStatsRequest(
    @SerializedName("pending") val pending: Int
)

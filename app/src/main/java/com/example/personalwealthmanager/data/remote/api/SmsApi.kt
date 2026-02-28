package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.SmsParseRequest
import com.example.personalwealthmanager.data.remote.dto.SmsParseResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SmsApi {

    @POST("api/sms/parse")
    suspend fun parseSms(
        @Header("x-session-token") sessionToken: String,
        @Body request: SmsParseRequest
    ): Response<ApiResponse<SmsParseResult>>
}

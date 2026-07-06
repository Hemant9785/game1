package com.wordduel.app.data.network

import androidx.annotation.Keep
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ProxyDictionaryApi {
    @GET("validate")
    suspend fun validateWord(
        @Query("word") word: String
    ): Response<ProxyValidationDto>
}

@Keep
data class ProxyValidationDto(
    val word: String,
    val valid: Boolean,
    val source: String,
    val message: String? = null
)

package com.wordduel.app.data.network

import androidx.annotation.Keep
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DatamuseApi {
    @GET("words")
    suspend fun lookupWord(
        @Query("sp") spelling: String,
        @Query("qe") queryEcho: String = "sp",
        @Query("md") metadata: String = "d",
        @Query("max") max: Int = 1
    ): Response<List<DatamuseWordDto>>
}

@Keep
data class DatamuseWordDto(
    val word: String? = null,
    val defs: List<String>? = null
)

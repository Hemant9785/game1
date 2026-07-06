package com.wordduel.app.data.network

import androidx.annotation.Keep
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FreeDictionaryApi {
    @GET("entries/en/{word}")
    suspend fun validateWord(
        @Path("word") word: String
    ): Response<List<FreeDictionaryEntryDto>>
}

@Keep
data class FreeDictionaryEntryDto(
    val word: String? = null,
    val meanings: List<Any>? = null
)

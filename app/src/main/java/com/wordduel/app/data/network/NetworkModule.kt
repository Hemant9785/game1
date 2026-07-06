package com.wordduel.app.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class NetworkModule(
    val source: String,
    val freeDictionaryApi: FreeDictionaryApi,
    val proxyDictionaryApi: ProxyDictionaryApi,
    val datamuseApi: DatamuseApi
) {
    companion object {
        fun create(
            baseUrl: String,
            source: String,
            enableLogging: Boolean
        ): NetworkModule {
            val logging = HttpLoggingInterceptor().apply {
                level = if (enableLogging) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(baseUrl))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val datamuseRetrofit = Retrofit.Builder()
                .baseUrl("https://api.datamuse.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return NetworkModule(
                source = source,
                freeDictionaryApi = retrofit.create(FreeDictionaryApi::class.java),
                proxyDictionaryApi = retrofit.create(ProxyDictionaryApi::class.java),
                datamuseApi = datamuseRetrofit.create(DatamuseApi::class.java)
            )
        }

        private fun ensureTrailingSlash(url: String): String {
            return if (url.endsWith("/")) url else "$url/"
        }
    }
}

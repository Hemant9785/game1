package com.wordduel.app.data.repository

import com.wordduel.app.data.model.ValidationResult
import com.wordduel.app.data.network.NetworkModule
import com.wordduel.app.domain.GameWordRules
import java.io.IOException

class RetrofitWordValidationRepository(
    private val networkModule: NetworkModule
) : WordValidationRepository {

    override suspend fun validateWord(word: String): ValidationResult {
        val normalized = GameWordRules.normalizeWord(word)
        if (!GameWordRules.isAlphabeticWord(normalized)) {
            return ValidationResult(
                isValid = false,
                normalizedWord = normalized,
                source = "Local rule check",
                message = "Use letters only. Hyphenated words are allowed."
            )
        }

        return try {
            if (networkModule.source.equals("proxy", ignoreCase = true)) {
                val response = networkModule.proxyDictionaryApi.validateWord(normalized)
                if (response.isSuccessful) {
                    val body = response.body()
                    ValidationResult(
                        isValid = body?.valid == true,
                        normalizedWord = body?.word ?: normalized,
                        source = body?.source ?: "Proxy",
                        message = body?.message
                    )
                } else {
                    ValidationResult(
                        isValid = false,
                        normalizedWord = normalized,
                        source = "Proxy",
                        message = "Validation server returned ${response.code()}."
                    )
                }
            } else {
                val response = networkModule.freeDictionaryApi.validateWord(normalized)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    ValidationResult(
                        isValid = true,
                        normalizedWord = response.body()?.firstOrNull()?.word ?: normalized,
                        source = "Free Dictionary API"
                    )
                } else {
                    validateWithDatamuseFallback(normalized)
                }
            }
        } catch (_: IOException) {
            validateWithDatamuseFallback(normalized)
        } catch (error: Exception) {
            ValidationResult(
                isValid = false,
                normalizedWord = normalized,
                source = "Validation",
                message = error.message ?: "Unexpected validation error."
            )
        }
    }

    private suspend fun validateWithDatamuseFallback(word: String): ValidationResult {
        return try {
            val response = networkModule.datamuseApi.lookupWord(word)
            val body = response.body().orEmpty()
            val exactMatch = body.firstOrNull {
                it.word.equals(word, ignoreCase = true) && !it.defs.isNullOrEmpty()
            }

            if (response.isSuccessful && exactMatch != null) {
                ValidationResult(
                    isValid = true,
                    normalizedWord = exactMatch.word ?: word,
                    source = "Datamuse"
                )
            } else {
                ValidationResult(
                    isValid = false,
                    normalizedWord = word,
                    source = "Free Dictionary API / Datamuse",
                    message = "No free dictionary source accepted that word."
                )
            }
        } catch (_: IOException) {
            ValidationResult(
                isValid = false,
                normalizedWord = word,
                source = "Network",
                message = "Unable to reach the free validation services. Check the connection and try again."
            )
        }
    }
}

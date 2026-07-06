package com.wordduel.app.data.repository

import com.wordduel.app.data.model.ValidationResult

interface WordValidationRepository {
    suspend fun validateWord(word: String): ValidationResult
}

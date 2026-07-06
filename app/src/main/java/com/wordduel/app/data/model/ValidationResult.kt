package com.wordduel.app.data.model

data class ValidationResult(
    val isValid: Boolean,
    val normalizedWord: String,
    val source: String,
    val message: String? = null
)

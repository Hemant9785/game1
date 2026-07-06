package com.wordduel.app.domain

object GameWordRules {
    fun normalizeWord(input: String): String {
        return input.trim().lowercase()
    }

    fun isAlphabeticWord(word: String): Boolean {
        return word.matches(Regex("^[a-zA-Z-]+$"))
    }

    fun respectsBoundaryLetters(word: String, startLetter: Char, endLetter: Char): Boolean {
        val normalized = normalizeWord(word)
        if (normalized.isBlank()) return false
        return normalized.first() == startLetter.lowercaseChar() &&
            normalized.last() == endLetter.lowercaseChar()
    }
}

package com.wordduel.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameWordRulesTest {
    @Test
    fun respectsBoundaryLetters_acceptsMatchingWord() {
        assertThat(GameWordRules.respectsBoundaryLetters("Alpha", 'A', 'A')).isTrue()
    }

    @Test
    fun respectsBoundaryLetters_rejectsMismatchedWord() {
        assertThat(GameWordRules.respectsBoundaryLetters("planet", 'B', 'T')).isFalse()
    }

    @Test
    fun normalizeWord_trimsAndLowercases() {
        assertThat(GameWordRules.normalizeWord("  Camel  ")).isEqualTo("camel")
    }
}

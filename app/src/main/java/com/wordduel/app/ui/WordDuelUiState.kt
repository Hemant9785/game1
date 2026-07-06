package com.wordduel.app.ui

import com.wordduel.app.data.session.PlayerProfile
import com.wordduel.app.data.session.SessionSnapshot

enum class AppScreen {
    Home,
    Lobby,
    LetterPick,
    Round,
    Result
}

data class WordDuelUiState(
    val screen: AppScreen = AppScreen.Home,
    val profileNameDraft: String = "",
    val joinCodeDraft: String = "",
    val timerDraft: String = "60",
    val letterDraft: String = "",
    val wordDraft: String = "",
    val savedProfile: PlayerProfile? = null,
    val session: SessionSnapshot? = null,
    val infoBanner: String = "Create a private couple room and play from anywhere.",
    val sessionError: String? = null,
    val secondsRemaining: Int = 0,
    val isSubmittingWord: Boolean = false
)

package com.wordduel.app.data.session

enum class SessionPhase {
    Lobby,
    PickingLetters,
    Round,
    Result
}

data class PlayerProfile(
    val playerId: String,
    val displayName: String
)

data class SessionParticipant(
    val playerId: String,
    val displayName: String,
    val isHost: Boolean,
    val isReady: Boolean = false,
    val isConnected: Boolean = true
)

data class RoundSetup(
    val roundNumber: Int = 1,
    val firstLetterChooserId: String? = null,
    val lastLetterChooserId: String? = null,
    val firstLetter: Char? = null,
    val lastLetter: Char? = null,
    val lettersDeadlineAt: Long = 0L,
    val roundStartedAt: Long = 0L,
    val roundEndsAt: Long = 0L
)

data class RoundSubmission(
    val playerId: String,
    val displayName: String,
    val word: String = "",
    val submittedAt: Long = 0L,
    val isValid: Boolean = false,
    val source: String? = null,
    val message: String? = null
)

data class RoundResult(
    val winnerPlayerId: String? = null,
    val winnerName: String? = null,
    val winningWord: String? = null,
    val reason: String = ""
)

data class SessionSnapshot(
    val sessionCode: String,
    val phase: SessionPhase = SessionPhase.Lobby,
    val hostPlayerId: String,
    val timerSeconds: Int = 60,
    val participants: List<SessionParticipant> = emptyList(),
    val roundSetup: RoundSetup = RoundSetup(),
    val submissions: List<RoundSubmission> = emptyList(),
    val result: RoundResult? = null,
    val message: String = "Waiting for players."
)

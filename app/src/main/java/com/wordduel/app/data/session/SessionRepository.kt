package com.wordduel.app.data.session

import com.wordduel.app.data.model.ValidationResult
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun saveProfile(displayName: String): PlayerProfile
    suspend fun createSession(profile: PlayerProfile, timerSeconds: Int): SessionSnapshot
    suspend fun joinSession(sessionCode: String, profile: PlayerProfile): Result<SessionSnapshot>
    fun observeSession(sessionCode: String): Flow<SessionSnapshot?>
    suspend fun setReady(sessionCode: String, playerId: String, ready: Boolean): SessionSnapshot?
    suspend fun updateTimer(sessionCode: String, playerId: String, timerSeconds: Int): SessionSnapshot?
    suspend fun startNextRound(sessionCode: String, playerId: String): SessionSnapshot?
    suspend fun chooseLetter(
        sessionCode: String,
        playerId: String,
        chooseFirstLetter: Boolean,
        letter: Char
    ): SessionSnapshot?
    suspend fun submitWord(
        sessionCode: String,
        profile: PlayerProfile,
        word: String,
        validationResult: ValidationResult
    ): SessionSnapshot?
    suspend fun resolveRoundIfExpired(sessionCode: String, nowMillis: Long): SessionSnapshot?
    suspend fun leaveSession(sessionCode: String, playerId: String): SessionSnapshot?
}

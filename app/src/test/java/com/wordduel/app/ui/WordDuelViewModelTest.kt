package com.wordduel.app.ui

import com.google.common.truth.Truth.assertThat
import com.wordduel.app.data.model.ValidationResult
import com.wordduel.app.data.repository.WordValidationRepository
import com.wordduel.app.data.session.PlayerProfile
import com.wordduel.app.data.session.SessionPhase
import com.wordduel.app.data.session.SessionRepository
import com.wordduel.app.data.session.SessionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordDuelViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveProfile_requiresName() = runTest {
        val viewModel = WordDuelViewModel(
            repository = FakeWordValidationRepository(),
            sessionRepository = FakeSessionRepository()
        )

        viewModel.saveProfile()

        assertThat(viewModel.uiState.value.sessionError).isNotNull()
    }

    @Test
    fun createSession_movesToLobbyAfterProfileSaved() = runTest {
        val sessionRepository = FakeSessionRepository()
        val viewModel = WordDuelViewModel(
            repository = FakeWordValidationRepository(),
            sessionRepository = sessionRepository
        )

        viewModel.updateProfileName("Alex")
        viewModel.saveProfile()
        advanceUntilIdle()
        viewModel.createSession()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.savedProfile?.displayName).isEqualTo("Alex")
        assertThat(viewModel.uiState.value.screen).isEqualTo(AppScreen.Lobby)
        assertThat(viewModel.uiState.value.session?.phase).isEqualTo(SessionPhase.Lobby)
    }
}

private class FakeWordValidationRepository(
    private val result: ValidationResult = ValidationResult(
        isValid = true,
        normalizedWord = "arena",
        source = "Fake API"
    )
) : WordValidationRepository {
    override suspend fun validateWord(word: String): ValidationResult = result
}

private class FakeSessionRepository : SessionRepository {
    private val sessionFlow = MutableStateFlow<SessionSnapshot?>(null)
    private var savedProfile: PlayerProfile? = null

    override suspend fun saveProfile(displayName: String): PlayerProfile {
        return PlayerProfile(playerId = "player-1", displayName = displayName).also {
            savedProfile = it
        }
    }

    override suspend fun createSession(profile: PlayerProfile, timerSeconds: Int): SessionSnapshot {
        val snapshot = SessionSnapshot(
            sessionCode = "ABC123",
            phase = SessionPhase.Lobby,
            hostPlayerId = profile.playerId,
            timerSeconds = timerSeconds
        )
        sessionFlow.value = snapshot
        return snapshot
    }

    override suspend fun joinSession(sessionCode: String, profile: PlayerProfile): Result<SessionSnapshot> {
        return Result.success(
            SessionSnapshot(
                sessionCode = sessionCode,
                phase = SessionPhase.Lobby,
                hostPlayerId = profile.playerId,
                timerSeconds = 60
            ).also { sessionFlow.value = it }
        )
    }

    override fun observeSession(sessionCode: String): Flow<SessionSnapshot?> = sessionFlow

    override suspend fun setReady(sessionCode: String, playerId: String, ready: Boolean): SessionSnapshot? = sessionFlow.value

    override suspend fun updateTimer(sessionCode: String, playerId: String, timerSeconds: Int): SessionSnapshot? = sessionFlow.value

    override suspend fun startNextRound(sessionCode: String, playerId: String): SessionSnapshot? = sessionFlow.value

    override suspend fun chooseLetter(
        sessionCode: String,
        playerId: String,
        chooseFirstLetter: Boolean,
        letter: Char
    ): SessionSnapshot? = sessionFlow.value

    override suspend fun submitWord(
        sessionCode: String,
        profile: PlayerProfile,
        word: String,
        validationResult: ValidationResult
    ): SessionSnapshot? = sessionFlow.value

    override suspend fun resolveRoundIfExpired(sessionCode: String, nowMillis: Long): SessionSnapshot? = sessionFlow.value

    override suspend fun leaveSession(sessionCode: String, playerId: String): SessionSnapshot? = null
}

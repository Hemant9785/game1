package com.wordduel.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wordduel.app.data.repository.WordValidationRepository
import com.wordduel.app.data.session.FirebaseSessionRepository
import com.wordduel.app.data.session.PlayerProfile
import com.wordduel.app.data.session.SessionPhase
import com.wordduel.app.data.session.SessionRepository
import com.wordduel.app.data.session.SessionSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class WordDuelViewModel(
    private val repository: WordValidationRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordDuelUiState())
    val uiState: StateFlow<WordDuelUiState> = _uiState.asStateFlow()

    private var sessionObserverJob: Job? = null
    private var countdownJob: Job? = null

    fun updateProfileName(name: String) {
        _uiState.update { it.copy(profileNameDraft = name, sessionError = null) }
    }

    fun updateJoinCode(code: String) {
        _uiState.update { it.copy(joinCodeDraft = code.uppercase().take(6), sessionError = null) }
    }

    fun updateTimerDraft(value: String) {
        val digits = value.filter(Char::isDigit).take(3)
        _uiState.update { it.copy(timerDraft = digits, sessionError = null) }
    }

    fun updateLetterDraft(value: String) {
        _uiState.update { it.copy(letterDraft = value.take(1).uppercase(), sessionError = null) }
    }

    fun updateWordDraft(value: String) {
        _uiState.update { it.copy(wordDraft = value, sessionError = null) }
    }

    fun saveProfile() {
        val displayName = _uiState.value.profileNameDraft.trim()
        if (displayName.isBlank()) {
            _uiState.update { it.copy(sessionError = "Choose a username first.") }
            return
        }
        viewModelScope.launch {
            runCatching { sessionRepository.saveProfile(displayName) }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            savedProfile = profile,
                            infoBanner = "Profile saved. Create a room or join your partner with a code.",
                            sessionError = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(sessionError = error.message ?: "Unable to save profile.") }
                }
        }
    }

    fun createSession() {
        val profile = requireProfile() ?: return
        val timer = _uiState.value.timerDraft.toIntOrNull()?.coerceIn(30, 180) ?: 60
        viewModelScope.launch {
            runCatching { sessionRepository.createSession(profile, timer) }
                .onSuccess { snapshot ->
                    observeSession(snapshot.sessionCode)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(sessionError = error.message ?: "Unable to create a session.") }
                }
        }
    }

    fun joinSession() {
        val profile = requireProfile() ?: return
        val code = _uiState.value.joinCodeDraft.trim()
        if (code.length < 4) {
            _uiState.update { it.copy(sessionError = "Enter a valid session code.") }
            return
        }
        viewModelScope.launch {
            sessionRepository.joinSession(code, profile)
                .onSuccess { snapshot -> observeSession(snapshot.sessionCode) }
                .onFailure { error ->
                    _uiState.update { it.copy(sessionError = error.message ?: "Unable to join session.") }
                }
        }
    }

    fun toggleReady() {
        val profile = _uiState.value.savedProfile ?: return
        val session = _uiState.value.session ?: return
        val participant = session.participants.firstOrNull { it.playerId == profile.playerId } ?: return
        viewModelScope.launch {
            sessionRepository.setReady(session.sessionCode, profile.playerId, !participant.isReady)
        }
    }

    fun saveTimer() {
        val profile = _uiState.value.savedProfile ?: return
        val session = _uiState.value.session ?: return
        val timer = _uiState.value.timerDraft.toIntOrNull()
        if (timer == null || timer !in 30..180) {
            _uiState.update { it.copy(sessionError = "Timer must be between 30 and 180 seconds.") }
            return
        }
        viewModelScope.launch {
            sessionRepository.updateTimer(session.sessionCode, profile.playerId, timer)
        }
    }

    fun startNextRound() {
        val profile = _uiState.value.savedProfile ?: return
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            sessionRepository.startNextRound(session.sessionCode, profile.playerId)
        }
    }

    fun chooseAssignedLetter(chooseFirstLetter: Boolean) {
        val profile = _uiState.value.savedProfile ?: return
        val session = _uiState.value.session ?: return
        val letter = _uiState.value.letterDraft.firstOrNull()
        if (letter == null || !letter.isLetter()) {
            _uiState.update { it.copy(sessionError = "Choose one valid letter.") }
            return
        }
        viewModelScope.launch {
            sessionRepository.chooseLetter(session.sessionCode, profile.playerId, chooseFirstLetter, letter)
            _uiState.update { it.copy(letterDraft = "") }
        }
    }

    fun submitWord() {
        val profile = _uiState.value.savedProfile ?: return
        val session = _uiState.value.session ?: return
        val word = _uiState.value.wordDraft.trim()
        if (word.isBlank()) {
            _uiState.update { it.copy(sessionError = "Enter a word before submitting.") }
            return
        }
        _uiState.update { it.copy(isSubmittingWord = true, sessionError = null) }
        viewModelScope.launch {
            val result = repository.validateWord(word)
            sessionRepository.submitWord(session.sessionCode, profile, word, result)
            _uiState.update { it.copy(isSubmittingWord = false, wordDraft = "") }
        }
    }

    fun leaveSession() {
        val profile = _uiState.value.savedProfile ?: return
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            sessionRepository.leaveSession(session.sessionCode, profile.playerId)
            resetToHome("You left the session.")
        }
    }

    private fun observeSession(sessionCode: String) {
        sessionObserverJob?.cancel()
        sessionObserverJob = viewModelScope.launch {
            sessionRepository.observeSession(sessionCode).collectLatest { snapshot ->
                if (snapshot == null) {
                    resetToHome("Session ended.")
                    return@collectLatest
                }
                applySnapshot(snapshot)
            }
        }
    }

    private fun applySnapshot(snapshot: SessionSnapshot) {
        _uiState.update { state ->
            state.copy(
                session = snapshot,
                screen = snapshot.phase.toScreen(),
                infoBanner = snapshot.message,
                timerDraft = snapshot.timerSeconds.toString(),
                letterDraft = if (snapshot.phase == SessionPhase.PickingLetters) state.letterDraft else "",
                wordDraft = if (snapshot.phase == SessionPhase.Round) state.wordDraft else "",
                sessionError = null
            )
        }
        when (snapshot.phase) {
            SessionPhase.Lobby -> stopCountdown()
            SessionPhase.PickingLetters -> startCountdown(snapshot.roundSetup.lettersDeadlineAt, snapshot)
            SessionPhase.Round -> startCountdown(snapshot.roundSetup.roundEndsAt, snapshot)
            SessionPhase.Result -> stopCountdown()
        }
    }

    private fun startCountdown(deadlineMillis: Long, snapshot: SessionSnapshot) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val remaining = max(0L, (deadlineMillis - System.currentTimeMillis() + 999L) / 1000L).toInt()
                _uiState.update { it.copy(secondsRemaining = remaining) }
                if (remaining <= 0) {
                    if (snapshot.phase == SessionPhase.Round || snapshot.phase == SessionPhase.PickingLetters) {
                        sessionRepository.resolveRoundIfExpired(snapshot.sessionCode, System.currentTimeMillis())
                    }
                    break
                }
                delay(1_000)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(secondsRemaining = 0) }
    }

    private fun requireProfile(): PlayerProfile? {
        val profile = _uiState.value.savedProfile
        if (profile == null) {
            _uiState.update { it.copy(sessionError = "Save your username first.") }
        }
        return profile
    }

    private fun resetToHome(message: String) {
        countdownJob?.cancel()
        sessionObserverJob?.cancel()
        _uiState.update {
            it.copy(
                screen = AppScreen.Home,
                session = null,
                joinCodeDraft = "",
                letterDraft = "",
                wordDraft = "",
                secondsRemaining = 0,
                infoBanner = message,
                sessionError = null,
                isSubmittingWord = false
            )
        }
    }
}

class WordDuelViewModelFactory(
    private val repository: WordValidationRepository,
    private val sessionRepository: SessionRepository = FirebaseSessionRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordDuelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordDuelViewModel(repository, sessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun SessionPhase.toScreen(): AppScreen {
    return when (this) {
        SessionPhase.Lobby -> AppScreen.Lobby
        SessionPhase.PickingLetters -> AppScreen.LetterPick
        SessionPhase.Round -> AppScreen.Round
        SessionPhase.Result -> AppScreen.Result
    }
}

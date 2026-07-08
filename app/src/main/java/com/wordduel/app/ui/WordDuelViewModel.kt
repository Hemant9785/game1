package com.wordduel.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wordduel.app.data.profile.LocalProfileState
import com.wordduel.app.data.profile.ProfilePreferencesRepository
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
    private val sessionRepository: SessionRepository,
    private val profilePreferencesRepository: ProfilePreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordDuelUiState())
    val uiState: StateFlow<WordDuelUiState> = _uiState.asStateFlow()

    private var sessionObserverJob: Job? = null
    private var countdownJob: Job? = null

    init {
        observeLocalProfile()
    }

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

    fun nextOnboardingPage() {
        val current = _uiState.value.onboardingPage
        if (current < 2) {
            _uiState.update { it.copy(onboardingPage = current + 1) }
        } else {
            saveProfile(fromOnboarding = true)
        }
    }

    fun previousOnboardingPage() {
        _uiState.update { it.copy(onboardingPage = max(0, it.onboardingPage - 1)) }
    }

    fun skipOnboarding() {
        _uiState.update {
            it.copy(
                onboardingPage = 2,
                screen = AppScreen.Onboarding,
                sessionError = null
            )
        }
    }

    fun saveProfile(fromOnboarding: Boolean = false) {
        val displayName = _uiState.value.profileNameDraft.trim()
        if (displayName.isBlank()) {
            _uiState.update { it.copy(sessionError = "Choose a player name first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, sessionError = null) }
            runCatching {
                profilePreferencesRepository.savePlayerName(displayName)
                sessionRepository.saveProfile(displayName)
            }.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        savedProfile = profile,
                        screen = AppScreen.Home,
                        infoBanner = "Welcome back, ${profile.displayName}.",
                        sessionError = null,
                        isBusy = false,
                        isInitializing = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        sessionError = error.message ?: "Unable to save profile.",
                        isBusy = false,
                        screen = if (fromOnboarding) AppScreen.Onboarding else it.screen
                    )
                }
            }
        }
    }

    fun openCreateGame() {
        _uiState.update { it.copy(screen = AppScreen.CreateGame, sessionError = null) }
    }

    fun openJoinGame() {
        _uiState.update { it.copy(screen = AppScreen.JoinGame, sessionError = null) }
    }

    fun openSettings() {
        _uiState.update { it.copy(screen = AppScreen.Settings, sessionError = null) }
    }

    fun backToHome() {
        _uiState.update { it.copy(screen = AppScreen.Home, sessionError = null) }
    }

    fun createSession() {
        val profile = requireProfile() ?: return
        val timer = _uiState.value.timerDraft.toIntOrNull()?.coerceIn(30, 180) ?: 60
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, sessionError = null) }
            runCatching { sessionRepository.createSession(profile, timer) }
                .onSuccess { snapshot ->
                    _uiState.update { it.copy(isBusy = false) }
                    observeSession(snapshot.sessionCode)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            sessionError = error.message ?: "Unable to create a session."
                        )
                    }
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
            _uiState.update { it.copy(isBusy = true, sessionError = null) }
            sessionRepository.joinSession(code, profile)
                .onSuccess { snapshot ->
                    _uiState.update { it.copy(isBusy = false) }
                    observeSession(snapshot.sessionCode)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            sessionError = error.message ?: "Unable to join session."
                        )
                    }
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

    fun resetOnboarding() {
        viewModelScope.launch {
            profilePreferencesRepository.resetOnboarding()
            _uiState.update {
                it.copy(
                    screen = AppScreen.Onboarding,
                    onboardingPage = 0,
                    infoBanner = "Onboarding reset. You can walk through the intro again."
                )
            }
        }
    }

    fun resetLocalProfile() {
        viewModelScope.launch {
            sessionObserverJob?.cancel()
            countdownJob?.cancel()
            runCatching {
                profilePreferencesRepository.clearProfile()
                sessionRepository.signOut()
            }.onSuccess {
                _uiState.value = WordDuelUiState(
                    screen = AppScreen.Onboarding,
                    onboardingPage = 0,
                    isInitializing = false,
                    infoBanner = "Profile cleared. Start fresh."
                )
            }.onFailure { error ->
                _uiState.update { it.copy(sessionError = error.message ?: "Unable to clear profile.") }
            }
        }
    }

    private fun observeLocalProfile() {
        viewModelScope.launch {
            profilePreferencesRepository.profileState.collectLatest { localState ->
                hydrateFromLocalState(localState)
            }
        }
    }

    private suspend fun hydrateFromLocalState(localState: LocalProfileState) {
        if (localState.playerName.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    isBusy = false,
                    profileNameDraft = "",
                    savedProfile = null,
                    onboardingPage = if (localState.hasCompletedOnboarding) 2 else 0,
                    screen = AppScreen.Onboarding
                )
            }
            return
        }

        if (_uiState.value.savedProfile?.displayName == localState.playerName && !_uiState.value.isInitializing) {
            return
        }

        _uiState.update {
            it.copy(
                isInitializing = true,
                isBusy = true,
                profileNameDraft = localState.playerName
            )
        }

        runCatching { sessionRepository.saveProfile(localState.playerName) }
            .onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        savedProfile = profile,
                        screen = AppScreen.Home,
                        isInitializing = false,
                        isBusy = false,
                        infoBanner = "Welcome back, ${profile.displayName}.",
                        sessionError = null
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        onboardingPage = if (localState.hasCompletedOnboarding) 2 else 0,
                        screen = AppScreen.Onboarding,
                        isInitializing = false,
                        isBusy = false,
                        sessionError = error.message ?: "Unable to restore profile."
                    )
                }
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
                sessionError = null,
                isBusy = false,
                isInitializing = false
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
            _uiState.update { it.copy(sessionError = "Save your player name first.") }
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
                isSubmittingWord = false,
                isBusy = false
            )
        }
    }
}

class WordDuelViewModelFactory(
    private val repository: WordValidationRepository,
    private val sessionRepository: SessionRepository = FirebaseSessionRepository(),
    private val profilePreferencesRepository: ProfilePreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordDuelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordDuelViewModel(
                repository = repository,
                sessionRepository = sessionRepository,
                profilePreferencesRepository = profilePreferencesRepository
            ) as T
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

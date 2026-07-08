package com.wordduel.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordduel.app.data.session.RoundSubmission
import com.wordduel.app.data.session.SessionParticipant
import com.wordduel.app.ui.theme.AccentCoral
import com.wordduel.app.ui.theme.AccentGold
import com.wordduel.app.ui.theme.AccentMint
import com.wordduel.app.ui.theme.AccentRed
import com.wordduel.app.ui.theme.BorderBlush
import com.wordduel.app.ui.theme.CherryCream
import com.wordduel.app.ui.theme.CodeLavender
import com.wordduel.app.ui.theme.InkBlue
import com.wordduel.app.ui.theme.MidnightPlum
import com.wordduel.app.ui.theme.SoftLilac

@Composable
fun WordDuelApp(viewModel: WordDuelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.session != null && uiState.screen !in listOf(AppScreen.Home, AppScreen.Onboarding, AppScreen.Loading)) {
        BackHandler(onBack = viewModel::leaveSession)
    } else if (uiState.screen in listOf(AppScreen.CreateGame, AppScreen.JoinGame, AppScreen.Settings)) {
        BackHandler(onBack = viewModel::backToHome)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (uiState.screen) {
            AppScreen.Loading -> LoadingScreen(uiState)
            AppScreen.Onboarding -> OnboardingScreen(
                uiState = uiState,
                onNameChanged = viewModel::updateProfileName,
                onNext = viewModel::nextOnboardingPage,
                onBack = viewModel::previousOnboardingPage,
                onSkip = viewModel::skipOnboarding
            )
            AppScreen.Home -> HomeScreen(
                uiState = uiState,
                onCreateGame = viewModel::openCreateGame,
                onJoinGame = viewModel::openJoinGame,
                onOpenSettings = viewModel::openSettings
            )
            AppScreen.CreateGame -> CreateGameScreen(
                uiState = uiState,
                onTimerChanged = viewModel::updateTimerDraft,
                onCreateSession = viewModel::createSession,
                onBack = viewModel::backToHome
            )
            AppScreen.JoinGame -> JoinGameScreen(
                uiState = uiState,
                onJoinCodeChanged = viewModel::updateJoinCode,
                onJoinSession = viewModel::joinSession,
                onBack = viewModel::backToHome
            )
            AppScreen.Settings -> SettingsScreen(
                uiState = uiState,
                onNameChanged = viewModel::updateProfileName,
                onSaveProfile = { viewModel.saveProfile(fromOnboarding = false) },
                onResetOnboarding = viewModel::resetOnboarding,
                onResetProfile = viewModel::resetLocalProfile,
                onBack = viewModel::backToHome
            )
            AppScreen.Lobby -> LobbyScreen(
                uiState = uiState,
                onTimerChanged = viewModel::updateTimerDraft,
                onSaveTimer = viewModel::saveTimer,
                onToggleReady = viewModel::toggleReady,
                onStartRound = viewModel::startNextRound,
                onLeave = viewModel::leaveSession
            )
            AppScreen.LetterPick -> LetterPickScreen(
                uiState = uiState,
                onLetterChanged = viewModel::updateLetterDraft,
                onChooseFirst = { viewModel.chooseAssignedLetter(true) },
                onChooseLast = { viewModel.chooseAssignedLetter(false) }
            )
            AppScreen.Round -> RoundScreen(
                uiState = uiState,
                onWordChanged = viewModel::updateWordDraft,
                onSubmit = viewModel::submitWord
            )
            AppScreen.Result -> ResultScreen(
                uiState = uiState,
                onNextRound = viewModel::startNextRound,
                onLeave = viewModel::leaveSession
            )
        }
    }
}

@Composable
private fun LoadingScreen(uiState: WordDuelUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CodeLavender, CherryCream)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = InkBlue)
            Text("Preparing your game room...", style = MaterialTheme.typography.bodyLarge, color = InkBlue)
            if (uiState.sessionError != null) {
                Text(uiState.sessionError, style = MaterialTheme.typography.bodyMedium, color = AccentRed)
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    uiState: WordDuelUiState,
    onNameChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val pages = listOf(
        "Choose letters together" to "Each round gives one partner the first letter and the other the last. The word lives in the space between both of you.",
        "Race in real time" to "You are both in the same live room, watching the same timer, feeling the same pressure.",
        "Finish the setup" to "Pick the name your partner will see every time you join a room."
    )
    val page = pages[uiState.onboardingPage]

    ScreenFrame {
        HeroSection(
            eyebrow = "Welcome",
            title = page.first,
            subtitle = page.second
        )
        ProgressDots(current = uiState.onboardingPage, total = pages.size)
        Spacer(modifier = Modifier.height(18.dp))
        ErrorText(uiState.sessionError)

        if (uiState.onboardingPage == 2) {
            ElevatedPanel {
                SectionHeader(
                    title = "Your player name",
                    subtitle = "Keep it simple. This becomes your default identity across sessions."
                )
                OutlinedTextField(
                    value = uiState.profileNameDraft,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Player name") },
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (uiState.onboardingPage > 0) {
                OutlinedAction(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
            }
            PrimaryAction(
                text = if (uiState.onboardingPage == 2) {
                    if (uiState.isBusy) "Saving..." else "Enter game"
                } else {
                    "Next"
                },
                onClick = onNext,
                enabled = !uiState.isBusy,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedAction(text = "Skip", onClick = onSkip)
    }
}

@Composable
private fun HomeScreen(
    uiState: WordDuelUiState,
    onCreateGame: () -> Unit,
    onJoinGame: () -> Unit,
    onOpenSettings: () -> Unit
) {
    ScreenFrame {
        HeroSection(
            eyebrow = "Ready to play",
            title = "Word Duel",
            subtitle = uiState.savedProfile?.displayName?.let {
                "Signed in as $it. Start a room or jump into your partner's code."
            } ?: "Save a profile to start."
        )
        InfoText(uiState.infoBanner)
        ErrorText(uiState.sessionError)

        ElevatedPanel {
            PrimaryAction(text = "Create Game", onClick = onCreateGame)
            OutlinedAction(text = "Join Game", onClick = onJoinGame)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextAction(text = "Settings / Profile", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
private fun CreateGameScreen(
    uiState: WordDuelUiState,
    onTimerChanged: (String) -> Unit,
    onCreateSession: () -> Unit,
    onBack: () -> Unit
) {
    ScreenFrame {
        HeroSection(
            eyebrow = "Create room",
            title = "Start a private session",
            subtitle = "Set the round timer once, then generate a code and wait for your partner in the room."
        )
        InfoText(uiState.infoBanner)
        ErrorText(uiState.sessionError)
        ElevatedPanel {
            SectionHeader(
                title = "Round timer",
                subtitle = "Default is 60 seconds. Keep it fast for more tension."
            )
            OutlinedTextField(
                value = uiState.timerDraft,
                onValueChange = onTimerChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Seconds") },
                singleLine = true
            )
            PrimaryAction(
                text = if (uiState.isBusy) "Creating..." else "Generate session code",
                onClick = onCreateSession,
                enabled = !uiState.isBusy
            )
            OutlinedAction(text = "Back", onClick = onBack)
        }
    }
}

@Composable
private fun JoinGameScreen(
    uiState: WordDuelUiState,
    onJoinCodeChanged: (String) -> Unit,
    onJoinSession: () -> Unit,
    onBack: () -> Unit
) {
    ScreenFrame {
        HeroSection(
            eyebrow = "Join room",
            title = "Enter a session code",
            subtitle = "Use the exact code your partner shared. This screen stays minimal so the task is obvious."
        )
        InfoText(uiState.infoBanner)
        ErrorText(uiState.sessionError)
        ElevatedPanel {
            OutlinedTextField(
                value = uiState.joinCodeDraft,
                onValueChange = onJoinCodeChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Session code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
            PrimaryAction(
                text = if (uiState.isBusy) "Joining..." else "Join Game",
                onClick = onJoinSession,
                enabled = !uiState.isBusy
            )
            OutlinedAction(text = "Back", onClick = onBack)
        }
    }
}

@Composable
private fun SettingsScreen(
    uiState: WordDuelUiState,
    onNameChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onResetOnboarding: () -> Unit,
    onResetProfile: () -> Unit,
    onBack: () -> Unit
) {
    ScreenFrame {
        HeroSection(
            eyebrow = "Settings",
            title = "Profile and reset tools",
            subtitle = "Keep the profile editable, but leave the game rules untouched."
        )
        InfoText(uiState.infoBanner)
        ErrorText(uiState.sessionError)
        ElevatedPanel {
            SectionHeader(
                title = "Player name",
                subtitle = "Update the name shown in your rooms."
            )
            OutlinedTextField(
                value = uiState.profileNameDraft,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Player name") },
                singleLine = true
            )
            PrimaryAction(
                text = if (uiState.isBusy) "Saving..." else "Save changes",
                onClick = onSaveProfile,
                enabled = !uiState.isBusy
            )
            OutlinedAction(text = "Reset onboarding", onClick = onResetOnboarding)
            OutlinedAction(text = "Logout / reset local profile", onClick = onResetProfile)
            TinyAction(text = "Back", onClick = onBack)
        }
    }
}

@Composable
private fun LobbyScreen(
    uiState: WordDuelUiState,
    onTimerChanged: (String) -> Unit,
    onSaveTimer: () -> Unit,
    onToggleReady: () -> Unit,
    onStartRound: () -> Unit,
    onLeave: () -> Unit
) {
    val session = uiState.session ?: return
    val me = session.participants.firstOrNull { it.playerId == uiState.savedProfile?.playerId }
    val isHost = me?.isHost == true
    val bothReady = session.participants.size == 2 && session.participants.all { it.isReady }
    val clipboard = LocalClipboardManager.current

    ScreenFrame {
        HeroSection(
            eyebrow = "Waiting room",
            title = "Session ${session.sessionCode}",
            subtitle = "Minimal room, clear status, one obvious next step."
        )
        InfoText(uiState.infoBanner)
        ErrorText(uiState.sessionError)

        ElevatedPanel(containerColor = MidnightPlum, contentColor = CherryCream) {
            SectionHeader(
                title = "Share code",
                subtitle = "Send the code, then wait until both players show ready.",
                titleColor = CherryCream,
                subtitleColor = CherryCream.copy(alpha = 0.72f)
            )
            SessionCodeCard(session.sessionCode, compact = true)
            OutlinedAction(
                text = "Copy code",
                onClick = { clipboard.setText(AnnotatedString(session.sessionCode)) },
                borderColor = CherryCream.copy(alpha = 0.45f),
                textColor = CherryCream
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        ElevatedPanel {
            SectionHeader(
                title = "Players",
                subtitle = "Only the essential room presence is visible here."
            )
            session.participants.forEach { ParticipantRow(it) }
        }

        Spacer(modifier = Modifier.height(18.dp))

        ElevatedPanel {
            SectionHeader(
                title = "Round controls",
                subtitle = if (isHost) "You can update the timer and open the next round." else "The host will start the round once both of you are ready."
            )
            OutlinedTextField(
                value = uiState.timerDraft,
                onValueChange = onTimerChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Round timer in seconds") },
                singleLine = true,
                enabled = isHost
            )
            if (isHost) {
                OutlinedAction(text = "Save timer", onClick = onSaveTimer)
            }
            PrimaryAction(
                text = if (me?.isReady == true) "Unready" else "I'm ready",
                onClick = onToggleReady
            )
            PrimaryAction(
                text = if (bothReady) "Start round" else "Waiting for both players",
                onClick = onStartRound,
                enabled = bothReady && isHost
            )
            TinyAction(text = "Leave session", onClick = onLeave)
        }
    }
}

@Composable
private fun LetterPickScreen(
    uiState: WordDuelUiState,
    onLetterChanged: (String) -> Unit,
    onChooseFirst: () -> Unit,
    onChooseLast: () -> Unit
) {
    val session = uiState.session ?: return
    val me = uiState.savedProfile ?: return
    val isFirstChooser = session.roundSetup.firstLetterChooserId == me.playerId
    val isLastChooser = session.roundSetup.lastLetterChooserId == me.playerId

    ScreenFrame {
        HeroSection(
            eyebrow = "Letter phase",
            title = "Lock the round",
            subtitle = "Only the letters and the countdown matter here."
        )
        InfoText(uiState.infoBanner)
        Banner("${uiState.secondsRemaining}s left")
        ErrorText(uiState.sessionError)
        HighlightCard(
            headline = "${session.roundSetup.firstLetter ?: "_"} ... ${session.roundSetup.lastLetter ?: "_"}",
            supporting = session.message
        )
        Spacer(modifier = Modifier.height(18.dp))
        ElevatedPanel {
            OutlinedTextField(
                value = uiState.letterDraft,
                onValueChange = onLetterChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Single letter") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
            PrimaryAction(
                text = if (isFirstChooser) "Lock first letter" else "Partner owns first letter",
                onClick = onChooseFirst,
                enabled = isFirstChooser && session.roundSetup.firstLetter == null
            )
            PrimaryAction(
                text = if (isLastChooser) "Lock last letter" else "Partner owns last letter",
                onClick = onChooseLast,
                enabled = isLastChooser && session.roundSetup.lastLetter == null
            )
        }
    }
}

@Composable
private fun RoundScreen(
    uiState: WordDuelUiState,
    onWordChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val session = uiState.session ?: return
    val myId = uiState.savedProfile?.playerId
    val mySubmission = session.submissions.firstOrNull { it.playerId == myId }

    ScreenFrame {
        HeroSection(
            eyebrow = "Live round",
            title = "${session.roundSetup.firstLetter ?: "_"} ... ${session.roundSetup.lastLetter ?: "_"}",
            subtitle = "Timer, letters, input, submit state. Nothing extra."
        )
        InfoText(uiState.infoBanner)
        Banner("${uiState.secondsRemaining}s remaining")
        ErrorText(uiState.sessionError)

        ElevatedPanel {
            OutlinedTextField(
                value = uiState.wordDraft,
                onValueChange = onWordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your word") },
                singleLine = true,
                enabled = mySubmission == null
            )
            AnimatedVisibility(visible = uiState.isSubmittingWord, enter = fadeIn(), exit = fadeOut()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Validating...")
                }
            }
            PrimaryAction(
                text = if (mySubmission == null) "Submit" else "Submitted",
                onClick = onSubmit,
                enabled = !uiState.isSubmittingWord && mySubmission == null
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        ElevatedPanel(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            SectionHeader(
                title = "Player status",
                subtitle = "See only who is done and who is still thinking."
            )
            session.participants.forEach { participant ->
                SubmissionRow(
                    participant = participant,
                    submission = session.submissions.firstOrNull { it.playerId == participant.playerId }
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(
    uiState: WordDuelUiState,
    onNextRound: () -> Unit,
    onLeave: () -> Unit
) {
    val session = uiState.session ?: return
    val me = uiState.savedProfile
    val isHost = session.hostPlayerId == me?.playerId

    ScreenFrame {
        HeroSection(
            eyebrow = "Round result",
            title = session.result?.winnerName?.let { "$it wins" } ?: "Round complete",
            subtitle = session.result?.reason ?: "The round has ended."
        )
        InfoText(uiState.infoBanner)
        ErrorText(uiState.sessionError)
        HighlightCard(
            headline = session.result?.winningWord ?: "No winning word",
            supporting = "Letters: ${session.roundSetup.firstLetter ?: "_"} ... ${session.roundSetup.lastLetter ?: "_"}"
        )
        Spacer(modifier = Modifier.height(18.dp))
        ElevatedPanel {
            SectionHeader(
                title = "Submissions",
                subtitle = "A simple recap of what both players entered."
            )
            if (session.submissions.isEmpty()) {
                EmptyState("No submissions were recorded for this round.")
            } else {
                session.submissions.forEach { submission ->
                    ResultSubmissionRow(submission)
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        if (isHost) {
            PrimaryAction(text = "Start another round", onClick = onNextRound)
            Spacer(modifier = Modifier.height(12.dp))
        }
        TinyAction(text = "Leave session", onClick = onLeave)
    }
}

@Composable
private fun HeroSection(eyebrow: String, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(colors = listOf(CodeLavender, SoftLilac, CherryCream)))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusPill(text = eyebrow, background = InkBlue, textColor = CherryCream)
            Text(title, style = MaterialTheme.typography.displaySmall, color = InkBlue)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = InkBlue.copy(alpha = 0.82f))
        }
    }
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == current) 26.dp else 10.dp, 10.dp)
                    .clip(CircleShape)
                    .background(if (index == current) InkBlue else BorderBlush)
            )
        }
    }
}

@Composable
private fun ElevatedPanel(
    containerColor: Color = CherryCream,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, BorderBlush.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = titleColor)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
}

@Composable
private fun HighlightCard(headline: String, supporting: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPlum),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(headline, style = MaterialTheme.typography.displayMedium, color = CherryCream)
            Text(supporting, style = MaterialTheme.typography.bodyLarge, color = CherryCream.copy(alpha = 0.82f))
        }
    }
}

@Composable
private fun SessionCodeCard(code: String, compact: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = InkBlue),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Session code", style = MaterialTheme.typography.bodyMedium, color = CherryCream.copy(alpha = 0.75f))
            Text(
                code,
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displayMedium,
                color = AccentGold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = InkBlue,
            contentColor = CherryCream,
            disabledContainerColor = InkBlue.copy(alpha = 0.35f),
            disabledContentColor = CherryCream.copy(alpha = 0.7f)
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun OutlinedAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = InkBlue.copy(alpha = 0.25f),
    textColor: Color = InkBlue,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TinyAction(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentCoral,
            contentColor = CherryCream
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TextAction(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = InkBlue)
    }
}

@Composable
private fun Banner(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = AccentGold), shape = MaterialTheme.shapes.medium) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = InkBlue
        )
    }
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun InfoText(text: String?) {
    if (text.isNullOrBlank()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftLilac.copy(alpha = 0.62f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MidnightPlum
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun StatusPill(text: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}

@Composable
private fun ErrorText(text: String?) {
    AnimatedVisibility(visible = text != null, enter = fadeIn(), exit = fadeOut()) {
        if (text != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text,
                    modifier = Modifier.padding(14.dp),
                    color = AccentRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderBlush, MaterialTheme.shapes.medium)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
    }
}

@Composable
private fun ParticipantRow(participant: SessionParticipant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CherryCream.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = participant.displayName + if (participant.isHost) " - Host" else "",
                color = CherryCream,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (participant.isConnected) "Connected" else "Disconnected",
                color = CherryCream.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        StatusPill(
            text = if (participant.isReady) "Ready" else "Waiting",
            background = if (participant.isReady) AccentMint else CherryCream.copy(alpha = 0.16f),
            textColor = if (participant.isReady) InkBlue else CherryCream
        )
    }
}

@Composable
private fun SubmissionRow(participant: SessionParticipant, submission: RoundSubmission?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CherryCream.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(participant.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = submission?.word?.takeIf { it.isNotBlank() } ?: "No revealed word yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
        StatusPill(
            text = when {
                submission == null -> "Thinking"
                submission.isValid -> "Valid"
                else -> "Invalid"
            },
            background = when {
                submission == null -> SoftLilac
                submission.isValid -> AccentMint
                else -> AccentRed.copy(alpha = 0.2f)
            },
            textColor = when {
                submission == null -> MidnightPlum
                submission.isValid -> InkBlue
                else -> AccentRed
            }
        )
    }
}

@Composable
private fun ResultSubmissionRow(submission: RoundSubmission) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CherryCream),
        border = BorderStroke(1.dp, BorderBlush)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(submission.displayName, style = MaterialTheme.typography.titleMedium)
                StatusPill(
                    text = if (submission.isValid) "Accepted" else "Rejected",
                    background = if (submission.isValid) AccentMint else AccentRed.copy(alpha = 0.2f),
                    textColor = if (submission.isValid) InkBlue else AccentRed
                )
            }
            Text(
                text = if (submission.word.isBlank()) "No word submitted" else submission.word,
                style = MaterialTheme.typography.headlineSmall,
                color = MidnightPlum
            )
            Text(
                text = if (submission.isValid) {
                    "Validated by ${submission.source ?: "dictionary"}"
                } else {
                    submission.message ?: "Invalid word"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun ScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.Top,
        content = content
    )
}

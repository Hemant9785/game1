package com.wordduel.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordduel.app.data.session.RoundSubmission
import com.wordduel.app.data.session.SessionParticipant
import com.wordduel.app.ui.theme.AccentGold
import com.wordduel.app.ui.theme.AccentRed
import com.wordduel.app.ui.theme.PanelCream
import com.wordduel.app.ui.theme.PanelNavy

@Composable
fun WordDuelApp(viewModel: WordDuelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.screen != AppScreen.Home && uiState.session != null) {
        BackHandler(onBack = viewModel::leaveSession)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (uiState.screen) {
            AppScreen.Home -> HomeScreen(
                uiState = uiState,
                onProfileNameChanged = viewModel::updateProfileName,
                onJoinCodeChanged = viewModel::updateJoinCode,
                onTimerChanged = viewModel::updateTimerDraft,
                onSaveProfile = viewModel::saveProfile,
                onCreateSession = viewModel::createSession,
                onJoinSession = viewModel::joinSession
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
private fun HomeScreen(
    uiState: WordDuelUiState,
    onProfileNameChanged: (String) -> Unit,
    onJoinCodeChanged: (String) -> Unit,
    onTimerChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onCreateSession: () -> Unit,
    onJoinSession: () -> Unit
) {
    ScreenFrame {
        TitleBlock(
            title = "Word Duel",
            subtitle = "A live word game for long-distance couples. Build a private room, stay connected, and race to the better word."
        )
        Banner(uiState.infoBanner)
        ErrorText(uiState.sessionError)

        Card(colors = CardDefaults.cardColors(containerColor = PanelCream)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = uiState.profileNameDraft,
                    onValueChange = onProfileNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your username") },
                    singleLine = true
                )
                Button(onClick = onSaveProfile, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.savedProfile == null) "Save profile" else "Update profile")
                }
                if (uiState.savedProfile != null) {
                    Text(
                        text = "Signed in as ${uiState.savedProfile.displayName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Create private room", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = uiState.timerDraft,
                    onValueChange = onTimerChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Round timer in seconds") },
                    singleLine = true
                )
                Button(onClick = onCreateSession, modifier = Modifier.fillMaxWidth()) {
                    Text("Create session")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Join your partner", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = uiState.joinCodeDraft,
                    onValueChange = onJoinCodeChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Session code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                Button(onClick = onJoinSession, modifier = Modifier.fillMaxWidth()) {
                    Text("Join session")
                }
            }
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

    ScreenFrame {
        TitleBlock(
            title = "Couple Lobby",
            subtitle = "Share this code with your partner and stay in the room. Leaving the match counts as a loss in the final competitive version."
        )
        Banner("Session code: ${session.sessionCode}")
        ErrorText(uiState.sessionError)

        Card(colors = CardDefaults.cardColors(containerColor = PanelNavy)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Players", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                session.participants.forEach {
                    ParticipantRow(participant = it)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(colors = CardDefaults.cardColors(containerColor = PanelCream)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Round settings", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = uiState.timerDraft,
                    onValueChange = onTimerChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Round timer in seconds") },
                    singleLine = true,
                    enabled = isHost
                )
                if (isHost) {
                    Button(onClick = onSaveTimer, modifier = Modifier.fillMaxWidth()) {
                        Text("Save timer")
                    }
                }
                Button(onClick = onToggleReady, modifier = Modifier.fillMaxWidth()) {
                    Text(if (me?.isReady == true) "Unready" else "I'm ready")
                }
                Button(
                    onClick = onStartRound,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isHost && bothReady
                ) {
                    Text("Start next round")
                }
                Button(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                    Text("Leave session")
                }
            }
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
        TitleBlock(
            title = "Pick Letters",
            subtitle = "This stage lasts 30 seconds. One player chooses the starting letter and the other chooses the ending letter."
        )
        Banner("${uiState.secondsRemaining}s left to lock both letters")
        ErrorText(uiState.sessionError)

        Card(colors = CardDefaults.cardColors(containerColor = PanelNavy)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${session.roundSetup.firstLetter ?: "_"} ... ${session.roundSetup.lastLetter ?: "_"}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = session.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(colors = CardDefaults.cardColors(containerColor = PanelCream)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = uiState.letterDraft,
                    onValueChange = onLetterChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Choose one letter") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                Button(
                    onClick = onChooseFirst,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFirstChooser && session.roundSetup.firstLetter == null
                ) {
                    Text("Lock as first letter")
                }
                Button(
                    onClick = onChooseLast,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isLastChooser && session.roundSetup.lastLetter == null
                ) {
                    Text("Lock as last letter")
                }
            }
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
        TitleBlock(
            title = "Live Round",
            subtitle = "Both players submit words that match the chosen letters. The app waits for both submissions or the timer to expire."
        )
        Banner("${uiState.secondsRemaining}s remaining")
        ErrorText(uiState.sessionError)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelNavy, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${session.roundSetup.firstLetter ?: "_"} ... ${session.roundSetup.lastLetter ?: "_"}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = session.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(colors = CardDefaults.cardColors(containerColor = PanelCream)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.wordDraft,
                    onValueChange = onWordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your word") },
                    singleLine = true
                )
                if (uiState.isSubmittingWord) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Validating your submission...")
                    }
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSubmittingWord && mySubmission == null
                ) {
                    Text(if (mySubmission == null) "Submit word" else "Submitted")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Submission board", style = MaterialTheme.typography.headlineSmall)
                session.participants.forEach { participant ->
                    SubmissionRow(
                        participant = participant,
                        submission = session.submissions.firstOrNull { it.playerId == participant.playerId }
                    )
                }
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
        TitleBlock(
            title = session.result?.winnerName?.let { "$it wins" } ?: "Round complete",
            subtitle = session.result?.reason ?: "The round has ended."
        )
        ErrorText(uiState.sessionError)

        Card(colors = CardDefaults.cardColors(containerColor = PanelCream)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Letters: ${session.roundSetup.firstLetter ?: "_"} ... ${session.roundSetup.lastLetter ?: "_"}",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (session.result?.winningWord != null) {
                    Text(
                        text = "Winning word: ${session.result.winningWord}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(text = session.message, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Round submissions", style = MaterialTheme.typography.headlineSmall)
                session.submissions.forEach { submission ->
                    ResultSubmissionRow(submission)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (isHost) {
            Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                Text("Start another round")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Button(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
            Text("Leave session")
        }
    }
}

@Composable
private fun TitleBlock(title: String, subtitle: String) {
    Text(text = title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
    )
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun Banner(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = AccentGold)) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = PanelNavy
        )
    }
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun ErrorText(text: String?) {
    if (text != null) {
        Text(text = text, color = AccentRed, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ParticipantRow(participant: SessionParticipant) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = participant.displayName + if (participant.isHost) " (Host)" else "",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = if (participant.isReady) "Ready" else "Waiting",
            color = if (participant.isReady) AccentGold else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SubmissionRow(participant: SessionParticipant, submission: RoundSubmission?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(participant.displayName, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = when {
                submission == null -> "Waiting"
                submission.isValid -> "Valid submitted"
                else -> "Invalid submitted"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (submission?.isValid == true) AccentGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun ResultSubmissionRow(submission: RoundSubmission) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelCream)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(submission.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (submission.word.isBlank()) "No word submitted" else submission.word,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (submission.isValid) {
                    "Valid via ${submission.source ?: "dictionary"}"
                } else {
                    submission.message ?: "Invalid word"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (submission.isValid) AccentGold else AccentRed
            )
        }
    }
}

@Composable
private fun ScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Top,
        content = content
    )
}

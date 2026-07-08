package com.wordduel.app.data.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.wordduel.app.data.model.ValidationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import kotlin.random.Random

class FirebaseSessionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : SessionRepository {

    private val usersRef: DatabaseReference = database.getReference("users")
    private val sessionsRef: DatabaseReference = database.getReference("sessions")

    override suspend fun saveProfile(displayName: String): PlayerProfile {
        val uid = ensureSignedIn()
        val profile = PlayerProfile(uid, displayName.trim())
        usersRef.child(uid).setValue(profile.toEntity()).awaitResult()
        return profile
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun createSession(profile: PlayerProfile, timerSeconds: Int): SessionSnapshot {
        ensureSignedIn()
        val code = generateCode()
        val snapshot = SessionSnapshot(
            sessionCode = code,
            hostPlayerId = profile.playerId,
            timerSeconds = timerSeconds.coerceIn(30, 180),
            participants = listOf(
                SessionParticipant(
                    playerId = profile.playerId,
                    displayName = profile.displayName,
                    isHost = true
                )
            ),
            message = "Invite your partner with code $code."
        )
        sessionsRef.child(code).setValue(snapshot.toEntity()).awaitResult()
        return snapshot
    }

    override suspend fun joinSession(sessionCode: String, profile: PlayerProfile): Result<SessionSnapshot> {
        return try {
            ensureSignedIn()
            val code = sessionCode.trim().uppercase()
            val existing = readSession(code)
                ?: return Result.failure(IllegalArgumentException("Session code not found."))

            if (existing.participants.any { it.playerId == profile.playerId }) {
                return Result.success(existing)
            }
            if (existing.participants.size >= 2) {
                return Result.failure(IllegalStateException("This session already has two players."))
            }

            val updated = existing.copy(
                participants = existing.participants + SessionParticipant(
                    playerId = profile.playerId,
                    displayName = profile.displayName,
                    isHost = false
                ),
                message = "Both players are here. Tap ready when you are set."
            )
            writeSession(updated)
            Result.success(updated)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override fun observeSession(sessionCode: String): Flow<SessionSnapshot?> = callbackFlow {
        val ref = sessionsRef.child(sessionCode.trim().uppercase())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toSessionSnapshot())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun setReady(sessionCode: String, playerId: String, ready: Boolean): SessionSnapshot? {
        val current = readSession(sessionCode.trim().uppercase()) ?: return null
        val updatedParticipants = current.participants.map {
            if (it.playerId == playerId) it.copy(isReady = ready, isConnected = true) else it
        }
        val bothReady = updatedParticipants.size == 2 && updatedParticipants.all { it.isReady }
        val updated = current.copy(
            participants = updatedParticipants,
            message = if (bothReady) {
                "Both players are ready. Host can start the letter round."
            } else {
                "Waiting for both players to tap ready."
            }
        )
        writeSession(updated)
        return updated
    }

    override suspend fun updateTimer(sessionCode: String, playerId: String, timerSeconds: Int): SessionSnapshot? {
        val current = readSession(sessionCode.trim().uppercase()) ?: return null
        if (current.hostPlayerId != playerId) return current
        val safeTimer = timerSeconds.coerceIn(30, 180)
        val updated = current.copy(
            timerSeconds = safeTimer,
            message = "Round timer set to $safeTimer seconds."
        )
        writeSession(updated)
        return updated
    }

    override suspend fun startNextRound(sessionCode: String, playerId: String): SessionSnapshot? {
        val current = readSession(sessionCode.trim().uppercase()) ?: return null
        if (current.hostPlayerId != playerId) return current
        if (current.participants.size < 2 || !current.participants.all { it.isReady }) return current

        val shuffled = current.participants.shuffled(Random(System.currentTimeMillis()))
        val deadline = System.currentTimeMillis() + 30_000L
        val updated = current.copy(
            phase = SessionPhase.PickingLetters,
            roundSetup = RoundSetup(
                roundNumber = current.roundSetup.roundNumber + if (current.result != null || current.submissions.isNotEmpty()) 1 else 0,
                firstLetterChooserId = shuffled[0].playerId,
                lastLetterChooserId = shuffled[1].playerId,
                lettersDeadlineAt = deadline
            ),
            submissions = emptyList(),
            result = null,
            message = "${shuffled[0].displayName} chooses the first letter. ${shuffled[1].displayName} chooses the last letter."
        )
        writeSession(updated)
        return updated
    }

    override suspend fun chooseLetter(
        sessionCode: String,
        playerId: String,
        chooseFirstLetter: Boolean,
        letter: Char
    ): SessionSnapshot? {
        val current = readSession(sessionCode.trim().uppercase()) ?: return null
        if (current.phase != SessionPhase.PickingLetters) return current

        val normalizedLetter = letter.uppercaseChar()
        val setup = current.roundSetup
        val canChoose = if (chooseFirstLetter) {
            setup.firstLetterChooserId == playerId && setup.firstLetter == null
        } else {
            setup.lastLetterChooserId == playerId && setup.lastLetter == null
        }
        if (!canChoose) return current

        val updatedSetup = if (chooseFirstLetter) {
            setup.copy(firstLetter = normalizedLetter)
        } else {
            setup.copy(lastLetter = normalizedLetter)
        }

        val bothChosen = updatedSetup.firstLetter != null && updatedSetup.lastLetter != null
        val roundStartedAt = if (bothChosen) System.currentTimeMillis() else updatedSetup.roundStartedAt
        val updated = current.copy(
            phase = if (bothChosen) SessionPhase.Round else SessionPhase.PickingLetters,
            roundSetup = updatedSetup.copy(
                roundStartedAt = roundStartedAt,
                roundEndsAt = if (bothChosen) roundStartedAt + current.timerSeconds * 1_000L else updatedSetup.roundEndsAt
            ),
            message = if (bothChosen) {
                "Both letters are locked. Submit your word before time runs out."
            } else {
                current.message
            }
        )
        writeSession(updated)
        return updated
    }

    override suspend fun submitWord(
        sessionCode: String,
        profile: PlayerProfile,
        word: String,
        validationResult: ValidationResult
    ): SessionSnapshot? {
        val current = readSession(sessionCode.trim().uppercase()) ?: return null
        if (current.phase != SessionPhase.Round) return current

        val submission = RoundSubmission(
            playerId = profile.playerId,
            displayName = profile.displayName,
            word = validationResult.normalizedWord.ifBlank { word.trim() },
            submittedAt = System.currentTimeMillis(),
            isValid = validationResult.isValid,
            source = validationResult.source,
            message = validationResult.message
        )

        val updatedSubmissions = current.submissions
            .filterNot { it.playerId == profile.playerId } + submission

        val updated = if (updatedSubmissions.size >= current.participants.size) {
            val result = decideWinner(current, updatedSubmissions, timedOut = false)
            current.copy(
                phase = SessionPhase.Result,
                submissions = updatedSubmissions.sortedBy { it.submittedAt },
                result = result,
                message = result.reason
            )
        } else {
            current.copy(
                submissions = updatedSubmissions.sortedBy { it.submittedAt },
                message = "${profile.displayName} submitted. Waiting for the other player."
            )
        }
        writeSession(updated)
        return updated
    }

    override suspend fun resolveRoundIfExpired(sessionCode: String, nowMillis: Long): SessionSnapshot? {
        val current = readSession(sessionCode.trim().uppercase()) ?: return null
        if (current.phase == SessionPhase.PickingLetters) {
            if (current.roundSetup.lettersDeadlineAt <= 0L || nowMillis < current.roundSetup.lettersDeadlineAt) return current
            val filledFirst = current.roundSetup.firstLetter ?: randomLetter()
            val filledLast = current.roundSetup.lastLetter ?: randomLetter()
            val roundStartedAt = System.currentTimeMillis()
            val updated = current.copy(
                phase = SessionPhase.Round,
                roundSetup = current.roundSetup.copy(
                    firstLetter = filledFirst,
                    lastLetter = filledLast,
                    roundStartedAt = roundStartedAt,
                    roundEndsAt = roundStartedAt + current.timerSeconds * 1_000L
                ),
                message = "Letter pick timer expired. Missing letters were auto-filled and the round has started."
            )
            writeSession(updated)
            return updated
        }

        if (current.phase != SessionPhase.Round) return current
        if (current.roundSetup.roundEndsAt <= 0L || nowMillis < current.roundSetup.roundEndsAt) return current

        val result = decideWinner(current, current.submissions, timedOut = true)
        val updated = current.copy(
            phase = SessionPhase.Result,
            result = result,
            message = result.reason
        )
        writeSession(updated)
        return updated
    }

    override suspend fun leaveSession(sessionCode: String, playerId: String): SessionSnapshot? {
        val current = readSession(sessionCode.trim().uppercase()) ?: return null
        val remaining = current.participants.filterNot { it.playerId == playerId }
        if (remaining.isEmpty()) {
            sessionsRef.child(sessionCode.trim().uppercase()).removeValue().awaitResult()
            return null
        }

        val nextHostId = if (current.hostPlayerId == playerId) remaining.first().playerId else current.hostPlayerId
        val updated = current.copy(
            hostPlayerId = nextHostId,
            participants = remaining.map { it.copy(isHost = it.playerId == nextHostId, isReady = false) },
            phase = SessionPhase.Lobby,
            submissions = emptyList(),
            result = null,
            message = "A player left the session."
        )
        writeSession(updated)
        return updated
    }

    private suspend fun ensureSignedIn(): String {
        val current = auth.currentUser
        if (current != null) return current.uid
        auth.signInAnonymously().awaitResult()
        return auth.currentUser?.uid ?: UUID.randomUUID().toString()
    }

    private suspend fun readSession(code: String): SessionSnapshot? {
        return sessionsRef.child(code).get().awaitResult().toSessionSnapshot()
    }

    private suspend fun writeSession(snapshot: SessionSnapshot) {
        sessionsRef.child(snapshot.sessionCode).setValue(snapshot.toEntity()).awaitResult()
    }

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(6) { append(alphabet.random()) }
        }
    }

    private fun randomLetter(): Char {
        return ('A'..'Z').random()
    }

    private fun decideWinner(
        current: SessionSnapshot,
        submissions: List<RoundSubmission>,
        timedOut: Boolean
    ): RoundResult {
        val validSubmissions = submissions.filter { it.isValid }.sortedBy { it.submittedAt }
        if (validSubmissions.isNotEmpty()) {
            val winner = validSubmissions.first()
            val reason = if (timedOut && submissions.size < current.participants.size) {
                "${winner.displayName} wins because only one valid word arrived before time expired."
            } else if (validSubmissions.size >= 2) {
                "${winner.displayName} wins with the faster valid word."
            } else {
                "${winner.displayName} wins with a valid submission."
            }
            return RoundResult(
                winnerPlayerId = winner.playerId,
                winnerName = winner.displayName,
                winningWord = winner.word,
                reason = reason
            )
        }

        return RoundResult(
            winnerPlayerId = null,
            winnerName = null,
            winningWord = null,
            reason = if (timedOut) {
                "Time expired and no valid word was submitted."
            } else {
                "Both submissions were invalid. Start another round."
            }
        )
    }
}

private data class PlayerProfileEntity(
    val playerId: String = "",
    val displayName: String = ""
)

private data class SessionParticipantEntity(
    val playerId: String = "",
    val displayName: String = "",
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val isConnected: Boolean = true
)

private data class RoundSetupEntity(
    val roundNumber: Int = 1,
    val firstLetterChooserId: String? = null,
    val lastLetterChooserId: String? = null,
    val firstLetter: String? = null,
    val lastLetter: String? = null,
    val lettersDeadlineAt: Long = 0L,
    val roundStartedAt: Long = 0L,
    val roundEndsAt: Long = 0L
)

private data class RoundSubmissionEntity(
    val playerId: String = "",
    val displayName: String = "",
    val word: String = "",
    val submittedAt: Long = 0L,
    val isValid: Boolean = false,
    val source: String? = null,
    val message: String? = null
)

private data class RoundResultEntity(
    val winnerPlayerId: String? = null,
    val winnerName: String? = null,
    val winningWord: String? = null,
    val reason: String = ""
)

private data class SessionSnapshotEntity(
    val sessionCode: String = "",
    val phase: String = SessionPhase.Lobby.name,
    val hostPlayerId: String = "",
    val timerSeconds: Int = 60,
    val participants: Map<String, SessionParticipantEntity> = emptyMap(),
    val roundSetup: RoundSetupEntity = RoundSetupEntity(),
    val submissions: Map<String, RoundSubmissionEntity> = emptyMap(),
    val result: RoundResultEntity? = null,
    val message: String = "Waiting for players."
)

private fun PlayerProfile.toEntity(): PlayerProfileEntity {
    return PlayerProfileEntity(playerId = playerId, displayName = displayName)
}

private fun SessionSnapshot.toEntity(): SessionSnapshotEntity {
    return SessionSnapshotEntity(
        sessionCode = sessionCode,
        phase = phase.name,
        hostPlayerId = hostPlayerId,
        timerSeconds = timerSeconds,
        participants = participants.associate { participant ->
            participant.playerId to SessionParticipantEntity(
                playerId = participant.playerId,
                displayName = participant.displayName,
                isHost = participant.isHost,
                isReady = participant.isReady,
                isConnected = participant.isConnected
            )
        },
        roundSetup = RoundSetupEntity(
            roundNumber = roundSetup.roundNumber,
            firstLetterChooserId = roundSetup.firstLetterChooserId,
            lastLetterChooserId = roundSetup.lastLetterChooserId,
            firstLetter = roundSetup.firstLetter?.toString(),
            lastLetter = roundSetup.lastLetter?.toString(),
            lettersDeadlineAt = roundSetup.lettersDeadlineAt,
            roundStartedAt = roundSetup.roundStartedAt,
            roundEndsAt = roundSetup.roundEndsAt
        ),
        submissions = submissions.associate { submission ->
            submission.playerId to RoundSubmissionEntity(
                playerId = submission.playerId,
                displayName = submission.displayName,
                word = submission.word,
                submittedAt = submission.submittedAt,
                isValid = submission.isValid,
                source = submission.source,
                message = submission.message
            )
        },
        result = result?.let {
            RoundResultEntity(
                winnerPlayerId = it.winnerPlayerId,
                winnerName = it.winnerName,
                winningWord = it.winningWord,
                reason = it.reason
            )
        },
        message = message
    )
}

private fun DataSnapshot.toSessionSnapshot(): SessionSnapshot? {
    val entity = getValue(SessionSnapshotEntity::class.java) ?: return null
    return SessionSnapshot(
        sessionCode = entity.sessionCode,
        phase = entity.phase.toSessionPhase(),
        hostPlayerId = entity.hostPlayerId,
        timerSeconds = entity.timerSeconds,
        participants = entity.participants.values.map {
            SessionParticipant(
                playerId = it.playerId,
                displayName = it.displayName,
                isHost = it.isHost,
                isReady = it.isReady,
                isConnected = it.isConnected
            )
        }.sortedByDescending { it.isHost },
        roundSetup = RoundSetup(
            roundNumber = entity.roundSetup.roundNumber,
            firstLetterChooserId = entity.roundSetup.firstLetterChooserId,
            lastLetterChooserId = entity.roundSetup.lastLetterChooserId,
            firstLetter = entity.roundSetup.firstLetter?.firstOrNull(),
            lastLetter = entity.roundSetup.lastLetter?.firstOrNull(),
            lettersDeadlineAt = entity.roundSetup.lettersDeadlineAt,
            roundStartedAt = entity.roundSetup.roundStartedAt,
            roundEndsAt = entity.roundSetup.roundEndsAt
        ),
        submissions = entity.submissions.values.map {
            RoundSubmission(
                playerId = it.playerId,
                displayName = it.displayName,
                word = it.word,
                submittedAt = it.submittedAt,
                isValid = it.isValid,
                source = it.source,
                message = it.message
            )
        }.sortedBy { it.submittedAt },
        result = entity.result?.let {
            RoundResult(
                winnerPlayerId = it.winnerPlayerId,
                winnerName = it.winnerName,
                winningWord = it.winningWord,
                reason = it.reason
            )
        },
        message = entity.message
    )
}

private fun String.toSessionPhase(): SessionPhase {
    return SessionPhase.entries.firstOrNull { it.name == this } ?: SessionPhase.Lobby
}

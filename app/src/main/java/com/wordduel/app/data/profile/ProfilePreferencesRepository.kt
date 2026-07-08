package com.wordduel.app.data.profile

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "word_duel_profile")

data class LocalProfileState(
    val hasCompletedOnboarding: Boolean = false,
    val playerName: String? = null
)

class ProfilePreferencesRepository(
    private val context: Context
) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val playerName = stringPreferencesKey("player_name")
    }

    val profileState: Flow<LocalProfileState> = context.profileDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences.toProfileState() }

    suspend fun savePlayerName(name: String) {
        context.profileDataStore.edit { preferences ->
            preferences[Keys.playerName] = name.trim()
            preferences[Keys.onboardingCompleted] = true
        }
    }

    suspend fun markOnboardingCompleted() {
        context.profileDataStore.edit { preferences ->
            preferences[Keys.onboardingCompleted] = true
        }
    }

    suspend fun resetOnboarding() {
        context.profileDataStore.edit { preferences ->
            preferences[Keys.onboardingCompleted] = false
        }
    }

    suspend fun clearProfile() {
        context.profileDataStore.edit { preferences ->
            preferences.remove(Keys.playerName)
            preferences[Keys.onboardingCompleted] = false
        }
    }

    private fun Preferences.toProfileState(): LocalProfileState {
        return LocalProfileState(
            hasCompletedOnboarding = this[Keys.onboardingCompleted] ?: false,
            playerName = this[Keys.playerName]
        )
    }
}

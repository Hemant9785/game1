package com.wordduel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordduel.app.data.network.NetworkModule
import com.wordduel.app.data.profile.ProfilePreferencesRepository
import com.wordduel.app.data.repository.RetrofitWordValidationRepository
import com.wordduel.app.data.session.FirebaseSessionRepository
import com.wordduel.app.ui.WordDuelApp
import com.wordduel.app.ui.WordDuelViewModel
import com.wordduel.app.ui.WordDuelViewModelFactory
import com.wordduel.app.ui.theme.WordDuelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val networkModule = NetworkModule.create(
            baseUrl = BuildConfig.WORD_VALIDATOR_BASE_URL,
            source = BuildConfig.WORD_VALIDATOR_SOURCE,
            enableLogging = BuildConfig.ENABLE_HTTP_LOGGING
        )
        val profileRepository = ProfilePreferencesRepository(applicationContext)

        setContent {
            WordDuelTheme {
                val viewModel: WordDuelViewModel = viewModel(
                    factory = WordDuelViewModelFactory(
                        repository = RetrofitWordValidationRepository(networkModule),
                        sessionRepository = FirebaseSessionRepository(),
                        profilePreferencesRepository = profileRepository
                    )
                )
                WordDuelApp(viewModel = viewModel)
            }
        }
    }
}

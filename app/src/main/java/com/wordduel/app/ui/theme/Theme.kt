package com.wordduel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = ForestInk,
    onPrimary = PanelCream,
    secondary = AccentGold,
    onSecondary = ForestInk,
    background = Sand,
    onBackground = ForestInk,
    surface = PanelCream,
    onSurface = ForestInk,
    error = AccentRed
)

@Composable
fun WordDuelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = WordDuelTypography,
        content = content
    )
}

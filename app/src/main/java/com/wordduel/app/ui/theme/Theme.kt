package com.wordduel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = InkBlue,
    onPrimary = CherryCream,
    secondary = AccentGold,
    onSecondary = InkBlue,
    tertiary = AccentCoral,
    background = RoseMist,
    onBackground = InkBlue,
    surface = CherryCream,
    onSurface = InkBlue,
    surfaceVariant = SoftLilac,
    onSurfaceVariant = MidnightPlum,
    outline = BorderBlush,
    error = AccentRed
)

private val WordDuelShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

@Composable
fun WordDuelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = WordDuelTypography,
        shapes = WordDuelShapes,
        content = content
    )
}

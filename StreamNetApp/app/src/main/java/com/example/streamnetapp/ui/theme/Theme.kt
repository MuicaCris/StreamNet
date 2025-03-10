package com.example.streamnetapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StreamNetColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = AccentGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = AccentGreen,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = TextBlack,
    onSecondary = TextBlack,
    onTertiary = TextBlack,
    onBackground = TextBlack,
    onSurface = TextBlack
)

@Composable
fun StreamNetTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StreamNetColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StreamNetTypography,
        content = content
    )
}
package com.example.mgpt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Lime600,
    secondary = Stone700,
    tertiary = Blue600,
    background = Stone950,
    surface = Stone900,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Stone300,
    onSurface = Stone300,
    error = Red700
)

private val LightColorScheme = lightColorScheme(
    primary = Lime600,
    secondary = Stone700,
    tertiary = Blue600,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Stone900,
    onSurface = Stone900,
    error = Red700
)

@Composable
fun MgptTheme(
    darkTheme: Boolean = true, // Default to dark for military aesthetic
    dynamicColor: Boolean = false, // Maintain tactical look, no dynamic colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

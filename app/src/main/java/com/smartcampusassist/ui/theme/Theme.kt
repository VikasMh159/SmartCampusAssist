package com.smartcampusassist.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.smartcampusassist.theme.AppTypography

private val DarkColorScheme = darkColorScheme()

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    primaryContainer = SurfaceTint,
    secondary = SecondaryColor,
    secondaryContainer = SurfaceMuted,
    tertiary = AccentColor,
    background = BackgroundLight,
    surface = SurfaceWhite,
    surfaceVariant = SurfaceMuted,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onPrimaryContainer = PrimaryVariant,
    onSecondaryContainer = TextPrimary,
    onSurfaceVariant = TextSecondary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = OutlineSoft
)

@Composable
fun SmartCampusAssistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

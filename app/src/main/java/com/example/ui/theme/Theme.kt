package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FmPdfLightColorScheme = lightColorScheme(
    primary = FmRed,
    onPrimary = Color.White,
    primaryContainer = FmRedLight,
    onPrimaryContainer = OnFmRedContainer,
    secondary = FmSlate,
    onSecondary = Color.White,
    secondaryContainer = FmSurfaceVariant,
    onSecondaryContainer = FmSlateDark,
    tertiary = FmBlue,
    onTertiary = Color.White,
    background = FmBackground,
    onBackground = FmSlateDark,
    surface = FmPureWhite,
    onSurface = FmSlateDark,
    surfaceVariant = FmSurfaceVariant,
    onSurfaceVariant = FmSlateMuted,
    outline = FmBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun FmPdfTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FmPdfLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FmPdfTheme(content = content)
}

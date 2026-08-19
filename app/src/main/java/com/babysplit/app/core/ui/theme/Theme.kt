package com.babysplit.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = TurquoisePrimary,
    onPrimary = Color.White,
    primaryContainer = TurquoiseLight,
    onPrimaryContainer = TurquoiseDark,
    secondary = TurquoiseMint,
    onSecondary = Color.White,
    secondaryContainer = TurquoiseSubtle,
    onSecondaryContainer = TurquoiseDark,
    tertiary = WhatsAppDarkGreen,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorderLight,
    error = DebtRed,
    onError = Color.White,
    errorContainer = DebtRedLight,
    onErrorContainer = Color(0xFF8A0000)
)

private val DarkColorScheme = LightColorScheme // Default to light theme for vibrant baby chick look

@Composable
fun BabySplitTheme(
    darkTheme: Boolean = false, // Always vibrant Light Theme per user requirement
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

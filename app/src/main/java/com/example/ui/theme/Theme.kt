package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NatDarkPrimary,
    onPrimary = NatDarkOnPrimary,
    primaryContainer = NatDarkPrimaryContainer,
    onPrimaryContainer = NatDarkOnPrimaryContainer,
    secondary = NatDarkSecondary,
    onSecondary = NatDarkOnSecondary,
    secondaryContainer = NatDarkSecondaryContainer,
    onSecondaryContainer = NatDarkOnSecondaryContainer,
    background = NatDarkBackground,
    onBackground = NatDarkOnBackground,
    surface = NatDarkSurface,
    onSurface = NatDarkOnSurface,
    surfaceVariant = NatDarkSurfaceVariant,
    onSurfaceVariant = NatDarkOnSurfaceVariant,
    error = NatError,
    onError = NatOnError
)

private val LightColorScheme = lightColorScheme(
    primary = NatPrimary,
    onPrimary = NatOnPrimary,
    primaryContainer = NatPrimaryContainer,
    onPrimaryContainer = NatOnPrimaryContainer,
    secondary = NatSecondary,
    onSecondary = NatOnSecondary,
    secondaryContainer = NatSecondaryContainer,
    onSecondaryContainer = NatOnSecondaryContainer,
    background = NatBackground,
    onBackground = NatOnBackground,
    surface = NatSurface,
    onSurface = NatOnSurface,
    surfaceVariant = NatSurfaceVariant,
    onSurfaceVariant = NatOnSurfaceVariant,
    error = NatError,
    onError = NatOnError
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

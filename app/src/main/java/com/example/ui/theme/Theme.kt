package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GuardDayPrimary,
    onPrimary = GuardDayOnPrimary,
    primaryContainer = GuardDayPrimaryContainer,
    onPrimaryContainer = GuardDayOnPrimaryContainer,
    secondary = GuardDaySecondary,
    onSecondary = GuardDayOnSecondary,
    secondaryContainer = GuardDaySecondaryContainer,
    onSecondaryContainer = GuardDayOnSecondaryContainer,
    tertiary = GuardDayTertiary,
    onTertiary = GuardDayOnTertiary,
    tertiaryContainer = GuardDayTertiaryContainer,
    onTertiaryContainer = GuardDayOnTertiaryContainer,
    background = GuardDayDarkBackground,
    onBackground = GuardDayDarkOnSurface,
    surface = GuardDayDarkSurface,
    onSurface = GuardDayDarkOnSurface,
    surfaceVariant = GuardDayDarkSurfaceVariant,
    onSurfaceVariant = GuardDayDarkOnSurfaceVariant,
    outline = GuardDayDarkOutline,
    outlineVariant = GuardDayDarkOutlineVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6E559C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE6F9),
    onPrimaryContainer = Color(0xFF261843),
    secondary = Color(0xFF8873B3),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1EBFB),
    onSecondaryContainer = Color(0xFF2C1E48),
    tertiary = Color(0xFFB06482),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDE8F1),
    onTertiaryContainer = Color(0xFF431629),
    background = GuardDayLightBackground,
    onBackground = GuardDayLightOnSurface,
    surface = GuardDayLightSurface,
    onSurface = GuardDayLightOnSurface,
    surfaceVariant = GuardDayLightSurfaceVariant,
    onSurfaceVariant = GuardDayLightOnSurfaceVariant,
    outline = GuardDayLightOutline,
    outlineVariant = GuardDayLightOutlineVariant
  )

@Composable
fun PrivTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep Priv signature identity
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

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  PrivTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}


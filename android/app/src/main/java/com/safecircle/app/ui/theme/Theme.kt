package com.safecircle.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    error = BrandError,
    errorContainer = BrandErrorContainer,
    background = BrandBackgroundLight,
    surface = BrandSurfaceLight,
    surfaceVariant = BrandSurfaceVariantLight,
    onSurface = BrandOnSurfaceLight,
    onSurfaceVariant = BrandOnSurfaceVariantLight,
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandPrimaryDark,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = BrandPrimaryContainer,
    secondary = BrandSecondary,
    secondaryContainer = BrandOnSecondaryContainer,
    onSecondaryContainer = BrandSecondaryContainer,
    tertiary = BrandTertiary,
    tertiaryContainer = BrandOnTertiaryContainer,
    onTertiaryContainer = BrandTertiaryContainer,
    error = BrandError,
    errorContainer = BrandErrorContainer,
    background = BrandBackgroundDark,
    surface = BrandSurfaceDark,
    surfaceVariant = BrandSurfaceVariantDark,
    onSurface = BrandOnSurfaceDark,
    onSurfaceVariant = BrandOnSurfaceVariantDark,
)

@Composable
fun SafeCircleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SafeCircleTypography,
        content = content
    )
}

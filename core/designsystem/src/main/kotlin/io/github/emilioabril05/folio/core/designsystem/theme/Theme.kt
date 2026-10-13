package io.github.emilioabril05.folio.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val DEFAULT_ANIMATOR_SCALE = 1f

/**
 * Tema de Folio: paleta propia sin color dinámico, Inter, esquinas de 12 a 16 dp y
 * movimiento que respeta "Quitar animaciones" del sistema.
 */
@Composable
fun FolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) FolioColors.Dark else FolioColors.Light
    val context = LocalContext.current
    val reducedMotion =
        remember(context) {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                DEFAULT_ANIMATOR_SCALE,
            ) == 0f
        }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            typography = FolioTypography,
            shapes = FolioShapes,
            content = content,
        )
    }
}

// Todos los roles de Material 3 salen de la paleta: un solo acento y ningún color por defecto de Material.
// Solo los roles de error y el velo de los diálogos conservan los valores de Material.
private fun FolioPalette.toColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent,
        onPrimaryContainer = onAccent,
        inversePrimary = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = text,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = surfaceHigh,
        onTertiaryContainer = text,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = textSecondary,
        surfaceTint = surface,
        inverseSurface = text,
        inverseOnSurface = background,
        outline = outline,
        outlineVariant = surfaceHigh,
        surfaceBright = surface,
        surfaceDim = background,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHigh,
        primaryFixed = accent,
        primaryFixedDim = accent,
        onPrimaryFixed = onAccent,
        onPrimaryFixedVariant = onAccent,
        secondaryFixed = accent,
        secondaryFixedDim = accent,
        onSecondaryFixed = onAccent,
        onSecondaryFixedVariant = onAccent,
        tertiaryFixed = accent,
        tertiaryFixedDim = accent,
        onTertiaryFixed = onAccent,
        onTertiaryFixedVariant = onAccent,
    )
}

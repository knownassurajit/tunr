package com.knownassurajit.tunr_tool.app.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LockEmerald,
    onPrimary = AmoledBlack,
    primaryContainer = LockContainer,
    onPrimaryContainer = OnLockContainer,
    secondary = MatteBlue,
    onSecondary = AmoledBlack,
    secondaryContainer = MatteBlueContainer,
    onSecondaryContainer = OnMatteBlueContainer,
    background = MatteBackground,
    onBackground = TextPrimary,
    surface = MatteSurface,
    onSurface = TextPrimary,
    surfaceVariant = MatteSurfaceContainer,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = MatteSurfaceLow,
    surfaceContainer = MatteSurfaceContainer,
    surfaceContainerHigh = MatteSurfaceContainerHigh,
    surfaceContainerHighest = MatteSurfaceContainerHighest,
    outline = BorderMedium,
    outlineVariant = BorderSubtle
)

@Composable
fun TunrTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TunrTypography,
        shapes = TunrShapes,
        content = content
    )
}

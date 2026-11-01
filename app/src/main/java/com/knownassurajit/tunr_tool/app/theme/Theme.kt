package com.knownassurajit.tunr_tool.app.theme

import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import com.knownassurajit.tunr_tool.app.model.AppThemeScheme

// -----------------------------------------------------------------------------
// 1. EMERALD THEME (Default Studio Precision)
// Pure AMOLED black canvas with vibrant acoustic lime & mint accents
// -----------------------------------------------------------------------------
val EmeraldDarkColors = darkColorScheme(
    primary = Color(0xFFB9F23C),
    onPrimary = Color(0xFF1F3000),
    primaryContainer = Color(0xFF2E4A00),
    onPrimaryContainer = Color(0xFFD6FF7F),
    inversePrimary = Color(0xFF3F6900),

    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF005234),
    onSecondaryContainer = Color(0xFF6EE7B7),

    tertiary = Color(0xFF86D9EB),
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFF004E5B),
    onTertiaryContainer = Color(0xFFB4EBFA),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color.Black,
    onBackground = Color(0xFFE3E8D6),
    surface = Color.Black,
    onSurface = Color(0xFFE3E8D6),
    surfaceVariant = Color(0xFF3F4535),
    onSurfaceVariant = Color(0xFFC3C9B1),
    surfaceTint = Color(0xFFB9F23C),
    inverseSurface = Color(0xFFE3E8D6),
    inverseOnSurface = Color(0xFF2F3328),
    outline = Color(0xFF8D937D),
    outlineVariant = Color(0xFF3F4535),
    scrim = Color.Black,

    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF2A2F20),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0D120B),
    surfaceContainer = Color(0xFF141C11),
    surfaceContainerHigh = Color(0xFF1B2618),
    surfaceContainerHighest = Color(0xFF243320),
)

// -----------------------------------------------------------------------------
// 2. AZURE THEME (Electric Cyan & Sky Blue)
// High-energy modern electric aesthetic with crisp sky blue & cyan glows
// -----------------------------------------------------------------------------
val AzureDarkColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF00354E),
    primaryContainer = Color(0xFF004D70),
    onPrimaryContainer = Color(0xFFBAE6FD),
    inversePrimary = Color(0xFF0284C7),

    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF00354E),
    secondaryContainer = Color(0xFF0C2B42),
    onSecondaryContainer = Color(0xFFBAE6FD),

    tertiary = Color(0xFF67E8F9),
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFF083344),
    onTertiaryContainer = Color(0xFFCFFAFE),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color.Black,
    onBackground = Color(0xFFE0F2FE),
    surface = Color.Black,
    onSurface = Color(0xFFE0F2FE),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = Color(0xFF38BDF8),
    inverseSurface = Color(0xFFE0F2FE),
    inverseOnSurface = Color(0xFF0F172A),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF1E293B),
    scrim = Color.Black,

    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF1E293B),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A121D),
    surfaceContainer = Color(0xFF0F1B2C),
    surfaceContainerHigh = Color(0xFF16253D),
    surfaceContainerHighest = Color(0xFF1F3250),
)

// -----------------------------------------------------------------------------
// 3. MONOCHROME THEME (Minimalist Matte Slate & Mist)
// Pure distraction-free monochrome slate with silver & mist accents
// -----------------------------------------------------------------------------
val MonochromeDarkColors = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF94A3B8),

    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),

    tertiary = Color(0xFFCBD5E1),
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF334155),
    onTertiaryContainer = Color(0xFFF1F5F9),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color.Black,
    onBackground = Color(0xFFF1F5F9),
    surface = Color.Black,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0F172A),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    scrim = Color.Black,

    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF242933),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0D0F13),
    surfaceContainer = Color(0xFF14171E),
    surfaceContainerHigh = Color(0xFF1C202A),
    surfaceContainerHighest = Color(0xFF262C38),
)

// -----------------------------------------------------------------------------
// 4. AMBER THEME (Warm Gold & Stage Brass)
// Warm organic resonance with stage brass, amber, and gold highlights
// -----------------------------------------------------------------------------
val AmberDarkColors = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF452B00),
    primaryContainer = Color(0xFF633F00),
    onPrimaryContainer = Color(0xFFFEF3C7),
    inversePrimary = Color(0xFFD97706),

    secondary = Color(0xFFFCD34D),
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF3D2805),
    onSecondaryContainer = Color(0xFFFDE68A),

    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF431407),
    tertiaryContainer = Color(0xFF7C2D12),
    onTertiaryContainer = Color(0xFFFFEDD5),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color.Black,
    onBackground = Color(0xFFFFFBEB),
    surface = Color.Black,
    onSurface = Color(0xFFFFFBEB),
    surfaceVariant = Color(0xFF42382A),
    onSurfaceVariant = Color(0xFFD4C4AE),
    surfaceTint = Color(0xFFFBBF24),
    inverseSurface = Color(0xFFFFFBEB),
    inverseOnSurface = Color(0xFF2E2416),
    outline = Color(0xFF9C8E7B),
    outlineVariant = Color(0xFF42382A),
    scrim = Color.Black,

    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF2E2416),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF130F08),
    surfaceContainer = Color(0xFF1B150C),
    surfaceContainerHigh = Color(0xFF261E11),
    surfaceContainerHighest = Color(0xFF332917),
)

// -----------------------------------------------------------------------------
// 5. VIOLET THEME (Radiant Purple & Plum)
// Deep psychedelic & synthwave atmosphere with radiant purple & lavender
// -----------------------------------------------------------------------------
val VioletDarkColors = darkColorScheme(
    primary = Color(0xFFA855F7),
    onPrimary = Color(0xFF380062),
    primaryContainer = Color(0xFF5B0099),
    onPrimaryContainer = Color(0xFFF3E8FF),
    inversePrimary = Color(0xFF9333EA),

    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF380062),
    secondaryContainer = Color(0xFF361254),
    onSecondaryContainer = Color(0xFFE9D5FF),

    tertiary = Color(0xFFE879F9),
    onTertiary = Color(0xFF4A044E),
    tertiaryContainer = Color(0xFF701A75),
    onTertiaryContainer = Color(0xFFFDF4FF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color.Black,
    onBackground = Color(0xFFFAF5FF),
    surface = Color.Black,
    onSurface = Color(0xFFFAF5FF),
    surfaceVariant = Color(0xFF3B2F48),
    onSurfaceVariant = Color(0xFFD0C0DF),
    surfaceTint = Color(0xFFA855F7),
    inverseSurface = Color(0xFFFAF5FF),
    inverseOnSurface = Color(0xFF281E33),
    outline = Color(0xFF9888A7),
    outlineVariant = Color(0xFF3B2F48),
    scrim = Color.Black,

    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF281E33),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF120B1C),
    surfaceContainer = Color(0xFF1A1127),
    surfaceContainerHigh = Color(0xFF251937),
    surfaceContainerHighest = Color(0xFF322349),
)

fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> Color(AndroidColor.parseColor("#$clean"))
            8 -> Color(AndroidColor.parseColor("#$clean"))
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

private fun hsvColor(h: Float, s: Float, v: Float): Color {
    val hsv = floatArrayOf((h % 360f + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    return Color(AndroidColor.HSVToColor(hsv))
}

/**
 * Dynamically synthesizes an AMOLED true-black Material 3 ColorScheme from any arbitrary seed color.
 * Maintains strict contrast requirements and OLED power savings (#000000 background/surface).
 */
fun buildAmoledColorScheme(seed: Color): ColorScheme {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]
    val sat = hsv[1]
    val value = hsv[2]

    // Perceived luminance
    val lum = 0.299f * seed.red + 0.587f * seed.green + 0.114f * seed.blue
    val onPrimary = if (lum > 0.45f) Color(0xFF10150A) else Color.White

    val primaryContainer = hsvColor(hue, (sat * 0.9f).coerceIn(0.4f, 1f), 0.28f)
    val onPrimaryContainer = hsvColor(hue, (sat * 0.35f), 0.98f)

    val secondaryHue = (hue + 20f) % 360f
    val secondary = hsvColor(secondaryHue, (sat * 0.75f).coerceIn(0.3f, 0.85f), 0.90f)
    val secondaryLum = 0.299f * secondary.red + 0.587f * secondary.green + 0.114f * secondary.blue
    val onSecondary = if (secondaryLum > 0.45f) Color(0xFF10150A) else Color.White
    val secondaryContainer = hsvColor(secondaryHue, (sat * 0.8f).coerceIn(0.4f, 1f), 0.22f)
    val onSecondaryContainer = hsvColor(secondaryHue, sat * 0.3f, 0.95f)

    val tertiaryHue = (hue + 15f) % 360f
    val tertiary = hsvColor(tertiaryHue, (sat * 0.85f).coerceIn(0.3f, 0.95f), 0.92f)
    val tertiaryContainer = hsvColor(tertiaryHue, 0.7f, 0.24f)
    val onTertiaryContainer = hsvColor(tertiaryHue, 0.3f, 0.96f)

    return darkColorScheme(
        primary = seed,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = hsvColor(hue, sat, 0.45f),

        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,

        tertiary = tertiary,
        onTertiary = Color.Black,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,

        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),

        background = Color.Black,
        onBackground = Color(0xFFF1F5F9),
        surface = Color.Black,
        onSurface = Color(0xFFF1F5F9),
        surfaceVariant = hsvColor(hue, 0.15f, 0.22f),
        onSurfaceVariant = Color(0xFF94A3B8),
        surfaceTint = seed,
        inverseSurface = Color(0xFFF1F5F9),
        inverseOnSurface = Color(0xFF121212),
        outline = hsvColor(hue, 0.20f, 0.45f),
        outlineVariant = hsvColor(hue, 0.15f, 0.20f),
        scrim = Color.Black,

        surfaceDim = Color.Black,
        surfaceBright = hsvColor(hue, 0.10f, 0.16f),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = hsvColor(hue, 0.12f, 0.06f),
        surfaceContainer = hsvColor(hue, 0.14f, 0.09f),
        surfaceContainerHigh = hsvColor(hue, 0.16f, 0.13f),
        surfaceContainerHighest = hsvColor(hue, 0.18f, 0.18f),
    )
}

fun AppThemeScheme.toColorScheme(customHex: String = "#B9F23C"): ColorScheme = when (this) {
    AppThemeScheme.Emerald -> EmeraldDarkColors
    AppThemeScheme.Azure -> AzureDarkColors
    AppThemeScheme.Monochrome -> MonochromeDarkColors
    AppThemeScheme.Amber -> AmberDarkColors
    AppThemeScheme.Violet -> VioletDarkColors
    AppThemeScheme.Custom -> {
        val parsed = parseHexColor(customHex) ?: Color(0xFFB9F23C)
        buildAmoledColorScheme(parsed)
    }
}

/**
 * Material 3 Expressive theme with dynamic color scheme selection.
 * Always renders with true OLED black background (#000000) for zero battery drain.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TunrTheme(
    themeScheme: AppThemeScheme = AppThemeScheme.Emerald,
    customHex: String = "#B9F23C",
    content: @Composable () -> Unit,
) {
    val colors = themeScheme.toColorScheme(customHex)
    val reducedMotion by rememberReducedMotion()
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
            shapes = TunrShapes,
            content = content,
        )
    }
}

/** Fixed-width digits so live readouts (Hz, cents, BPM) never jitter sideways. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

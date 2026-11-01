package com.knownassurajit.tunr_tool.app.ui.sheets

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NoiseAware
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.model.AppThemeScheme
import com.knownassurajit.tunr_tool.app.model.MetronomeSound
import com.knownassurajit.tunr_tool.app.model.NotationPreference
import com.knownassurajit.tunr_tool.app.model.Precision
import com.knownassurajit.tunr_tool.app.model.ScreenDimTimeout
import com.knownassurajit.tunr_tool.app.model.ScreenTimeout
import com.knownassurajit.tunr_tool.app.model.SettingsState
import com.knownassurajit.tunr_tool.app.model.TunerSensitivity
import com.knownassurajit.tunr_tool.app.theme.parseHexColor
import com.knownassurajit.tunr_tool.app.theme.tabular
import com.knownassurajit.tunr_tool.app.ui.components.SettingCard
import com.knownassurajit.tunr_tool.app.ui.components.TunrSlider
import java.util.Locale
import kotlin.math.roundToInt

private const val A4_MIN = 415f
private const val A4_MAX = 466f

/**
 * Descriptive guidance metadata surfaced when the user taps the question mark icon on any section.
 */
private data class SettingsSectionInfo(
    val title: String,
    val icon: ImageVector,
    val guideItems: List<Pair<String, String>>,
)

private val tuningGuide = SettingsSectionInfo(
    title = "Tuning & Calibration Guide",
    icon = Icons.Rounded.Tune,
    guideItems = listOf(
        "Tuning Precision" to "Standard (±3¢) offers comfortable everyday tuning. High (±1¢) delivers studio-grade accuracy. Ultra (±0.5¢) is tailored for luthiers, setup work, and exacting intonation.",
        "Response Sensitivity" to "Smooth mode applies intelligent anti-jitter damping to calm fluctuating frequencies, making the dial steady and easy to catch up with. Standard balances responsiveness and stability. Fast offers direct, unfiltered needle movement.",
        "Musical Notation" to "Switch between Sharps (C♯, D♯, F♯, G♯, A♯) and Flats (D♭, E♭, G♭, A♭, B♭) to match your sheet music, charts, or personal preference.",
        "A4 Reference Pitch" to "Concert standard is 440 Hz. Quickly switch between historical Baroque (415 Hz), Verdi / Scientific (432 Hz), or European orchestra (442 Hz) concert standards.",
    )
)

private val appearanceGuide = SettingsSectionInfo(
    title = "Appearance & Theme Guide",
    icon = Icons.Rounded.Palette,
    guideItems = listOf(
        "AMOLED Themes" to "Curated dark colorways crafted for pure-black OLED screens. Maximizes contrast on dark stages while minimizing battery consumption.",
        "Full Spectrum & HEX" to "Drag across the 360° color wheel or type any 6-digit HEX code to style TUNR with your personal identity.",
        "Accent Synchronization" to "The library icon, tuning dial, waveform meters, and launcher icons dynamically adapt to your chosen accent color for a unified look.",
        "Circular Color Options" to "Tap any signature circular swatch at the top to immediately preview and apply vibrant musician color schemes.",
    )
)

private val audioFilterGuide = SettingsSectionInfo(
    title = "Noise Suppression & Audio Filters",
    icon = Icons.Rounded.NoiseAware,
    guideItems = listOf(
        "Noise Gate Threshold" to "Rejects room sounds, amp hum, and ambient chatter below the threshold so the tuner only listens to your instrument.",
        "Harmonic Speech Filter" to "Uses autocorrelation periodicity analysis to distinguish vibrating instrument strings from speaking voices, crowd noise, and clapping.",
        "Subsonic 35Hz Filter" to "Blocks microphone DC drift, wind drafts, and physical handling thumps when holding or mounting your phone.",
    )
)

private val metronomeGuide = SettingsSectionInfo(
    title = "Metronome Audio & Tactile Guide",
    icon = Icons.Rounded.Speed,
    guideItems = listOf(
        "Sound Profiles" to "Choose between acoustic woodblock, sharp electronic digital click, drum rimshot, or resonant Latin clave strikes.",
        "Metronome Volume" to "Attenuate metronome output to sit comfortably alongside your acoustic or amplified instrument.",
        "Beat Haptics" to "Feel tactile pulses in your hand on every beat, with a distinct heavy pulse on measure downbeats.",
        "Visual Beat Flash" to "Pulses high contrast across the screen on measure downbeats for silent practice or noisy rehearsal rooms.",
    )
)

private val hardwareGuide = SettingsSectionInfo(
    title = "Display & Hardware Guide",
    icon = Icons.Rounded.Visibility,
    guideItems = listOf(
        "Screen Timeout" to "Keep the screen awake while practicing so you don't need to keep tapping your device while your hands are on your instrument.",
        "Energy Saver Dimming" to "Reduces OLED brightness to 4% after inactivity to eliminate screen burn-in and save power. Tapping the screen or plucking a string immediately restores full brightness.",
        "Haptic Pitch Lock" to "Provides a crisp haptic tap the moment your note locks into the in-tune window.",
        "Sound Pitch Lock" to "Plays a gentle crystal harmonic chime confirmation when the pitch locks in tune.",
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: SettingsState,
    onChange: (SettingsState) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val segmentedButtonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = scheme.primaryContainer,
        activeContentColor = scheme.onPrimaryContainer,
        activeBorderColor = scheme.primary.copy(alpha = 0.7f),
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = scheme.onSurfaceVariant,
        inactiveBorderColor = scheme.outlineVariant.copy(alpha = 0.4f),
    )
    var activeGuide by remember { mutableStateOf<SettingsSectionInfo?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Calibration & settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )

            // -------------------------------------------------------------------------
            // Section 1: Tuning Precision, Sensitivity & Notation
            // -------------------------------------------------------------------------
            SectionHeader(
                icon = Icons.Rounded.Tune,
                title = "Tuning & calibration",
                onInfoClick = { activeGuide = tuningGuide },
            )

            SettingCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tuning precision", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = scheme.primaryContainer,
                    ) {
                        Text(
                            settings.precision.thresholdLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Precision.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = settings.precision == option,
                            onClick = { onChange(settings.copy(precision = option)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = Precision.entries.size),
                            icon = {},
                            colors = segmentedButtonColors,
                            label = {
                                Text(
                                    option.label,
                                    maxLines = 1,
                                    fontWeight = if (settings.precision == option) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                Text(
                    settings.precision.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            // Sensitivity / Anti-Jitter Damping Setting
            SettingCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tuner sensitivity & anti-jitter", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Stabilizes fluctuating frequencies to catch up with the tuner dial",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = scheme.primaryContainer,
                    ) {
                        Text(
                            settings.sensitivity.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TunerSensitivity.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = settings.sensitivity == option,
                            onClick = { onChange(settings.copy(sensitivity = option)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = TunerSensitivity.entries.size),
                            icon = {},
                            colors = segmentedButtonColors,
                            label = {
                                Text(
                                    option.label,
                                    maxLines = 1,
                                    fontWeight = if (settings.sensitivity == option) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                Text(
                    settings.sensitivity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            SettingCard {
                Text("Musical notation", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NotationPreference.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = settings.notation == option,
                            onClick = { onChange(settings.copy(notation = option)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = NotationPreference.entries.size),
                            icon = {},
                            colors = segmentedButtonColors,
                            label = {
                                Text(
                                    option.label,
                                    maxLines = 1,
                                    fontWeight = if (settings.notation == option) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                Text(
                    if (settings.notation == NotationPreference.Sharps)
                        "Displays accidental notes as C♯, D♯, F♯, G♯, A♯"
                    else
                        "Displays accidental notes as D♭, E♭, G♭, A♭, B♭",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            // A4 Concert Pitch Calibration
            SettingCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("A4 reference pitch", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Concert standard is 440 Hz",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "%.1f Hz".format(settings.a4Hz),
                        style = MaterialTheme.typography.headlineSmallEmphasized.tabular(),
                        color = scheme.primary,
                    )
                }

                TunrSlider(
                    value = settings.a4Hz,
                    onValueChange = { onChange(settings.copy(a4Hz = (it * 10).roundToInt() / 10f)) },
                    valueRange = A4_MIN..A4_MAX,
                )

                // Quick Concert Pitch Presets - Uses color for selection, no check icons
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        415f to "415 Hz (Baroque)",
                        432f to "432 Hz (Verdi)",
                        440f to "440 Hz (Standard)",
                        442f to "442 Hz (European)"
                    ).forEach { (freq, label) ->
                        val isSelected = settings.a4Hz == freq
                        FilterChip(
                            selected = isSelected,
                            onClick = { onChange(settings.copy(a4Hz = freq)) },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            leadingIcon = null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primaryContainer,
                                selectedLabelColor = scheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { onChange(settings.copy(a4Hz = (settings.a4Hz - 1f).coerceAtLeast(A4_MIN))) },
                        shapes = ButtonDefaults.shapes(),
                    ) { Text("−1 Hz") }
                    FilledTonalButton(
                        onClick = { onChange(settings.copy(a4Hz = (settings.a4Hz + 1f).coerceAtMost(A4_MAX))) },
                        shapes = ButtonDefaults.shapes(),
                    ) { Text("+1 Hz") }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { onChange(settings.copy(a4Hz = 440f)) },
                        enabled = settings.a4Hz != 440f,
                        shapes = ButtonDefaults.shapes(),
                    ) { Text("Reset 440") }
                }
            }

            // -------------------------------------------------------------------------
            // Section 2: Appearance & Theme
            // -------------------------------------------------------------------------
            SectionHeader(
                icon = Icons.Rounded.Palette,
                title = "Appearance & theme",
                onInfoClick = { activeGuide = appearanceGuide },
            )

            SettingCard {
                val haptic = LocalHapticFeedback.current

                // Curated signature color options for the top circular picker
                val paletteOptions = remember {
                    listOf(
                        Triple("#B9F23C", "Emerald", AppThemeScheme.Emerald),
                        Triple("#38BDF8", "Azure", AppThemeScheme.Azure),
                        Triple("#FBBF24", "Amber", AppThemeScheme.Amber),
                        Triple("#A855F7", "Violet", AppThemeScheme.Violet),
                        Triple("#E2E8F0", "Monochrome", AppThemeScheme.Monochrome),
                        Triple("#34D399", "Mint", null as AppThemeScheme?),
                        Triple("#FB923C", "Orange", null as AppThemeScheme?),
                        Triple("#F43F5E", "Rose", null as AppThemeScheme?),
                        Triple("#EC4899", "Pink", null as AppThemeScheme?),
                        Triple("#6366F1", "Indigo", null as AppThemeScheme?),
                    )
                }

                val activeColor = if (settings.themeScheme == AppThemeScheme.Custom) {
                    parseHexColor(settings.customThemeHex) ?: scheme.primary
                } else {
                    Color(settings.themeScheme.primaryColorHex)
                }

                val activeLabel = if (settings.themeScheme == AppThemeScheme.Custom) {
                    paletteOptions.find { it.first.equals(settings.customThemeHex, ignoreCase = true) }?.second ?: settings.customThemeHex.uppercase()
                } else {
                    settings.themeScheme.label
                }

                var currentHue by remember(settings.customThemeHex) {
                    mutableFloatStateOf(hexToHue(settings.customThemeHex))
                }

                var hexInput by remember(settings.customThemeHex) {
                    mutableStateOf(settings.customThemeHex.removePrefix("#").uppercase())
                }
                var hexError by remember { mutableStateOf(false) }

                // Header with live active color pill badge
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("App color scheme & spectrum", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Curated AMOLED palettes, full spectrum & custom HEX",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            )
                            Text(
                                text = activeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 1. Circular Color Options at the Top
                Text(
                    "ACCENT PALETTES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    letterSpacing = 1.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    paletteOptions.forEach { (hex, name, presetScheme) ->
                        val isSelected = when {
                            presetScheme != null && settings.themeScheme == presetScheme -> true
                            settings.themeScheme == AppThemeScheme.Custom && settings.customThemeHex.equals(hex, ignoreCase = true) -> true
                            else -> false
                        }
                        val swatchColor = parseHexColor(hex) ?: Color.White
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "swatchScale_$name",
                        )
                        val borderWidth by animateDpAsState(
                            targetValue = if (isSelected) 2.5.dp else 1.dp,
                            label = "swatchBorder_$name",
                        )

                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = borderWidth,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hexInput = hex.removePrefix("#")
                                    currentHue = hexToHue(hex)
                                    if (presetScheme != null) {
                                        onChange(settings.copy(themeScheme = presetScheme, customThemeHex = hex))
                                    } else {
                                        onChange(settings.copy(themeScheme = AppThemeScheme.Custom, customThemeHex = hex))
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                // Concentric color dot selection indicator (no tick icon)
                                Box(
                                    Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(if (hex == "#E2E8F0") Color(0xFF1E293B) else Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 2. Full Color Spectrum Slider for Custom Color Selection
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "COLOR SPECTRUM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                        letterSpacing = 1.sp,
                    )
                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = "${currentHue.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }

                ColorSpectrumBar(
                    currentHue = currentHue,
                    onHueChange = { newHue ->
                        currentHue = newHue
                        val newHex = hueToHex(newHue)
                        hexInput = newHex.removePrefix("#")
                        onChange(settings.copy(themeScheme = AppThemeScheme.Custom, customThemeHex = newHex))
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                Spacer(Modifier.height(4.dp))

                // 3. Custom Hash Code Field (HEX)
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val cleaned = input.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.take(6).uppercase()
                        hexInput = cleaned
                        if (cleaned.length == 6) {
                            val fullHex = "#$cleaned"
                            val parsed = parseHexColor(fullHex)
                            if (parsed != null) {
                                hexError = false
                                currentHue = hexToHue(fullHex)
                                onChange(settings.copy(themeScheme = AppThemeScheme.Custom, customThemeHex = fullHex))
                            } else {
                                hexError = true
                            }
                        } else {
                            hexError = cleaned.isNotEmpty() && cleaned.length < 6
                        }
                    },
                    label = { Text("Custom HEX hash code") },
                    prefix = { Text("#", fontWeight = FontWeight.Bold, color = scheme.primary) },
                    placeholder = { Text("B9F23C") },
                    trailingIcon = {
                        val parsed = parseHexColor("#$hexInput")
                        if (parsed != null) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    },
                    isError = hexError,
                    supportingText = if (hexError) {
                        { Text("Enter a 6-digit hex code (e.g. B9F23C, 38BDF8, FF5722)") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )

                // 4. Dynamic Accent Feature Touch: Library Icon & UI Sync Note
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "Library icon, tuning dials, waveform meters, and app launcher icon dynamically synchronize with your chosen accent color.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // -------------------------------------------------------------------------
            // Section 3: Advanced Noise Suppression & Audio Filter
            // -------------------------------------------------------------------------
            SectionHeader(
                icon = Icons.Rounded.NoiseAware,
                title = "Noise suppression & audio filter",
                onInfoClick = { activeGuide = audioFilterGuide },
            )

            SettingCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Noise gate threshold", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Rejects ambient room sound and background hum",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${(settings.noiseGate * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineSmallEmphasized.tabular(),
                        color = scheme.primary,
                    )
                }

                TunrSlider(
                    value = settings.noiseGate,
                    onValueChange = { onChange(settings.copy(noiseGate = (it * 20).roundToInt() / 20f)) },
                    valueRange = 0f..1f,
                )
            }

            // Harmonic Speech & Chatter Filter - M3 color indication, thumbContent = null
            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerHigh) {
                ListItem(
                    headlineContent = { Text("Harmonic speech & noise filter", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = {
                        Text(
                            "Distinguishes instrument vibrations from voices and crowd chatter using autocorrelation periodicity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.harmonicFilter,
                            onCheckedChange = null,
                            thumbContent = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.toggleable(
                        value = settings.harmonicFilter,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(harmonicFilter = it)) },
                    ),
                )
            }

            // Subsonic 35Hz Rumble Filter - M3 color indication, thumbContent = null
            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerHigh) {
                ListItem(
                    headlineContent = { Text("Subsonic rumble filter (35 Hz)", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = {
                        Text(
                            "Blocks phone handling noise, breathing drafts, and DC microphone drift",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.subsonicFilter,
                            onCheckedChange = null,
                            thumbContent = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.toggleable(
                        value = settings.subsonicFilter,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(subsonicFilter = it)) },
                    ),
                )
            }

            // -------------------------------------------------------------------------
            // Section 4: Metronome Audio & Tactile
            // -------------------------------------------------------------------------
            SectionHeader(
                icon = Icons.Rounded.Speed,
                title = "Metronome audio & tactile",
                onInfoClick = { activeGuide = metronomeGuide },
            )

            SettingCard {
                Text("Sound profile", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetronomeSound.entries.forEach { sound ->
                        val isSelected = settings.metronomeSound == sound
                        FilterChip(
                            selected = isSelected,
                            onClick = { onChange(settings.copy(metronomeSound = sound)) },
                            label = {
                                Text(
                                    sound.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            leadingIcon = null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primaryContainer,
                                selectedLabelColor = scheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                Text(
                    settings.metronomeSound.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            SettingCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Metronome volume", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Balance against acoustic instruments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${(settings.metronomeVolume * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineSmallEmphasized.tabular(),
                        color = scheme.primary,
                    )
                }

                TunrSlider(
                    value = settings.metronomeVolume,
                    onValueChange = { onChange(settings.copy(metronomeVolume = (it * 20).roundToInt() / 20f)) },
                    valueRange = 0.1f..1.0f,
                )
            }

            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerHigh) {
                ListItem(
                    headlineContent = { Text("Metronome beat haptics", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Tactile pulses on beats with an accented downbeat") },
                    trailingContent = {
                        Switch(
                            checked = settings.metronomeHaptics,
                            onCheckedChange = null,
                            thumbContent = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.toggleable(
                        value = settings.metronomeHaptics,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(metronomeHaptics = it)) },
                    ),
                )
            }

            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerHigh) {
                ListItem(
                    headlineContent = { Text("Visual beat flash", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("High-contrast visual pulse on measure downbeats") },
                    trailingContent = {
                        Switch(
                            checked = settings.metronomeFlash,
                            onCheckedChange = null,
                            thumbContent = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.toggleable(
                        value = settings.metronomeFlash,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(metronomeFlash = it)) },
                    ),
                )
            }

            // -------------------------------------------------------------------------
            // Section 5: Display & Hardware
            // -------------------------------------------------------------------------
            SectionHeader(
                icon = Icons.Rounded.Visibility,
                title = "Display & hardware",
                onInfoClick = { activeGuide = hardwareGuide },
            )

            SettingCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Screen timeout", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = scheme.primaryContainer,
                    ) {
                        Text(
                            settings.screenTimeout.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ScreenTimeout.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = settings.screenTimeout == option,
                            onClick = { onChange(settings.copy(screenTimeout = option, keepScreenAwake = option != ScreenTimeout.SystemDefault)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ScreenTimeout.entries.size),
                            icon = {},
                            colors = segmentedButtonColors,
                            label = {
                                Text(
                                    option.label,
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    fontWeight = if (settings.screenTimeout == option) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                Text(
                    if (settings.screenTimeout == ScreenTimeout.AlwaysOn) {
                        "Keeps the screen awake indefinitely while tuning or practicing."
                    } else if (settings.screenTimeout == ScreenTimeout.SystemDefault) {
                        "Follows the system display sleep timeout to save battery."
                    } else {
                        "Keeps screen awake during activity, sleeps after ${settings.screenTimeout.minutes} minutes of inactivity."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            SettingCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Energy saver dimming", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (settings.screenDimTimeout == ScreenDimTimeout.Never) scheme.surfaceContainerHighest else scheme.primaryContainer,
                    ) {
                        Text(
                            if (settings.screenDimTimeout == ScreenDimTimeout.Never) "OFF" else "DIM IN ${settings.screenDimTimeout.label}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.screenDimTimeout == ScreenDimTimeout.Never) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ScreenDimTimeout.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = settings.screenDimTimeout == option,
                            onClick = { onChange(settings.copy(screenDimTimeout = option)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ScreenDimTimeout.entries.size),
                            icon = {},
                            colors = segmentedButtonColors,
                            label = {
                                Text(
                                    option.label,
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    fontWeight = if (settings.screenDimTimeout == option) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
                Text(
                    "Dims display to 4% brightness after inactivity to conserve AMOLED power and eliminate screen burn-in. Tapping the screen or plucking an instrument string immediately restores full brightness.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerHigh) {
                ListItem(
                    headlineContent = { Text("Haptic pitch lock", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Vibrates when the note enters the in-tune window") },
                    trailingContent = {
                        Switch(
                            checked = settings.haptics,
                            onCheckedChange = null,
                            thumbContent = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.toggleable(
                        value = settings.haptics,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(haptics = it)) },
                    ),
                )
            }

            // Sound Pitch Lock Confirmation Switch
            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerHigh) {
                ListItem(
                    headlineContent = { Text("Sound pitch lock chime", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Plays a gentle crystal harmonic chime when the note locks in tune") },
                    trailingContent = {
                        Switch(
                            checked = settings.soundFeedback,
                            onCheckedChange = null,
                            thumbContent = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.toggleable(
                        value = settings.soundFeedback,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(soundFeedback = it)) },
                    ),
                )
            }

            // Zero-recording audio privacy pledge card
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = scheme.primary,
                )
                Column {
                    Text("Zero-recording audio privacy", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "TUNR performs all pitch calculations exclusively in volatile RAM. No audio files or recordings are ever created, cached, or transmitted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Information Guide Dialog triggered by section question icons
    // -------------------------------------------------------------------------
    activeGuide?.let { guide ->
        AlertDialog(
            onDismissRequest = { activeGuide = null },
            icon = {
                Icon(
                    guide.icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            },
            title = {
                Text(
                    guide.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    guide.guideItems.forEach { (label, detail) ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeGuide = null }) {
                    Text("Got it")
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = scheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    onInfoClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = "Information about $title",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun ColorSpectrumBar(
    currentHue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000), // Red 0°
            Color(0xFFFFFF00), // Yellow 60°
            Color(0xFF00FF00), // Green 120°
            Color(0xFF00FFFF), // Cyan 180°
            Color(0xFF0000FF), // Blue 240°
            Color(0xFFFF00FF), // Magenta 300°
            Color(0xFFFF0000), // Red 360°
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onHueChange(ratio * 360f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                    onHueChange(ratio * 360f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(rainbowColors)
            )
            val thumbX = (currentHue / 360f).coerceIn(0f, 1f) * size.width
            val thumbCenter = Offset(thumbX, size.height / 2f)
            val hsv = floatArrayOf((currentHue % 360f + 360f) % 360f, 0.85f, 0.95f)
            val liveColor = Color(AndroidColor.HSVToColor(hsv))

            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = 14.dp.toPx(),
                center = thumbCenter,
            )
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = thumbCenter,
            )
            drawCircle(
                color = liveColor,
                radius = 8.5.dp.toPx(),
                center = thumbCenter,
            )
        }
    }
}

private fun hexToHue(hex: String): Float {
    val clean = hex.trim().removePrefix("#")
    return try {
        val colorInt = AndroidColor.parseColor("#$clean")
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(colorInt, hsv)
        hsv[0]
    } catch (e: Exception) {
        80f
    }
}

private fun hueToHex(hue: Float): String {
    val hsv = floatArrayOf((hue % 360f + 360f) % 360f, 0.85f, 0.95f)
    val colorInt = AndroidColor.HSVToColor(hsv)
    return String.format(Locale.US, "#%06X", 0xFFFFFF and colorInt)
}

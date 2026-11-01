package com.knownassurajit.tunr_tool.app.ui.tuner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.knownassurajit.tunr_tool.app.model.PitchReading
import com.knownassurajit.tunr_tool.app.model.Precision
import com.knownassurajit.tunr_tool.app.model.SettingsState
import com.knownassurajit.tunr_tool.app.model.TuneStatus
import com.knownassurajit.tunr_tool.app.model.TunerState
import com.knownassurajit.tunr_tool.app.model.TuningString
import com.knownassurajit.tunr_tool.app.model.status
import com.knownassurajit.tunr_tool.app.theme.LocalReducedMotion
import com.knownassurajit.tunr_tool.app.theme.TunrMotion
import com.knownassurajit.tunr_tool.app.theme.tabular
import com.knownassurajit.tunr_tool.app.ui.components.toComposePath
import kotlin.math.abs

@Composable
fun TunerScreen(
    state: TunerState,
    settings: SettingsState,
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    onAutoDetect: (Boolean) -> Unit,
    onSelectString: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = state.status()
    val haptics = LocalHapticFeedback.current
    val soundPlayer = remember { com.knownassurajit.tunr_tool.app.audio.InTuneSoundPlayer() }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }

    LaunchedEffect(status == TuneStatus.InTune) {
        if (status == TuneStatus.InTune) {
            if (settings.haptics) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            if (settings.soundFeedback) {
                soundPlayer.play()
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            // Tablets, foldables, phones in landscape on a stand: dial left, strings right.
            val dialSize = minOf(maxWidth * 0.40f, maxHeight * 0.50f, 260.dp).coerceAtLeast(180.dp)
            Row(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { TunerHero(state, settings, micGranted, onRequestMic, dialSize) }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) { StringsSection(state, settings.a4Hz, onAutoDetect, onSelectString) }
            }
        } else {
            // Unified portrait distribution: dynamically proportioned dial and space distribution
            val dialSize = (maxHeight * 0.28f).coerceIn(160.dp, 230.dp)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = if (state.tuning.strings.isEmpty()) Arrangement.Center else Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TunerHero(state, settings, micGranted, onRequestMic, dialSize)
                if (state.tuning.strings.isNotEmpty()) {
                    StringsSection(state, settings.a4Hz, onAutoDetect, onSelectString)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Hero: dial + direction + meter + numbers
// ---------------------------------------------------------------------------------------

@Composable
private fun TunerHero(
    state: TunerState,
    settings: SettingsState,
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    dialSize: Dp,
) {
    val reading = state.reading
    val status = state.status()
    val fallbackTarget = state.tuning.strings
        .firstOrNull { it.number == state.activeString }?.hz(settings.a4Hz)

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (micGranted) {
            TuningDial(reading, status, chromatic = state.tuning.strings.isEmpty(), dialSize = dialSize)
        } else {
            MicPrompt(onRequestMic)
        }
        CentsMeter(reading?.cents, settings.precision.lockCents)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Readout("Target", (reading?.targetHz ?: fallbackTarget)?.let { "%.1f Hz".format(it) })
            Readout("Deviation", reading?.let { signed(it.cents, settings.precision) })
            Readout("Detected", reading?.let { "%.1f Hz".format(it.detectedHz) })
        }
    }
}

/**
 * The hero dial. A cookie-shaped dial that turns with pitch deviation and morphs into a
 * perfect circle when the note locks. State is never colour-only:
 * the label and arrow clearly indicate "Tune up", "Tune down" or "In tune".
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TuningDial(reading: PitchReading?, status: TuneStatus, chromatic: Boolean, dialSize: Dp) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val reduced = LocalReducedMotion.current

    val (container, content) = when (status) {
        TuneStatus.Idle -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        TuneStatus.Far -> scheme.errorContainer to scheme.onErrorContainer
        TuneStatus.Near -> scheme.secondaryContainer to scheme.onSecondaryContainer
        TuneStatus.InTune -> scheme.primary to scheme.onPrimary
    }
    val containerColor by animateColorAsState(
        container, animationSpec = motion.defaultEffectsSpec(), label = "dialContainer",
    )
    val contentColor by animateColorAsState(
        content, animationSpec = motion.defaultEffectsSpec(), label = "dialContent",
    )
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Circle) }
    val morphProgress by animateFloatAsState(
        if (status == TuneStatus.InTune) 1f else 0f,
        animationSpec = if (reduced) snap() else motion.defaultSpatialSpec(), label = "dialMorph",
    )
    // +/-50 cents turns the cookie by one lobe (40 degrees)
    val rotation by animateFloatAsState(
        ((reading?.cents ?: 0f).coerceIn(-50f, 50f) / 50f) * 40f,
        animationSpec = TunrMotion.dialRotation(reduced), label = "dialRotation",
    )
    // The dial is already "large text": pin it to a physical size so 200% font scale cannot overflow the shape.
    val noteSize = with(LocalDensity.current) { (if (dialSize < 200.dp) 56.dp else 68.dp).toSp() }
    val noteLine = with(LocalDensity.current) { (if (dialSize < 200.dp) 60.dp else 72.dp).toSp() }
    val spoken = remember(reading?.note, reading?.octave, status, reading?.cents?.let { (it / 5f).toInt() }) {
        when {
            reading == null -> if (chromatic) "Play a note" else "Pluck a string"
            else -> "${reading.note}${reading.octave}, " + when (status) {
                TuneStatus.InTune -> "in tune"
                else -> "%d cents %s, tune %s".format(
                    abs(reading.cents).toInt(),
                    if (reading.cents < 0f) "flat" else "sharp",
                    if (reading.cents < 0f) "up" else "down",
                )
            }
        }
    }

    Box(
        Modifier
            .size(dialSize)
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .drawBehind {
                rotate(rotation, center) {
                    drawPath(morph.toComposePath(morphProgress, size), containerColor)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (reading == null) {
            Text(
                if (chromatic) "Play a note" else "Pluck a string",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    noteLabel(reading.note, reading.octave),
                    style = MaterialTheme.typography.displayLargeEmphasized
                        .copy(fontSize = noteSize, lineHeight = noteLine),
                    color = contentColor,
                )
                Row(
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (status == TuneStatus.InTune) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(contentColor)
                        )
                        Text(
                            "In tune",
                            style = MaterialTheme.typography.titleSmall,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        val icon = if (reading.cents < 0f) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
                        val label = if (reading.cents < 0f) "Tune up" else "Tune down"
                        Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = contentColor)
                        Text(label, style = MaterialTheme.typography.titleSmall, color = contentColor)
                    }
                }
            }
        }
    }
}

/** Needle + lock window. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CentsMeter(cents: Float?, lockCents: Float) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val reduced = LocalReducedMotion.current

    val needle by animateFloatAsState(
        (cents ?: 0f).coerceIn(-50f, 50f),
        animationSpec = TunrMotion.needle(reduced), label = "needle",
    )
    val needleColor by animateColorAsState(
        when {
            cents == null -> scheme.outline
            abs(cents) <= lockCents -> scheme.primary
            abs(cents) <= 10f -> scheme.secondary
            else -> scheme.error
        },
        animationSpec = motion.defaultEffectsSpec(), label = "needleColor",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clearAndSetSemantics {
                // Explorable by touch, but not a live region: updating 20x/s would flood TalkBack.
                stateDescription = when {
                    cents == null -> "No signal"
                    abs(cents) <= lockCents -> "In tune"
                    else -> "%d cents %s".format(abs(cents).toInt(), if (cents < 0f) "flat" else "sharp")
                }
            }
            .drawBehind {
                val pad = 6.dp.toPx()
                val usable = size.width - 2 * pad
                fun xAt(c: Float) = pad + (c + 50f) / 100f * usable

                val trackH = 10.dp.toPx()
                val top = (size.height - trackH) / 2
                val radius = CornerRadius(trackH / 2)

                drawRoundRect(scheme.surfaceContainerHigh, Offset(0f, top), Size(size.width, trackH), radius)

                val lockW = usable * (lockCents * 2f / 100f)
                drawRoundRect(
                    scheme.primary.copy(alpha = 0.28f),
                    Offset(xAt(0f) - lockW / 2, top), Size(lockW, trackH), radius,
                )
                for (i in -5..5) {
                    if (i != 0) drawCircle(scheme.outline, 1.2.dp.toPx(), Offset(xAt(i * 10f), size.height / 2))
                }

                val needleW = 5.dp.toPx()
                drawRoundRect(
                    needleColor,
                    Offset(xAt(needle) - needleW / 2, 0f), Size(needleW, size.height),
                    CornerRadius(needleW / 2),
                )
            },
    )
}

@Composable
private fun Readout(label: String, value: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value ?: "—",
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MicPrompt(onRequest: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Rounded.Mic, contentDescription = null,
                modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary,
            )
            Text("Microphone access needed", style = MaterialTheme.typography.titleMedium)
            Text(
                "TUNR listens to your instrument to find the pitch. Audio is analysed in memory and never stored.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRequest, shapes = ButtonDefaults.shapes()) { Text("Allow microphone") }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Strings Grid
// ---------------------------------------------------------------------------------------

@Composable
private fun StringsSection(
    state: TunerState,
    a4Hz: Float,
    onAutoDetect: (Boolean) -> Unit,
    onSelectString: (Int) -> Unit,
) {
    val strings = state.tuning.strings
    if (strings.isEmpty()) return // Chromatic mode has no fixed strings

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Strings", style = MaterialTheme.typography.titleSmall)
            FilterChip(
                selected = state.autoDetect,
                onClick = { onAutoDetect(!state.autoDetect) },
                label = { Text("Auto-detect") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }

        val count = strings.size
        val cols = when {
            count <= 3 -> count
            count == 4 -> 2
            count == 5 || count % 3 == 0 -> 3
            else -> 4
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            strings.chunked(cols).forEach { rowStrings ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowStrings.forEach { s ->
                        StringChip(
                            string = s,
                            a4Hz = a4Hz,
                            active = s.number == state.activeString,
                            tuned = s.number in state.tunedStrings,
                            onClick = { onSelectString(s.number) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(cols - rowStrings.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StringChip(
    string: TuningString,
    a4Hz: Float,
    active: Boolean,
    tuned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val container by animateColorAsState(
        if (active) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = motion.defaultEffectsSpec(), label = "chipContainer",
    )
    val corner by animateDpAsState(
        if (active) 12.dp else 24.dp,
        animationSpec = motion.defaultSpatialSpec(), label = "chipCorner",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { selected = active },
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "String ${string.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.72f),
                )
                if (tuned) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(scheme.primary)
                    )
                }
            }
            Text(
                noteLabel(string.note, string.octave, octaveSize = 12.sp),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "%.1f Hz".format(string.hz(a4Hz)),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = LocalContentColor.current.copy(alpha = 0.72f),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------------------

private fun noteLabel(
    note: String,
    octave: Int,
    octaveSize: androidx.compose.ui.unit.TextUnit = 24.sp,
): AnnotatedString = buildAnnotatedString {
    append(note)
    withStyle(SpanStyle(fontSize = octaveSize)) { append(octave.toString()) }
}

/** Signed cent deviation string following precision mode decimal places. */
private fun signed(cents: Float, precision: Precision): String =
    "%+.${precision.decimals}f¢".format(cents).replace('-', '−')

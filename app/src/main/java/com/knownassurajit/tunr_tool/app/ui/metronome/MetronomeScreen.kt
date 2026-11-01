package com.knownassurajit.tunr_tool.app.ui.metronome

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.MetronomeState
import com.knownassurajit.tunr_tool.app.model.SettingsState
import com.knownassurajit.tunr_tool.app.model.TimeSignature
import com.knownassurajit.tunr_tool.app.theme.tabular
import com.knownassurajit.tunr_tool.app.ui.components.TunrSlider
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val SIGNATURES = listOf(
    TimeSignature(2, 4), TimeSignature(3, 4), TimeSignature(4, 4), TimeSignature(6, 4),
)

data class TempoPreset(val name: String, val bpm: Int, val description: String)

private val TEMPO_PRESETS = listOf(
    TempoPreset("Grave", 35, "Very slow and solemn"),
    TempoPreset("Largo", 50, "Broad and slow"),
    TempoPreset("Larghetto", 62, "Rather broadly"),
    TempoPreset("Adagio", 72, "Slow with great expression"),
    TempoPreset("Andante", 92, "At a walking pace"),
    TempoPreset("Moderato", 112, "Moderately"),
    TempoPreset("Allegro", 136, "Fast, quickly, and bright"),
    TempoPreset("Vivace", 168, "Lively and fast"),
    TempoPreset("Presto", 188, "Very, very fast"),
    TempoPreset("Prestissimo", 215, "As fast as possible"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(
    state: MetronomeState,
    settings: SettingsState,
    onBpm: (Int) -> Unit,
    onNudge: (Int) -> Unit,
    onTap: () -> Unit,
    onSignature: (TimeSignature) -> Unit,
    onToggleAccent: (Int) -> Unit,
    onPlaying: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    DisposableEffect(state.playing, settings.keepScreenAwake) {
        view.keepScreenOn = state.playing || settings.keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }

    // Haptic feedback synchronization on metronome beat tick
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.beat, state.playing) {
        if (state.playing && settings.metronomeHaptics) {
            val accent = state.accents.getOrElse(state.beat) {
                if (state.beat == 0) BeatAccent.ACCENT else BeatAccent.NORMAL
            }
            when (accent) {
                BeatAccent.ACCENT -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                BeatAccent.NORMAL -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                BeatAccent.MUTE -> Unit
            }
        }
    }

    // Dialog state for interactive tempo selection and direct input
    var showTempoPresetsSheet by remember { mutableStateOf(false) }
    var showBpmInputDialog by remember { mutableStateOf(false) }
    var showCustomSignatureDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            // Tablet / foldable / landscape mode (2 columns)
            val wheelSize = minOf(maxWidth * 0.28f, maxHeight * 0.36f, 190.dp).coerceIn(120.dp, 190.dp)
            Row(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TempoCard(
                    state = state,
                    settings = settings,
                    onBpm = onBpm,
                    onOpenPresets = { showTempoPresetsSheet = true },
                    onOpenBpmInput = { showBpmInputDialog = true },
                    onToggleAccent = onToggleAccent,
                    wheelSize = wheelSize,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SignatureGroup(
                        selected = state.signature,
                        onSelect = onSignature,
                        onOpenCustom = { showCustomSignatureDialog = true }
                    )
                    TempoControls(state, onBpm, onNudge, onTap)
                    PlayButton(state.playing, onPlaying)
                }
            }
        } else {
            // Unified portrait distribution: fits seamlessly into viewport without scrolling
            val wheelSize = (maxHeight * 0.22f).coerceIn(145.dp, 190.dp)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SignatureGroup(
                    selected = state.signature,
                    onSelect = onSignature,
                    onOpenCustom = { showCustomSignatureDialog = true }
                )
                TempoCard(
                    state = state,
                    settings = settings,
                    onBpm = onBpm,
                    onOpenPresets = { showTempoPresetsSheet = true },
                    onOpenBpmInput = { showBpmInputDialog = true },
                    onToggleAccent = onToggleAccent,
                    wheelSize = wheelSize,
                )
                TempoControls(state, onBpm, onNudge, onTap)
                PlayButton(state.playing, onPlaying)
            }
        }
    }

    // Modal Sheet for Italian Tempo Presets
    if (showTempoPresetsSheet) {
        TempoPresetsSheet(
            currentBpm = state.bpm,
            onSelect = { presetBpm ->
                onBpm(presetBpm)
                showTempoPresetsSheet = false
            },
            onDismiss = { showTempoPresetsSheet = false }
        )
    }

    // Dialog for Direct Numerical BPM Input
    if (showBpmInputDialog) {
        BpmInputDialog(
            currentBpm = state.bpm,
            onConfirm = { newBpm ->
                onBpm(newBpm)
                showBpmInputDialog = false
            },
            onDismiss = { showBpmInputDialog = false }
        )
    }

    // Dialog for Custom Bar / Time Signature Configuration
    if (showCustomSignatureDialog) {
        CustomBarDialog(
            currentSignature = state.signature,
            onConfirm = { customSig ->
                onSignature(customSig)
                showCustomSignatureDialog = false
            },
            onDismiss = { showCustomSignatureDialog = false }
        )
    }
}

// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SignatureGroup(
    selected: TimeSignature,
    onSelect: (TimeSignature) -> Unit,
    onOpenCustom: () -> Unit,
) {
    val isCustom = selected !in SIGNATURES
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        SIGNATURES.forEachIndexed { index, sig ->
            ToggleButton(
                checked = !isCustom && sig == selected,
                onCheckedChange = { onSelect(sig) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(
                    sig.label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        ToggleButton(
            checked = isCustom,
            onCheckedChange = { onOpenCustom() },
            modifier = Modifier
                .weight(1.15f)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
        ) {
            Text(
                text = if (isCustom) selected.label else "Custom",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TempoCard(
    state: MetronomeState,
    settings: SettingsState,
    onBpm: (Int) -> Unit,
    onOpenPresets: () -> Unit,
    onOpenBpmInput: () -> Unit,
    onToggleAccent: (Int) -> Unit,
    wheelSize: Dp,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val flashActive = settings.metronomeFlash && state.playing && state.beat == 0

    val cardBorderColor by animateColorAsState(
        targetValue = if (flashActive) scheme.primary else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "flashBorder"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (cardBorderColor != Color.Transparent) {
                    Modifier.border(2.dp, cardBorderColor, RoundedCornerShape(32.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Interactive Italian Tempo Marking Chip
            Surface(
                onClick = onOpenPresets,
                shape = CircleShape,
                color = scheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        tempoMarking(state.bpm),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = "Select tempo preset",
                        modifier = Modifier.size(18.dp),
                        tint = scheme.primary
                    )
                }
            }

            // Interactive Rotary Jog Wheel with synchronized tempo mapping
            TempoJogWheel(
                bpm = state.bpm,
                wheelSize = wheelSize,
                onBpmChange = onBpm,
                onCenterClick = onOpenBpmInput
            )

            // Ergonomic, legible affordance pill: Tap to edit & Drag wheel guidance
            Surface(
                onClick = onOpenBpmInput,
                shape = CircleShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = scheme.primary
                    )
                    Text(
                        "Tap to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.outline.copy(alpha = 0.6f)
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.RotateRight,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = scheme.primary
                    )
                    Text(
                        "Drag wheel",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Interactive Beat Markers with Accent / Normal / Mute toggle
            BeatRow(
                state = state,
                onToggleAccent = onToggleAccent
            )
        }
    }
}

/**
 * Interactive rotary jog wheel with mathematically unified tempo mapping.
 *
 * Moving the wheel updates BPM; moving the slider or buttons immediately rotates the wheel.
 */
@Composable
private fun TempoJogWheel(
    bpm: Int,
    wheelSize: Dp,
    onBpmChange: (Int) -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragDeltaAccumulator by remember { mutableFloatStateOf(0f) }
    var previousAngle by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    // Always reference the latest configured BPM and callback to prevent stale closure capture
    val currentBpm by rememberUpdatedState(bpm)
    val currentOnBpmChange by rememberUpdatedState(onBpmChange)

    // Direct mathematical tempo-to-angle mapping:
    // Every 1 BPM corresponds to 3.2 degrees of rotation, anchored around 120 BPM.
    // Animate smoothly so external updates (slider, nudge, presets) spin the wheel dynamically.
    val baseRotation by animateFloatAsState(
        targetValue = (bpm - 120) * 3.2f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "wheelRotation"
    )

    Box(
        modifier = modifier
            .size(wheelSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val diff = offset - center
                        previousAngle = Math.toDegrees(atan2(diff.y.toDouble(), diff.x.toDouble())).toFloat()
                        dragDeltaAccumulator = 0f
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val diff = change.position - center
                        val angle = Math.toDegrees(atan2(diff.y.toDouble(), diff.x.toDouble())).toFloat()
                        var delta = angle - previousAngle
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        previousAngle = angle

                        dragDeltaAccumulator += delta
                        val bpmStep = (dragDeltaAccumulator / 3.2f).toInt()
                        if (bpmStep != 0) {
                            val newBpm = (currentBpm + bpmStep).coerceIn(30, 240)
                            if (newBpm != currentBpm) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnBpmChange(newBpm)
                            }
                            dragDeltaAccumulator -= bpmStep * 3.2f
                        }
                    }
                )
            }
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 8.dp.toPx()
                val innerRadius = radius - 14.dp.toPx()

                // Circular outer track
                drawCircle(
                    color = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Rotary ticks (48 graduated markers)
                val tickCount = 48
                val totalRotation = baseRotation + dragDeltaAccumulator
                rotate(totalRotation, center) {
                    for (i in 0 until tickCount) {
                        val tickAngle = (i * 360f / tickCount) * (PI / 180f).toFloat()
                        val isMajor = i % 4 == 0
                        val tickInner = if (isMajor) innerRadius - 3.dp.toPx() else innerRadius
                        val start = Offset(
                            center.x + tickInner * cos(tickAngle),
                            center.y + tickInner * sin(tickAngle)
                        )
                        val end = Offset(
                            center.x + radius * cos(tickAngle),
                            center.y + radius * sin(tickAngle)
                        )
                        val color = if (isMajor) scheme.primary.copy(alpha = 0.85f) else scheme.onSurfaceVariant.copy(alpha = 0.35f)
                        val stroke = if (isMajor) 2.2.dp.toPx() else 1.dp.toPx()
                        drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
                    }

                    // Jog indicator pip
                    val pipDistance = radius - 7.dp.toPx()
                    drawCircle(
                        color = scheme.primary,
                        radius = 4.dp.toPx(),
                        center = Offset(center.x, center.y - pipDistance)
                    )
                }

                // Inner stationary instrument dish and hairline boundary
                val dishRadius = innerRadius - 4.dp.toPx()
                drawCircle(
                    color = scheme.surfaceContainerHigh.copy(alpha = 0.45f),
                    radius = dishRadius,
                    center = center
                )
                drawCircle(
                    color = scheme.outlineVariant.copy(alpha = 0.35f),
                    radius = dishRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Interactive central BPM readout — tapping opens direct input
        val dishSize = (wheelSize - 52.dp).coerceAtLeast(88.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(dishSize)
                .clip(CircleShape)
                .clickable(
                    onClick = onCenterClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                )
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = "$bpm",
                style = MaterialTheme.typography.displayLargeEmphasized
                    .copy(
                        fontSize = if (wheelSize < 160.dp) 38.sp else 46.sp,
                        lineHeight = if (wheelSize < 160.dp) 38.sp else 46.sp,
                    )
                    .tabular(),
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "BPM",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
                color = scheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Interactive beat markers supporting custom accent patterns.
 * Tapping any beat dot cycles: ACCENT -> NORMAL -> MUTE.
 * Dynamically scales and wraps dots for custom meters up to 16 beats.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeatRow(
    state: MetronomeState,
    onToggleAccent: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val totalBeats = state.signature.beats

    val (slotSize, dotSizeAccent, dotSizeNormal) = when {
        totalBeats <= 6 -> Triple(32.dp, 26.dp, 20.dp)
        totalBeats <= 8 -> Triple(28.dp, 22.dp, 17.dp)
        totalBeats <= 12 -> Triple(24.dp, 19.dp, 14.dp)
        else -> Triple(20.dp, 16.dp, 12.dp)
    }

    val maxPerRow = when {
        totalBeats <= 8 -> totalBeats
        totalBeats == 12 -> 6
        totalBeats <= 10 -> 5
        else -> 8
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = if (state.playing) {
                        "Beat ${state.beat + 1} of ${state.signature.beats}"
                    } else {
                        "${state.signature.beats} beats per measure"
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = maxPerRow
        ) {
            repeat(totalBeats) { index ->
                val active = state.playing && index == state.beat
                val accent = state.accents.getOrElse(index) {
                    if (index == 0) BeatAccent.ACCENT else BeatAccent.NORMAL
                }

                val scale by animateFloatAsState(
                    if (active) 1.25f else 1f,
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    label = "beatScale",
                )

                val dotSize = if (accent == BeatAccent.ACCENT) dotSizeAccent else dotSizeNormal

                Box(
                    modifier = Modifier
                        .size(slotSize)
                        .clip(CircleShape)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAccent(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (accent) {
                        BeatAccent.ACCENT -> {
                            // Filled primary dot with animated active pop
                            Box(
                                Modifier
                                    .size(dotSize)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .background(if (active) scheme.primary else scheme.primary.copy(alpha = 0.85f), CircleShape)
                            )
                        }
                        BeatAccent.NORMAL -> {
                            // Regular sub-beat dot
                            Box(
                                Modifier
                                    .size(dotSize)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .background(if (active) scheme.primary.copy(alpha = 0.85f) else scheme.surfaceContainerHighest, CircleShape)
                            )
                        }
                        BeatAccent.MUTE -> {
                            // Muted / silent beat (hollow ring)
                            Box(
                                Modifier
                                    .size(dotSize)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .border(1.5.dp, if (active) scheme.primary else scheme.outlineVariant, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Tap dots to toggle Accent / Normal / Mute",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = scheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun TempoControls(
    state: MetronomeState,
    onBpm: (Int) -> Unit,
    onNudge: (Int) -> Unit,
    onTap: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TunrSlider(
            value = state.bpm.toFloat(),
            onValueChange = { onBpm(it.roundToInt()) },
            valueRange = 30f..240f,
            modifier = Modifier.semantics { stateDescription = "${state.bpm} beats per minute" },
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton("−5") { onNudge(-5) }
            StepButton("−1") { onNudge(-1) }
            TapTempoButton(onTap, Modifier.weight(1f))
            StepButton("+1") { onNudge(1) }
            StepButton("+5") { onNudge(5) }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    FilledTonalIconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.size(44.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium.tabular())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TapTempoButton(onTap: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onTap()
        },
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        shapes = ButtonDefaults.shapes()
    ) {
        Icon(Icons.Rounded.TouchApp, contentDescription = null, Modifier.size(16.dp))
        Text(
            "Tap",
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Expressive morphing play button with ergonomic touch target. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayButton(playing: Boolean, onPlaying: (Boolean) -> Unit) {
    val corner by animateDpAsState(
        if (playing) 24.dp else 40.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), label = "playCorner",
    )
    val haptics = LocalHapticFeedback.current
    FilledIconToggleButton(
        checked = playing,
        onCheckedChange = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onPlaying(it)
        },
        modifier = Modifier.size(76.dp),
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Stop metronome" else "Start metronome",
            modifier = Modifier.size(34.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------
// Interactive Modals: Tempo Presets & Direct BPM Input
// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TempoPresetsSheet(
    currentBpm: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "Standard tempo markings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(TEMPO_PRESETS) { preset ->
                    val isSelected = currentBpm == preset.bpm
                    ListItem(
                        headlineContent = {
                            Text(
                                preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        supportingContent = { Text(preset.description) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${preset.bpm} BPM",
                                    style = MaterialTheme.typography.titleMedium.tabular(),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(preset.bpm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BpmInputDialog(
    currentBpm: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentBpm.toString()) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set tempo (BPM)") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { str ->
                        val filtered = str.filter { it.isDigit() }.take(3)
                        textValue = filtered
                    },
                    label = { Text("30 to 240 BPM") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = textValue.toIntOrNull()?.coerceIn(30, 240) ?: currentBpm
                            onConfirm(parsed)
                            focusManager.clearFocus()
                        }
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Nudge Shortcuts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = {
                        val v = (textValue.toIntOrNull() ?: currentBpm) - 10
                        textValue = v.coerceIn(30, 240).toString()
                    }) { Text("−10") }

                    FilledTonalButton(onClick = {
                        textValue = "120"
                    }) { Text("120") }

                    FilledTonalButton(onClick = {
                        val v = (textValue.toIntOrNull() ?: currentBpm) + 10
                        textValue = v.coerceIn(30, 240).toString()
                    }) { Text("+10") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = textValue.toIntOrNull()?.coerceIn(30, 240) ?: currentBpm
                onConfirm(parsed)
            }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomBarDialog(
    currentSignature: TimeSignature,
    onConfirm: (TimeSignature) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var tempBeats by remember { mutableIntStateOf(currentSignature.beats.coerceIn(1, 16)) }
    var tempUnit by remember {
        mutableIntStateOf(
            if (currentSignature.unit in listOf(2, 4, 8, 16)) currentSignature.unit else 4
        )
    }

    val quickPresets = remember {
        listOf(
            TimeSignature(5, 4),
            TimeSignature(7, 4),
            TimeSignature(5, 8),
            TimeSignature(6, 8),
            TimeSignature(7, 8),
            TimeSignature(9, 8),
            TimeSignature(12, 8),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm(TimeSignature(tempBeats, tempUnit))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                )
            ) {
                Text("Apply", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Custom Bar / Meter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Hero Preview of Current Signature Selection
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$tempBeats / $tempUnit",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ).tabular(),
                            color = scheme.primary,
                        )
                        val unitDesc = when (tempUnit) {
                            2 -> "half notes"
                            4 -> "quarter notes"
                            8 -> "eighth notes"
                            16 -> "sixteenth notes"
                            else -> "notes"
                        }
                        Text(
                            text = "$tempBeats beats per measure ($unitDesc)",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Beats per measure stepper (1 to 16)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "BEATS PER MEASURE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = scheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(scheme.surfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (tempBeats > 1) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tempBeats--
                                }
                            },
                            enabled = tempBeats > 1,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$tempBeats",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold).tabular(),
                                color = scheme.onSurface,
                            )
                            Text(
                                text = if (tempBeats == 1) "beat" else "beats",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = scheme.onSurfaceVariant
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                if (tempBeats < 16) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tempBeats++
                                }
                            },
                            enabled = tempBeats < 16,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Beat Unit / Note Value
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "BEAT NOTE DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = scheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(2, 4, 8, 16).forEach { u ->
                            val isSelected = tempUnit == u
                            FilledTonalButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tempUnit = u
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) scheme.primary else scheme.surfaceContainerHigh,
                                    contentColor = if (isSelected) scheme.onPrimary else scheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "/$u",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Quick Presets for Odd and Compound Meters
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "POPULAR PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = scheme.primary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        quickPresets.forEach { preset ->
                            val isSelected = tempBeats == preset.beats && tempUnit == preset.unit
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tempBeats = preset.beats
                                    tempUnit = preset.unit
                                },
                                label = {
                                    Text(
                                        preset.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = scheme.primaryContainer,
                                    selectedLabelColor = scheme.onPrimaryContainer,
                                )
                            )
                        }
                    }
                }
            }
        }
    )
}

private fun tempoMarking(bpm: Int): String = when {
    bpm < 40 -> "Grave"
    bpm < 60 -> "Largo"
    bpm < 66 -> "Larghetto"
    bpm < 76 -> "Adagio"
    bpm < 108 -> "Andante"
    bpm <= 120 -> "Moderato"
    bpm < 156 -> "Allegro"
    bpm < 176 -> "Vivace"
    bpm < 200 -> "Presto"
    else -> "Prestissimo"
}

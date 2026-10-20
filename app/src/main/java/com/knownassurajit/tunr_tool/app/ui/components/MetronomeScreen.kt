package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.theme.AmoledBlack
import com.knownassurajit.tunr_tool.app.theme.BorderMedium
import com.knownassurajit.tunr_tool.app.theme.BorderSubtle
import com.knownassurajit.tunr_tool.app.theme.CardBackground
import com.knownassurajit.tunr_tool.app.theme.CardBorder
import com.knownassurajit.tunr_tool.app.theme.LockContainer
import com.knownassurajit.tunr_tool.app.theme.LockEmerald
import com.knownassurajit.tunr_tool.app.theme.LockGlow
import com.knownassurajit.tunr_tool.app.theme.LockMint
import com.knownassurajit.tunr_tool.app.theme.MatteBackground
import com.knownassurajit.tunr_tool.app.theme.MatteBlue
import com.knownassurajit.tunr_tool.app.theme.MatteBlueContainer
import com.knownassurajit.tunr_tool.app.theme.MatteSurface
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceContainer
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceLow
import com.knownassurajit.tunr_tool.app.theme.PillShape
import com.knownassurajit.tunr_tool.app.theme.TextPrimary
import com.knownassurajit.tunr_tool.app.theme.TextSecondary
import com.knownassurajit.tunr_tool.app.theme.TextTertiary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist, high-precision Metronome with interactive Rotary Jog Wheel.
 * Features:
 * - Tactile rotary wheel for angular tempo adjustment
 * - Dynamic tempo marking (Largo to Prestissimo)
 * - Tap Tempo averaging engine
 * - Time signature presets (2/4, 3/4, 4/4, 6/8)
 * - Synchronized beat visualization dots
 * - AMOLED matte color cohesion
 */
@Composable
fun MetronomeScreen(
    bpm: Int,
    beatsPerMeasure: Int,
    isPlaying: Boolean,
    currentBeat: Int,
    onBpmChanged: (Int) -> Unit,
    onBeatsPerMeasureChanged: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tap Tempo state tracker
    val tapTimestamps = remember { mutableStateListOf<Long>() }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val tempoName = remember(bpm) {
        when {
            bpm < 60 -> "Largo"
            bpm in 60..65 -> "Larghetto"
            bpm in 66..76 -> "Adagio"
            bpm in 77..108 -> "Andante"
            bpm in 109..120 -> "Moderato"
            bpm in 121..168 -> "Allegro"
            bpm in 169..200 -> "Presto"
            else -> "Prestissimo"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Time Signature Selector Bar
        TimeSignatureBar(
            selectedSignature = beatsPerMeasure,
            onSignatureSelected = onBeatsPerMeasureChanged
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Central Interactive Rotary Jog Wheel for Tempo
        TempoJogWheel(
            bpm = bpm,
            tempoName = tempoName,
            beatsPerMeasure = beatsPerMeasure,
            currentBeat = currentBeat,
            isPlaying = isPlaying,
            onBpmChanged = onBpmChanged,
            modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Quick Adjustment Steppers & Tap Tempo
        AdjustmentRow(
            bpm = bpm,
            onBpmChanged = onBpmChanged,
            onTapTempo = {
                val now = System.currentTimeMillis()
                lastTapTime = now
                tapTimestamps.add(now)
                if (tapTimestamps.size > 5) tapTimestamps.removeAt(0)
                if (tapTimestamps.size >= 2) {
                    val intervals = (1 until tapTimestamps.size).map { i ->
                        tapTimestamps[i] - tapTimestamps[i - 1]
                    }
                    val avgInterval = intervals.average()
                    if (avgInterval in 200.0..2000.0) {
                        val detectedBpm = (60_000.0 / avgInterval).toInt().coerceIn(30, 300)
                        onBpmChanged(detectedBpm)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Large Play / Pause Control Button
        PlayPauseButton(
            isPlaying = isPlaying,
            onToggle = onTogglePlay
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Interactive Rotary Jog Wheel for fluid, tactile tempo adjustment.
 * Turning clockwise increases BPM; turning counter-clockwise decreases BPM.
 */
@Composable
private fun TempoJogWheel(
    bpm: Int,
    tempoName: String,
    beatsPerMeasure: Int,
    currentBeat: Int,
    isPlaying: Boolean,
    onBpmChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var rotationAngleDegrees by remember { mutableFloatStateOf(0f) }
    var accumulatedDelta by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Visual ring pulse on beat
    val isDownbeat = currentBeat == 0 && isPlaying
    val beatPulseColor by animateColorAsState(
        targetValue = when {
            isDownbeat -> LockMint.copy(alpha = 0.4f)
            isPlaying -> MatteBlue.copy(alpha = 0.25f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 100),
        label = "beatPulseColor"
    )

    // Wheel dimensions
    val wheelSize = 260.dp

    Box(
        modifier = modifier
            .size(wheelSize)
            .pointerInput(Unit) {
                var prevAngle: Float? = null

                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val center = Offset(size.width / 2f, size.height / 2f)
                        prevAngle = atan2(offset.y - center.y, offset.x - center.x)
                    },
                    onDragEnd = {
                        isDragging = false
                        prevAngle = null
                        accumulatedDelta = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        prevAngle = null
                        accumulatedDelta = 0f
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val currentAngle = atan2(change.position.y - center.y, change.position.x - center.x)
                        val last = prevAngle ?: currentAngle

                        var delta = currentAngle - last
                        // Normalize angle wrap-around
                        while (delta > PI) delta -= (2 * PI).toFloat()
                        while (delta < -PI) delta += (2 * PI).toFloat()

                        val deltaDegrees = Math.toDegrees(delta.toDouble()).toFloat()
                        rotationAngleDegrees = (rotationAngleDegrees + deltaDegrees) % 360f

                        // Sensitivity: 7 degrees per BPM step
                        val degreesPerBpm = 7.0f
                        accumulatedDelta += deltaDegrees
                        val bpmStep = (accumulatedDelta / degreesPerBpm).toInt()

                        if (bpmStep != 0) {
                            val newBpm = (bpm + bpmStep).coerceIn(30, 300)
                            if (newBpm != bpm) {
                                onBpmChanged(newBpm)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            accumulatedDelta -= bpmStep * degreesPerBpm
                        }

                        prevAngle = currentAngle
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Rotary Wheel Outer Canvas (Ring, Ticks, Indicator Pip)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension / 2f) - 6.dp.toPx()
            val innerRadius = outerRadius - 26.dp.toPx()

            // 1. Ambient Beat Glow
            if (isPlaying) {
                drawCircle(
                    color = beatPulseColor,
                    radius = outerRadius + 8.dp.toPx()
                )
            }

            // 2. Beveled Rotary Track
            drawCircle(
                color = if (isDragging) MatteSurfaceContainer else MatteSurfaceLow,
                radius = (outerRadius + innerRadius) / 2f,
                style = Stroke(width = outerRadius - innerRadius)
            )

            // Outer and inner rim borders
            drawCircle(
                color = if (isDragging) MatteBlue.copy(alpha = 0.5f) else BorderSubtle,
                radius = outerRadius,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = BorderSubtle,
                radius = innerRadius,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Calibrated Radial Ticks around the Jog Wheel
            val tickCount = 60
            for (i in 0 until tickCount) {
                val tickBaseAngleDeg = (i * (360f / tickCount)) + rotationAngleDegrees
                val tickAngleRad = Math.toRadians(tickBaseAngleDeg.toDouble())
                val isMajor = i % 5 == 0

                val rStart = if (isMajor) innerRadius + 4.dp.toPx() else innerRadius + 9.dp.toPx()
                val rEnd = outerRadius - 4.dp.toPx()

                val tickColor = when {
                    isMajor && isDragging -> MatteBlue
                    isMajor -> TextSecondary.copy(alpha = 0.8f)
                    else -> BorderMedium.copy(alpha = 0.5f)
                }

                val start = Offset(
                    (center.x + rStart * cos(tickAngleRad)).toFloat(),
                    (center.y + rStart * sin(tickAngleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + rEnd * cos(tickAngleRad)).toFloat(),
                    (center.y + rEnd * sin(tickAngleRad)).toFloat()
                )

                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Rotating Indicator Pip / Knob on Wheel Rim
            val indicatorAngleRad = Math.toRadians(rotationAngleDegrees.toDouble() - 90.0)
            val indicatorRadius = (outerRadius + innerRadius) / 2f
            val indicatorCenter = Offset(
                (center.x + indicatorRadius * cos(indicatorAngleRad)).toFloat(),
                (center.y + indicatorRadius * sin(indicatorAngleRad)).toFloat()
            )

            drawCircle(
                color = if (isDragging) LockMint else LockEmerald,
                radius = 5.5.dp.toPx(),
                center = indicatorCenter
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = indicatorCenter
            )
        }

        // Inner Hub Display (Tempo Name, BPM Readout, Beat Indicator Dots)
        Box(
            modifier = Modifier
                .size(wheelSize - 56.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Tempo Classification
                Text(
                    text = tempoName.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatteBlue,
                    letterSpacing = 1.8.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Big Centered BPM Number
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "$bpm",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-1.5).sp
                    )
                    Text(
                        text = "BPM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Beat Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (beat in 0 until beatsPerMeasure) {
                        val isActive = isPlaying && beat == currentBeat
                        val isBeat0 = beat == 0

                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isActive && isBeat0 -> LockMint
                                isActive -> MatteBlue
                                isBeat0 -> LockEmerald.copy(alpha = 0.35f)
                                else -> BorderMedium
                            },
                            animationSpec = tween(durationMillis = 80),
                            label = "beatDotColor"
                        )

                        val dotScale by animateFloatAsState(
                            targetValue = if (isActive) 1.35f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh
                            ),
                            label = "beatDotScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(11.dp * dotScale)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSignatureBar(
    selectedSignature: Int,
    onSignatureSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(MatteSurfaceLow)
            .border(1.dp, CardBorder, PillShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val signatures = listOf(2, 3, 4, 6)
        for (sig in signatures) {
            val isSelected = sig == selectedSignature
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isSelected) LockEmerald else Color.Transparent)
                    .clickable { onSignatureSelected(sig) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$sig/4",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AmoledBlack else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AdjustmentRow(
    bpm: Int,
    onBpmChanged: (Int) -> Unit,
    onTapTempo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.92f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperPill(label = "-5", onClick = { onBpmChanged(bpm - 5) })
        StepperPill(label = "-1", onClick = { onBpmChanged(bpm - 1) })

        // Tap Tempo Interactive Pill
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(MatteBlueContainer)
                .border(1.dp, MatteBlue.copy(alpha = 0.45f), PillShape)
                .clickable(onClick = onTapTempo)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TAP TEMPO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MatteBlue,
                letterSpacing = 1.sp
            )
        }

        StepperPill(label = "+1", onClick = { onBpmChanged(bpm + 1) })
        StepperPill(label = "+5", onClick = { onBpmChanged(bpm + 5) })
    }
}

@Composable
private fun StepperPill(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(MatteSurfaceLow)
            .border(1.dp, CardBorder, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "btnScale"
    )

    val btnColor by animateColorAsState(
        targetValue = if (isPlaying) Color(0xFFEF4444) else LockEmerald,
        animationSpec = tween(durationMillis = 200),
        label = "btnColor"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(btnColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Stop Metronome" else "Start Metronome",
            tint = AmoledBlack,
            modifier = Modifier.size(34.dp)
        )
    }
}

package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.model.MusicalNote
import com.knownassurajit.tunr_tool.app.model.TuningMode
import com.knownassurajit.tunr_tool.app.theme.AmoledBlack
import com.knownassurajit.tunr_tool.app.theme.BorderMedium
import com.knownassurajit.tunr_tool.app.theme.BorderSubtle
import com.knownassurajit.tunr_tool.app.theme.FlatBlue
import com.knownassurajit.tunr_tool.app.theme.LockContainer
import com.knownassurajit.tunr_tool.app.theme.LockEmerald
import com.knownassurajit.tunr_tool.app.theme.LockGlow
import com.knownassurajit.tunr_tool.app.theme.LockMint
import com.knownassurajit.tunr_tool.app.theme.MatteBackground
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceContainer
import com.knownassurajit.tunr_tool.app.theme.OnLockContainer
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceLow
import com.knownassurajit.tunr_tool.app.theme.PillShape
import com.knownassurajit.tunr_tool.app.theme.SharpAmber
import com.knownassurajit.tunr_tool.app.theme.TextPrimary
import com.knownassurajit.tunr_tool.app.theme.TextSecondary
import com.knownassurajit.tunr_tool.app.theme.TextTertiary
import java.util.Locale
import kotlin.math.abs

/**
 * Focused, uncluttered Pitch Guidance HUD.
 * Designed to provide immediate, actionable visual direction without confusing clutter:
 * 1. Dominant note & octave identification
 * 2. Unambiguous tuning action badge ("TUNE UP", "TUNE DOWN", "LOCKED IN TUNE")
 * 3. Calibrated precision needle deviation scale
 * 4. Micro-telemetry (Detected Hz, Target Hz, Cents)
 */
@Composable
fun TunerHUD(
    currentNote: MusicalNote,
    detectedFrequencyHz: Float,
    targetFrequencyHz: Float,
    centsDeviation: Float,
    isVoiced: Boolean,
    rms: Float,
    tuningMode: TuningMode,
    modifier: Modifier = Modifier
) {
    val isInTune = isVoiced && abs(centsDeviation) <= tuningMode.inTuneThresholdCents

    // Animated colors and values for smooth, organic feedback
    val animatedCents by animateFloatAsState(
        targetValue = if (isVoiced) centsDeviation.coerceIn(-50f, 50f) else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "animatedCents"
    )

    val noteColor by animateColorAsState(
        targetValue = when {
            !isVoiced -> TextSecondary.copy(alpha = 0.5f)
            isInTune -> LockEmerald
            centsDeviation < 0f -> FlatBlue
            else -> SharpAmber
        },
        animationSpec = tween(150),
        label = "noteColor"
    )

    val haloGlowColor by animateColorAsState(
        targetValue = when {
            isInTune -> LockGlow
            isVoiced -> Color(0x1A38BDF8)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "haloGlow"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MatteSurfaceLow)
            .border(1.dp, if (isInTune) LockEmerald.copy(alpha = 0.5f) else BorderSubtle, PillShape)
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Audio Activity Ring & Giant Note Core
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient breathing ring reactive to audio amplitude
            Canvas(modifier = Modifier.size(190.dp)) {
                val amplitudeScale = if (isVoiced) (rms * 8f).coerceIn(0f, 1f) else 0f
                val ringRadius = (size.minDimension / 2f) - 10.dp.toPx()

                // Soft background halo
                drawCircle(
                    color = haloGlowColor,
                    radius = ringRadius + (amplitudeScale * 14.dp.toPx())
                )

                // Outer circular track
                drawCircle(
                    color = BorderSubtle,
                    radius = ringRadius,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Radial cent deviation arc
                if (isVoiced) {
                    val sweepAngle = (animatedCents / 50f) * 110f
                    drawArc(
                        color = noteColor,
                        startAngle = 270f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center zero reference tick
                drawCircle(
                    color = if (isInTune) LockEmerald else BorderMedium,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width / 2f, 10.dp.toPx())
                )
            }

            // Central Giant Note Readout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isVoiced && currentNote.name != "--") {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = currentNote.name,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold,
                            color = noteColor,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            text = currentNote.displayName.substring(currentNote.name.length),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = noteColor.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                } else {
                    Text(
                        text = "--",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Unambiguous Directional Guidance Badge
        DirectionalActionBadge(
            isVoiced = isVoiced,
            isInTune = isInTune,
            centsDeviation = centsDeviation,
            tuningMode = tuningMode
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Linear Micro-Fine Deviation Needle Meter
        LinearCentMeter(
            animatedCents = animatedCents,
            isInTune = isInTune,
            thresholdCents = tuningMode.inTuneThresholdCents,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(44.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Granular Telemetry Panel (Target Hz, Actual Hz, Cents)
        TelemetryRow(
            detectedFrequencyHz = detectedFrequencyHz,
            targetFrequencyHz = targetFrequencyHz,
            centsDeviation = centsDeviation,
            isVoiced = isVoiced,
            tuningMode = tuningMode
        )
    }
}

@Composable
private fun DirectionalActionBadge(
    isVoiced: Boolean,
    isInTune: Boolean,
    centsDeviation: Float,
    tuningMode: TuningMode
) {
    val (text, bgColor, contentColor) = when {
        !isVoiced -> Triple("PLUCK STRING", MatteSurfaceContainer, TextTertiary)
        isInTune -> Triple("✓ IN TUNE", LockContainer, OnLockContainer)
        centsDeviation < 0f -> Triple("TUNE UP ↗", Color(0xFF0C243C), FlatBlue)
        else -> Triple("TUNE DOWN ↘", Color(0xFF332005), SharpAmber)
    }

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bgColor)
            .border(
                1.dp,
                if (isInTune) LockEmerald.copy(alpha = 0.6f) else contentColor.copy(alpha = 0.3f),
                PillShape
            )
            .padding(horizontal = 20.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun LinearCentMeter(
    animatedCents: Float,
    isInTune: Boolean,
    thresholdCents: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val centerX = w / 2f

        // Base container rail
        drawRoundRect(
            color = AmoledBlack,
            topLeft = Offset(0f, centerY - 14.dp.toPx()),
            size = Size(w, 28.dp.toPx()),
            cornerRadius = CornerRadius(14.dp.toPx())
        )

        // Center in-tune target zone (illuminated emerald bar)
        val targetZoneWidth = (thresholdCents / 50f) * (w / 2f) * 2f
        drawRoundRect(
            color = if (isInTune) LockEmerald.copy(alpha = 0.25f) else Color(0x1838BDF8),
            topLeft = Offset(centerX - targetZoneWidth / 2f, centerY - 12.dp.toPx()),
            size = Size(targetZoneWidth, 24.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Graduation tick marks (-50, -25, 0, +25, +50)
        val tickOffsets = listOf(-50f, -25f, -10f, 0f, 10f, 25f, 50f)
        for (tick in tickOffsets) {
            val tickX = centerX + (tick / 50f) * (w / 2f - 18.dp.toPx())
            val isCenter = tick == 0f
            val tickHeight = if (isCenter) 18.dp.toPx() else 10.dp.toPx()
            val tickColor = if (isCenter) {
                if (isInTune) LockEmerald else TextSecondary
            } else {
                BorderMedium
            }

            drawLine(
                color = tickColor,
                start = Offset(tickX, centerY - tickHeight / 2f),
                end = Offset(tickX, centerY + tickHeight / 2f),
                strokeWidth = if (isCenter) 2.5f else 1.2f,
                cap = StrokeCap.Round
            )
        }

        // Active indicator needle
        val needleX = (centerX + (animatedCents / 50f) * (w / 2f - 18.dp.toPx()))
            .coerceIn(12.dp.toPx(), w - 12.dp.toPx())

        val needleColor = when {
            isInTune -> LockMint
            animatedCents < 0f -> FlatBlue
            else -> SharpAmber
        }

        // Needle vertical bar
        drawLine(
            color = needleColor,
            start = Offset(needleX, centerY - 16.dp.toPx()),
            end = Offset(needleX, centerY + 16.dp.toPx()),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )

        // Needle center dot
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(needleX, centerY)
        )
    }
}

@Composable
private fun TelemetryRow(
    detectedFrequencyHz: Float,
    targetFrequencyHz: Float,
    centsDeviation: Float,
    isVoiced: Boolean,
    tuningMode: TuningMode
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Target Pitch Telemetry
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "TARGET",
                fontSize = 10.sp,
                color = TextTertiary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (targetFrequencyHz > 0f) String.format(Locale.US, "%.1f Hz", targetFrequencyHz) else "--",
                fontSize = 13.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        // Cent Deviation (Real-time micro readout)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DEVIATION",
                fontSize = 10.sp,
                color = TextTertiary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            val centsText = when {
                !isVoiced -> "0.0¢"
                tuningMode.showFractionalCents -> String.format(Locale.US, "%+.1f¢", centsDeviation)
                else -> String.format(Locale.US, "%+d¢", centsDeviation.toInt())
            }
            Text(
                text = centsText,
                fontSize = 15.sp,
                color = if (abs(centsDeviation) <= tuningMode.inTuneThresholdCents && isVoiced) LockEmerald else TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Actual Detected Frequency
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "DETECTED",
                fontSize = 10.sp,
                color = TextTertiary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (isVoiced && detectedFrequencyHz > 0f) String.format(Locale.US, "%.1f Hz", detectedFrequencyHz) else "--",
                fontSize = 13.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

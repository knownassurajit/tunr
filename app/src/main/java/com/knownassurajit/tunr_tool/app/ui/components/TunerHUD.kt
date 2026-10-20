package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.knownassurajit.tunr_tool.app.theme.CardBackground
import com.knownassurajit.tunr_tool.app.theme.CardBorder
import com.knownassurajit.tunr_tool.app.theme.FlatBlue
import com.knownassurajit.tunr_tool.app.theme.LockContainer
import com.knownassurajit.tunr_tool.app.theme.LockEmerald
import com.knownassurajit.tunr_tool.app.theme.LockGlow
import com.knownassurajit.tunr_tool.app.theme.LockMint
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceContainer
import com.knownassurajit.tunr_tool.app.theme.OnLockContainer
import com.knownassurajit.tunr_tool.app.theme.PillShape
import com.knownassurajit.tunr_tool.app.theme.SharpAmber
import com.knownassurajit.tunr_tool.app.theme.TextPrimary
import com.knownassurajit.tunr_tool.app.theme.TextSecondary
import com.knownassurajit.tunr_tool.app.theme.TextTertiary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "animatedCents"
    )

    val noteColor by animateColorAsState(
        targetValue = when {
            !isVoiced -> TextTertiary.copy(alpha = 0.4f)
            isInTune -> LockEmerald
            centsDeviation < 0f -> FlatBlue
            else -> SharpAmber
        },
        animationSpec = tween(200),
        label = "noteColor"
    )

    val haloGlowColor by animateColorAsState(
        targetValue = when {
            isInTune -> LockGlow
            isVoiced -> Color(0x1A38BDF8)
            else -> Color.Transparent
        },
        animationSpec = tween(250),
        label = "haloGlow"
    )

    // Subtle breathing animation for idle state
    val infiniteTransition = rememberInfiniteTransition(label = "hudBreathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )

    // Animated border color for the HUD container
    val containerBorderColor by animateColorAsState(
        targetValue = when {
            isInTune -> LockEmerald.copy(alpha = 0.5f)
            isVoiced -> noteColor.copy(alpha = 0.2f)
            else -> CardBorder
        },
        animationSpec = tween(300),
        label = "containerBorder"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(CardBackground)
            .border(1.dp, containerBorderColor, PillShape)
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Audio Activity Ring & Giant Note Core
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val amplitudeScale = if (isVoiced) (rms * 8f).coerceIn(0f, 1f) else 0f
                val ringRadius = (size.minDimension / 2f) - 12.dp.toPx()

                // Soft ambient halo glow
                drawCircle(
                    color = haloGlowColor,
                    radius = ringRadius + (amplitudeScale * 16.dp.toPx())
                )

                // Outer circular track with consistent width
                drawCircle(
                    color = BorderSubtle,
                    radius = ringRadius,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Graduated tick marks around the arc for visual calibration
                val tickCount = 24
                for (i in 0 until tickCount) {
                    val angle = Math.toRadians((270.0 - 120.0) + (240.0 * i / (tickCount - 1)))
                    val isMajor = i % 6 == 0
                    val isCenter = i == tickCount / 2
                    val innerR = ringRadius - (if (isMajor) 10.dp.toPx() else 6.dp.toPx())
                    val outerR = ringRadius - 2.dp.toPx()
                    val tickColor = when {
                        isCenter -> if (isInTune) LockEmerald else TextSecondary
                        isMajor -> BorderMedium
                        else -> BorderSubtle.copy(alpha = 0.6f)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(
                            (size.width / 2f + innerR * cos(angle)).toFloat(),
                            (size.height / 2f + innerR * sin(angle)).toFloat()
                        ),
                        end = Offset(
                            (size.width / 2f + outerR * cos(angle)).toFloat(),
                            (size.height / 2f + outerR * sin(angle)).toFloat()
                        ),
                        strokeWidth = if (isCenter) 2.5f else if (isMajor) 1.8f else 1f,
                        cap = StrokeCap.Round
                    )
                }

                // Radial cent deviation arc
                if (isVoiced) {
                    val sweepAngle = (animatedCents / 50f) * 120f
                    drawArc(
                        color = noteColor,
                        startAngle = 270f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center zero reference marker (top of circle)
                val markerRadius = if (isInTune) 5.dp.toPx() else 3.5.dp.toPx()
                drawCircle(
                    color = if (isInTune) LockEmerald else BorderMedium,
                    radius = markerRadius,
                    center = Offset(size.width / 2f, 12.dp.toPx())
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
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = noteColor,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            text = currentNote.displayName.substring(currentNote.name.length),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = noteColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                } else {
                    Text(
                        text = "—",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = TextTertiary.copy(alpha = if (!isVoiced) breathingAlpha else 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Unambiguous Directional Guidance Badge
        DirectionalActionBadge(
            isVoiced = isVoiced,
            isInTune = isInTune,
            centsDeviation = centsDeviation,
            tuningMode = tuningMode
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Linear Micro-Fine Deviation Needle Meter
        LinearCentMeter(
            animatedCents = animatedCents,
            isInTune = isInTune,
            thresholdCents = tuningMode.inTuneThresholdCents,
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

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

    val borderColor by animateColorAsState(
        targetValue = when {
            isInTune -> LockEmerald.copy(alpha = 0.6f)
            isVoiced -> contentColor.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "badgeBorder"
    )

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bgColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, PillShape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
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
        val railHeight = 28.dp.toPx()

        // Base container rail
        drawRoundRect(
            color = AmoledBlack,
            topLeft = Offset(0f, centerY - railHeight / 2f),
            size = Size(w, railHeight),
            cornerRadius = CornerRadius(railHeight / 2f)
        )

        // Center in-tune target zone
        val targetZoneWidth = (thresholdCents / 50f) * (w / 2f) * 2f
        val zoneColor = if (isInTune) LockEmerald.copy(alpha = 0.22f) else Color(0x1438BDF8)
        drawRoundRect(
            color = zoneColor,
            topLeft = Offset(centerX - targetZoneWidth / 2f, centerY - (railHeight / 2f - 2.dp.toPx())),
            size = Size(targetZoneWidth, railHeight - 4.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Graduation tick marks
        val tickOffsets = listOf(-50f, -25f, -10f, 0f, 10f, 25f, 50f)
        for (tick in tickOffsets) {
            val tickX = centerX + (tick / 50f) * (w / 2f - 18.dp.toPx())
            val isCenter = tick == 0f
            val tickHeight = if (isCenter) 16.dp.toPx() else 8.dp.toPx()
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
            start = Offset(needleX, centerY - 14.dp.toPx()),
            end = Offset(needleX, centerY + 14.dp.toPx()),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Needle center dot
        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
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
        modifier = Modifier.fillMaxWidth(0.82f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Target Pitch Telemetry
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "TARGET",
                fontSize = 9.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (targetFrequencyHz > 0f) String.format(Locale.US, "%.1f Hz", targetFrequencyHz) else "—",
                fontSize = 12.sp,
                color = if (isVoiced) TextSecondary else TextTertiary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        // Cent Deviation (Real-time micro readout)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DEVIATION",
                fontSize = 9.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            val centsText = when {
                !isVoiced -> "0.0¢"
                tuningMode.showFractionalCents -> String.format(Locale.US, "%+.1f¢", centsDeviation)
                else -> String.format(Locale.US, "%+d¢", centsDeviation.toInt())
            }
            Text(
                text = centsText,
                fontSize = 14.sp,
                color = if (abs(centsDeviation) <= tuningMode.inTuneThresholdCents && isVoiced) LockEmerald else TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Actual Detected Frequency
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "DETECTED",
                fontSize = 9.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (isVoiced && detectedFrequencyHz > 0f) String.format(Locale.US, "%.1f Hz", detectedFrequencyHz) else "—",
                fontSize = 12.sp,
                color = if (isVoiced) TextSecondary else TextTertiary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

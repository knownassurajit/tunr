package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.theme.AmoledBlack
import com.knownassurajit.tunr_tool.app.theme.BentoShape
import com.knownassurajit.tunr_tool.app.theme.BorderMedium
import com.knownassurajit.tunr_tool.app.theme.BorderSubtle
import com.knownassurajit.tunr_tool.app.theme.LockEmerald
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

/**
 * Minimalist, focused Metronome view.
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
            .background(MatteBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Time Signature Selector Bar
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(MatteSurfaceLow)
                .border(1.dp, BorderSubtle, PillShape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val signatures = listOf(2, 3, 4, 6)
            for (sig in signatures) {
                val isSelected = sig == beatsPerMeasure
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (isSelected) LockEmerald else Color.Transparent)
                        .clickable { onBeatsPerMeasureChanged(sig) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$sig/4",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Central Tempo Display & Visual Beat Meter
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(MatteSurface)
                .border(1.dp, BorderSubtle, PillShape)
                .padding(vertical = 36.dp, horizontal = 24.dp)
        ) {
            Text(
                text = tempoName.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MatteBlue,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$bpm",
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "BPM",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp, start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Beat Indicator Dots (Downbeat highlighted in Emerald)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (beat in 0 until beatsPerMeasure) {
                    val isActive = isPlaying && beat == currentBeat
                    val isDownbeat = beat == 0

                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isActive && isDownbeat -> LockMint
                            isActive -> MatteBlue
                            isDownbeat -> LockEmerald.copy(alpha = 0.3f)
                            else -> BorderMedium
                        },
                        label = "beatDotColor"
                    )

                    val dotScale by animateFloatAsState(
                        targetValue = if (isActive) 1.4f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                        label = "beatDotScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(16.dp * dotScale)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Tempo Slider & Quick Adjustment Steppers
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = bpm.toFloat(),
                onValueChange = { onBpmChanged(it.toInt()) },
                valueRange = 30f..260f,
                colors = SliderDefaults.colors(
                    thumbColor = LockMint,
                    activeTrackColor = LockEmerald,
                    inactiveTrackColor = MatteSurfaceContainer
                ),
                modifier = Modifier.fillMaxWidth(0.92f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Steppers (-5, -1, +1, +5)
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperPill(label = "-5", onClick = { onBpmChanged(bpm - 5) })
                StepperPill(label = "-1", onClick = { onBpmChanged(bpm - 1) })

                // Tap Tempo Pill
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MatteBlueContainer)
                        .border(1.dp, MatteBlue.copy(alpha = 0.4f), PillShape)
                        .clickable {
                            val now = System.currentTimeMillis()
                            tapTimestamps.add(now)
                            if (tapTimestamps.size > 5) tapTimestamps.removeAt(0)
                            if (tapTimestamps.size >= 2) {
                                val intervals = (1 until tapTimestamps.size).map { i ->
                                    tapTimestamps[i] - tapTimestamps[i - 1]
                                }
                                val avgInterval = intervals.average()
                                if (avgInterval in 200.0..2000.0) {
                                    val detectedBpm = (60_000.0 / avgInterval).toInt()
                                    onBpmChanged(detectedBpm)
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Large Play / Pause Control Button
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isPlaying) Color(0xFFEF4444) else LockEmerald)
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Stop Metronome" else "Start Metronome",
                tint = AmoledBlack,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
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
            .border(1.dp, BorderSubtle, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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

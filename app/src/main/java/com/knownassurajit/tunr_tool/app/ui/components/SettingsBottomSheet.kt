package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.model.TuningMode
import com.knownassurajit.tunr_tool.app.theme.AmoledBlack
import com.knownassurajit.tunr_tool.app.theme.BentoShape
import com.knownassurajit.tunr_tool.app.theme.BorderMedium
import com.knownassurajit.tunr_tool.app.theme.CardBackground
import com.knownassurajit.tunr_tool.app.theme.CardBorder
import com.knownassurajit.tunr_tool.app.theme.LockContainer
import com.knownassurajit.tunr_tool.app.theme.LockEmerald
import com.knownassurajit.tunr_tool.app.theme.LockMint
import com.knownassurajit.tunr_tool.app.theme.MatteBlue
import com.knownassurajit.tunr_tool.app.theme.MatteSurface
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceContainer
import com.knownassurajit.tunr_tool.app.theme.PillShape
import com.knownassurajit.tunr_tool.app.theme.SectionLabelColor
import com.knownassurajit.tunr_tool.app.theme.SliderActiveTrack
import com.knownassurajit.tunr_tool.app.theme.SliderInactiveTrack
import com.knownassurajit.tunr_tool.app.theme.SliderThumb
import com.knownassurajit.tunr_tool.app.theme.TextPrimary
import com.knownassurajit.tunr_tool.app.theme.TextSecondary
import com.knownassurajit.tunr_tool.app.theme.TextTertiary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    tuningMode: TuningMode,
    a4CalibrationHz: Float,
    noiseGateThreshold: Float,
    hapticFeedbackEnabled: Boolean,
    onTuningModeChanged: (TuningMode) -> Unit,
    onA4CalibrationChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onHapticFeedbackToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MatteSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "CALIBRATION & SETTINGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SectionLabelColor,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Tuning Precision Mode (Standard vs High Precision)
            Text(
                text = "TUNING PRECISION MODE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TuningModeCard(
                    mode = TuningMode.DEFAULT,
                    isSelected = tuningMode == TuningMode.DEFAULT,
                    modifier = Modifier.weight(1f),
                    onClick = { onTuningModeChanged(TuningMode.DEFAULT) }
                )
                TuningModeCard(
                    mode = TuningMode.HIGH_PRECISION,
                    isSelected = tuningMode == TuningMode.HIGH_PRECISION,
                    modifier = Modifier.weight(1f),
                    onClick = { onTuningModeChanged(TuningMode.HIGH_PRECISION) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. A4 Reference Frequency Slider (432Hz to 446Hz)
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "A4 REFERENCE FREQUENCY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Standard Concert Pitch is 440 Hz",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.1f Hz", a4CalibrationHz),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = LockEmerald,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = a4CalibrationHz,
                    onValueChange = onA4CalibrationChanged,
                    valueRange = 432f..446f,
                    colors = SliderDefaults.colors(
                        thumbColor = SliderThumb,
                        activeTrackColor = SliderActiveTrack,
                        inactiveTrackColor = SliderInactiveTrack
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsPillButton("-1 Hz") { onA4CalibrationChanged((a4CalibrationHz - 1f).coerceAtLeast(432f)) }
                        SettingsPillButton("+1 Hz") { onA4CalibrationChanged((a4CalibrationHz + 1f).coerceAtMost(446f)) }
                    }
                    SettingsPillButton("RESET 440 Hz") { onA4CalibrationChanged(440f) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Noise Gate / Microphone Sensitivity
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NOISE GATE SENSITIVITY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Rejects background room chatter",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f%%", noiseGateThreshold * 5000f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatteBlue,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unified slider color scheme with blue accent for input/gain
                Slider(
                    value = noiseGateThreshold,
                    onValueChange = onNoiseGateChanged,
                    valueRange = 0.005f..0.035f,
                    colors = SliderDefaults.colors(
                        thumbColor = MatteBlue,
                        activeTrackColor = MatteBlue.copy(alpha = 0.85f),
                        inactiveTrackColor = SliderInactiveTrack
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Haptic Pitch Lock Feedback
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HAPTIC PITCH LOCK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Vibrates when note enters in-tune window",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }

                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmoledBlack,
                            checkedTrackColor = LockEmerald,
                            checkedBorderColor = LockEmerald,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = MatteSurfaceContainer,
                            uncheckedBorderColor = BorderMedium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Strict Privacy Notice Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BentoShape)
                    .background(MatteSurfaceContainer.copy(alpha = 0.4f))
                    .border(1.dp, CardBorder, BentoShape)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "🔒 PRIVACY-FIRST LOCAL PROCESSING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LockEmerald,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tunr samples audio strictly in volatile memory (RAM) for real-time mathematical pitch analysis. No recordings or audio files are ever written to disk or transmitted.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BentoShape)
            .background(CardBackground)
            .border(1.dp, CardBorder, BentoShape)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun TuningModeCard(
    mode: TuningMode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) LockEmerald else CardBorder,
        animationSpec = tween(200),
        label = "modeBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) LockContainer.copy(alpha = 0.45f) else CardBackground,
        animationSpec = tween(200),
        label = "modeBg"
    )

    Box(
        modifier = modifier
            .clip(BentoShape)
            .background(bgColor)
            .border(1.dp, borderColor, BentoShape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = mode.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) LockMint else TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mode.subtitle,
                fontSize = 10.sp,
                color = TextTertiary,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun SettingsPillButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(MatteSurfaceContainer)
            .border(1.dp, BorderMedium, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.knownassurajit.tunr_tool.app.model.InstrumentString
import com.knownassurajit.tunr_tool.app.theme.BentoShape
import com.knownassurajit.tunr_tool.app.theme.BorderMedium
import com.knownassurajit.tunr_tool.app.theme.BorderSubtle
import com.knownassurajit.tunr_tool.app.theme.LockContainer
import com.knownassurajit.tunr_tool.app.theme.LockEmerald
import com.knownassurajit.tunr_tool.app.theme.LockMint
import com.knownassurajit.tunr_tool.app.theme.MatteBlue
import com.knownassurajit.tunr_tool.app.theme.MatteBlueContainer
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceContainer
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceLow
import com.knownassurajit.tunr_tool.app.theme.PillShape
import com.knownassurajit.tunr_tool.app.theme.TextPrimary
import com.knownassurajit.tunr_tool.app.theme.TextSecondary
import com.knownassurajit.tunr_tool.app.theme.TextTertiary
import java.util.Locale

/**
 * Bento-style string selector card matrix.
 * Allows tapping strings to manually target or auto-detect.
 */
@Composable
fun StringMatrix(
    strings: List<InstrumentString>,
    activeStringNumber: Int?,
    lockedStringNumbers: Set<Int>,
    a4CalibrationHz: Float,
    isManualLockMode: Boolean,
    onStringSelected: (InstrumentString?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (strings.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STRINGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary,
                letterSpacing = 1.2.sp
            )
            // Mode Indicator Badge (AUTO vs MANUAL)
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isManualLockMode) MatteBlueContainer else MatteSurfaceContainer)
                    .border(
                        1.dp,
                        if (isManualLockMode) MatteBlue.copy(alpha = 0.5f) else BorderSubtle,
                        PillShape
                    )
                    .clickable { onStringSelected(null) }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isManualLockMode) "MANUAL LOCK (RESET TO AUTO)" else "AUTO-DETECTING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isManualLockMode) MatteBlue else TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bento Grid for Strings (3 columns for 6 strings, 2 columns for 4 strings)
        val columns = if (strings.size <= 4) 2 else 3

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(strings) { item ->
                val isActive = item.stringNumber == activeStringNumber
                val isLocked = lockedStringNumbers.contains(item.stringNumber)

                StringCard(
                    stringItem = item,
                    isActive = isActive,
                    isLocked = isLocked,
                    calibratedHz = item.getCalibratedFrequency(a4CalibrationHz),
                    onClick = {
                        if (isActive && isManualLockMode) {
                            onStringSelected(null) // Return to auto
                        } else {
                            onStringSelected(item) // Focus string
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StringCard(
    stringItem: InstrumentString,
    isActive: Boolean,
    isLocked: Boolean,
    calibratedHz: Float,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isLocked -> LockEmerald
            isActive -> MatteBlue
            else -> BorderSubtle
        },
        label = "stringBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isLocked -> LockContainer.copy(alpha = 0.6f)
            isActive -> MatteBlueContainer.copy(alpha = 0.7f)
            else -> MatteSurfaceLow
        },
        label = "stringBg"
    )

    Box(
        modifier = Modifier
            .clip(BentoShape)
            .background(bgColor)
            .border(1.2.dp, borderColor, BentoShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STRING ${stringItem.stringNumber}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringItem.noteName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) LockMint else if (isActive) MatteBlue else TextPrimary
                    )
                    Text(
                        text = "${stringItem.octave}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LockEmerald)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                } else {
                    Text(
                        text = String.format(Locale.US, "%.1f", calibratedHz),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Hz",
                        fontSize = 9.sp,
                        color = TextTertiary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

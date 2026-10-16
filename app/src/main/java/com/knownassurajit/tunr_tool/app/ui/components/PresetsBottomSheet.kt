package com.knownassurajit.tunr_tool.app.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.model.TuningPreset
import com.knownassurajit.tunr_tool.app.theme.BentoShape
import com.knownassurajit.tunr_tool.app.theme.BorderSubtle
import com.knownassurajit.tunr_tool.app.theme.LockContainer
import com.knownassurajit.tunr_tool.app.theme.LockEmerald
import com.knownassurajit.tunr_tool.app.theme.LockMint
import com.knownassurajit.tunr_tool.app.theme.MatteBackground
import com.knownassurajit.tunr_tool.app.theme.MatteSurface
import com.knownassurajit.tunr_tool.app.theme.MatteSurfaceLow
import com.knownassurajit.tunr_tool.app.theme.PillShape
import com.knownassurajit.tunr_tool.app.theme.TextPrimary
import com.knownassurajit.tunr_tool.app.theme.TextSecondary
import com.knownassurajit.tunr_tool.app.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsBottomSheet(
    currentPreset: TuningPreset,
    onPresetSelected: (TuningPreset) -> Unit,
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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTRUMENTS & TUNINGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "EXACT PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LockEmerald
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TuningPreset.ALL_PRESETS) { preset ->
                    val isSelected = preset.id == currentPreset.id

                    PresetItemCard(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = {
                            onPresetSelected(preset)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PresetItemCard(
    preset: TuningPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BentoShape)
            .background(if (isSelected) LockContainer.copy(alpha = 0.45f) else MatteSurfaceLow)
            .border(
                1.dp,
                if (isSelected) LockEmerald else BorderSubtle,
                BentoShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exact Vector Instrument Silhouette
            InstrumentGraphic(
                instrumentType = preset.instrumentType,
                tintColor = if (isSelected) LockMint else TextSecondary,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) TextPrimary else TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preset.instrumentType.title,
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(LockEmerald)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

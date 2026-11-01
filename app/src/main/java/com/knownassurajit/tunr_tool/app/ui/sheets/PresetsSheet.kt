package com.knownassurajit.tunr_tool.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.knownassurajit.tunr_tool.app.model.Tuning
import com.knownassurajit.tunr_tool.app.model.Tunings
import kotlinx.coroutines.launch

/**
 * Presets as a segmented list: one rounded group, tight inner corners, a single radio selection.
 * Name is the headline, notes + instrument are supporting text, so scanning is one column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsSheet(
    selectedId: String,
    onSelect: (Tuning) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Commit the choice immediately, then animate the sheet away.
    fun closeWith(tuning: Tuning) {
        onSelect(tuning)
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    "Tuning library",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Instrument presets & custom tunings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(Tunings.All, key = { _, t -> t.id }) { index, tuning ->
                TuningRow(
                    tuning = tuning,
                    selected = tuning.id == selectedId,
                    shape = segmentShape(index, Tunings.All.lastIndex),
                    onClick = { closeWith(tuning) },
                )
            }
        }
    }
}

@Composable
private fun TuningRow(tuning: Tuning, selected: Boolean, shape: Shape, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(tuning.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text("${tuning.notes} · ${tuning.instrument.label}") },
        leadingContent = {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selected) scheme.primary else scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    tuning.instrument.icon,
                    contentDescription = null,
                    tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = scheme.primary),
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
            headlineColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
            supportingColor = if (selected) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant,
        ),
        modifier = Modifier
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

private fun segmentShape(index: Int, last: Int): Shape {
    val outer = 28.dp
    val inner = 6.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index == last) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

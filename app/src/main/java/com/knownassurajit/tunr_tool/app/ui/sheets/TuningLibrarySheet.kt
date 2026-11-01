package com.knownassurajit.tunr_tool.app.ui.sheets

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knownassurajit.tunr_tool.app.model.InstrumentSpec
import com.knownassurajit.tunr_tool.app.model.Tuning
import com.knownassurajit.tunr_tool.app.ui.ImportOutcome
import com.knownassurajit.tunr_tool.app.ui.LibraryUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the library sheet can ask the host to do. Hoisted so the sheet stays stateless about data. */
class LibraryActions(
    val onSelect: (Tuning) -> Unit,
    val onToggleFavorite: (String) -> Unit,
    val onNewCustom: () -> Unit,
    val onEditCustom: (String) -> Unit,
    val onDuplicateCustom: (String) -> Unit,
    val onDeleteCustom: (String) -> Unit,
    val onMoveCustom: (String, Int) -> Unit,
    val exportJson: () -> String,
    val importJson: (String) -> ImportOutcome,
)

private fun categoryIcon(categoryId: String): ImageVector = when (categoryId) {
    "chromatic" -> Icons.Rounded.GraphicEq
    "custom" -> Icons.Rounded.Edit
    else -> Icons.Rounded.MusicNote
}

/**
 * Tuning library: search → favorites → recents → custom → Category ▸ Instrument ▸ Variant ▸ Tuning.
 * Selecting a tuning commits immediately, then the sheet animates away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningLibrarySheet(
    library: LibraryUi,
    selectedId: String,
    actions: LibraryActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var query by rememberSaveable { mutableStateOf("") }
    var instrumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    fun closeWith(tuning: Tuning) {
        actions.onSelect(tuning)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { writeText(context, uri, actions.exportJson()) }
            Toast.makeText(context, if (ok) "Tunings exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { readText(context, uri) }
            val message = when (val r = text?.let(actions.importJson)) {
                null -> "Could not read that file"
                is ImportOutcome.Done -> "Imported ${r.imported}" + if (r.skipped > 0) ", skipped ${r.skipped} invalid" else ""
                is ImportOutcome.Failed -> r.reason
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Tuning library", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${library.catalog.all.size} presets · ${library.custom.size} custom",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "More options") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Import tunings…") },
                        leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                        onClick = { menuOpen = false; importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                    )
                    DropdownMenuItem(
                        text = { Text("Export my tunings…") },
                        leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                        enabled = library.custom.isNotEmpty(),
                        onClick = { menuOpen = false; exportLauncher.launch("tunr-tunings.json") },
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search tunings, instruments, notes") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear search") }
            },
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        val results = remember(query, library.catalog) { library.catalog.search(query) }
        val openInstrument = remember(instrumentId, library.catalog) { instrumentId?.let(library.catalog::instrument) }

        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when {
                query.isNotBlank() -> {
                    sectionHeader("search", if (results.isEmpty()) "No matches" else "${results.size} result${if (results.size == 1) "" else "s"}")
                    tuningRows("search", results, selectedId, library, actions, ::closeWith)
                }
                openInstrument != null -> instrumentDetail(openInstrument, selectedId, library, actions, ::closeWith) { instrumentId = null }
                else -> {
                    customSection(library, selectedId, actions, ::closeWith) { deleteId = it }
                    if (library.favorites.isNotEmpty()) {
                        sectionHeader("fav", "Favorites")
                        tuningRows("fav", library.favorites, selectedId, library, actions, ::closeWith)
                    }
                    if (library.recents.isNotEmpty()) {
                        sectionHeader("recent", "Recent")
                        tuningRows("recent", library.recents, selectedId, library, actions, ::closeWith)
                    }
                    library.catalog.categories.forEach { category ->
                        sectionHeader("cat:${category.id}", category.name)
                        val last = category.instruments.lastIndex
                        itemsIndexed(category.instruments, key = { _, i -> "inst:${i.id}" }) { index, inst ->
                            InstrumentRow(inst, category.id, segmentShape(index, last)) { instrumentId = inst.id }
                        }
                    }
                }
            }
        }
    }

    deleteId?.let { id ->
        val name = library.custom.firstOrNull { it.id == id }?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Delete “$name”?") },
            text = { Text("This custom tuning will be removed from your library and favorites.") },
            confirmButton = { TextButton(onClick = { actions.onDeleteCustom(id); deleteId = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } },
        )
    }
}

// ---- sections ----------------------------------------------------------------------------------

private fun LazyListScope.sectionHeader(key: String, title: String) {
    item(key = "h:$key") {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp),
        )
    }
}

private fun LazyListScope.tuningRows(
    section: String,
    tunings: List<Tuning>,
    selectedId: String,
    library: LibraryUi,
    actions: LibraryActions,
    onPick: (Tuning) -> Unit,
) {
    itemsIndexed(tunings, key = { _, t -> "$section:${t.id}" }) { index, tuning ->
        TuningRow(
            tuning = tuning,
            selected = tuning.id == selectedId,
            favorite = tuning.id in library.favoriteIds,
            showPath = true,
            shape = segmentShape(index, tunings.lastIndex),
            onClick = { onPick(tuning) },
            onToggleFavorite = { actions.onToggleFavorite(tuning.id) },
        )
    }
}

private fun LazyListScope.instrumentDetail(
    instrument: InstrumentSpec,
    selectedId: String,
    library: LibraryUi,
    actions: LibraryActions,
    onPick: (Tuning) -> Unit,
    onBack: () -> Unit,
) {
    item(key = "back") {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp))
            Text("  ${instrument.name}")
        }
    }
    instrument.variants.forEach { variant ->
        item(key = "vh:${variant.id}") {
            Column(Modifier.padding(start = 8.dp, top = 12.dp, bottom = 6.dp)) {
                Text(variant.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                variant.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        itemsIndexed(variant.tunings, key = { _, t -> "v:${t.id}" }) { index, tuning ->
            TuningRow(
                tuning = tuning,
                selected = tuning.id == selectedId,
                favorite = tuning.id in library.favoriteIds,
                showPath = false,
                shape = segmentShape(index, variant.tunings.lastIndex),
                onClick = { onPick(tuning) },
                onToggleFavorite = { actions.onToggleFavorite(tuning.id) },
            )
        }
    }
}

private fun LazyListScope.customSection(
    library: LibraryUi,
    selectedId: String,
    actions: LibraryActions,
    onPick: (Tuning) -> Unit,
    onAskDelete: (String) -> Unit,
) {
    item(key = "h:custom") {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("My tunings", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FilledTonalButton(onClick = actions.onNewCustom) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Text("  New")
            }
        }
    }
    if (library.custom.isEmpty()) {
        item(key = "custom-empty") {
            Text(
                "Create tunings with any note, octave and cents offset per string.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    } else {
        itemsIndexed(library.custom, key = { _, t -> "custom:${t.id}" }) { index, tuning ->
            TuningRow(
                tuning = tuning,
                selected = tuning.id == selectedId,
                favorite = tuning.id in library.favoriteIds,
                showPath = false,
                shape = segmentShape(index, library.custom.lastIndex),
                onClick = { onPick(tuning) },
                onToggleFavorite = { actions.onToggleFavorite(tuning.id) },
                menu = CustomMenu(
                    canMoveUp = index > 0,
                    canMoveDown = index < library.custom.lastIndex,
                    onEdit = { actions.onEditCustom(tuning.id) },
                    onDuplicate = { actions.onDuplicateCustom(tuning.id) },
                    onMoveUp = { actions.onMoveCustom(tuning.id, -1) },
                    onMoveDown = { actions.onMoveCustom(tuning.id, 1) },
                    onDelete = { onAskDelete(tuning.id) },
                ),
            )
        }
    }
}

// ---- rows ----------------------------------------------------------------------------------------

private class CustomMenu(
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val onEdit: () -> Unit,
    val onDuplicate: () -> Unit,
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
private fun TuningRow(
    tuning: Tuning,
    selected: Boolean,
    favorite: Boolean,
    showPath: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    menu: CustomMenu? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(tuning.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                if (showPath) "${tuning.notes} · ${tuning.path}" else tuning.notes,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(if (selected) scheme.primary else scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (selected) Icons.Rounded.Check else categoryIcon(tuning.categoryId), null,
                    tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (favorite) "Remove ${tuning.name} from favorites" else "Add ${tuning.name} to favorites",
                        tint = if (favorite) scheme.primary else scheme.onSurfaceVariant,
                    )
                }
                if (menu != null) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "Actions for ${tuning.name}") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menuOpen = false; menu.onEdit() })
                            DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { menuOpen = false; menu.onDuplicate() })
                            DropdownMenuItem(text = { Text("Move up") }, enabled = menu.canMoveUp, onClick = { menuOpen = false; menu.onMoveUp() })
                            DropdownMenuItem(text = { Text("Move down") }, enabled = menu.canMoveDown, onClick = { menuOpen = false; menu.onMoveDown() })
                            DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menuOpen = false; menu.onDelete() })
                        }
                    }
                }
            }
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

@Composable
private fun InstrumentRow(
    instrument: InstrumentSpec,
    categoryId: String,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(instrument.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(
                if (instrument.variants.size > 1) "${instrument.variants.size} variants · ${instrument.tuningCount} tunings"
                else "${instrument.tuningCount} tuning${if (instrument.tuningCount == 1) "" else "s"}",
            )
        },
        leadingContent = {
            Box(Modifier.size(44.dp).clip(CircleShape).background(scheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(categoryId), null, tint = scheme.onSecondaryContainer)
            }
        },
        trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = scheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = scheme.surfaceContainerHigh),
        modifier = Modifier
            .clip(shape)
            .semantics(mergeDescendants = true) { contentDescription = "${instrument.name}, ${instrument.tuningCount} tunings" }
            .selectable(selected = false, role = Role.Button, onClick = onClick),
    )
}

internal fun segmentShape(index: Int, last: Int): androidx.compose.ui.graphics.Shape {
    val outer = 28.dp
    val inner = 6.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index == last) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

// ---- file IO (SAF, no storage permission needed) ----------------------------------------------------

private fun writeText(context: Context, uri: Uri, text: String): Boolean = runCatching {
    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) } != null
}.getOrDefault(false)

/** Reads at most 512 KB (API 24-safe) so a wrong file can't exhaust memory. */
private fun readText(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val limit = 512 * 1024
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > limit) return@use null
        }
        String(out.toByteArray(), Charsets.UTF_8)
    }
}.getOrNull()

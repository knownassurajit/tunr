package com.knownassurajit.tunr_tool.app.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knownassurajit.tunr_tool.app.model.CustomString
import com.knownassurajit.tunr_tool.app.model.CustomTuning
import com.knownassurajit.tunr_tool.app.model.Notes
import com.knownassurajit.tunr_tool.app.model.TuningError
import com.knownassurajit.tunr_tool.app.model.TuningValidation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Create / edit a custom tuning: name, 1–12 strings, per string a semitone stepper (note + octave)
 * and a ±50¢ detune. All rules live in [TuningValidation]; Save is enabled only when it passes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTuningEditor(
    initial: CustomTuning?,
    takenNames: List<String>,
    /** Strings of the tuning currently on the tuner, offered as a starting point for new tunings. */
    seed: List<CustomString>,
    a4Hz: Float,
    onSave: (name: String, strings: List<CustomString>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var flats by remember { mutableStateOf(initial?.strings?.any { it.note.endsWith("♭") || it.note.endsWith("b") } == true) }
    val strings = remember {
        mutableStateListOf<CustomString>().apply {
            addAll(initial?.strings ?: seed.ifEmpty { DEFAULT_STRINGS })
        }
    }
    var touchedName by remember { mutableStateOf(initial != null) }
    var isSaving by remember { mutableStateOf(false) }

    val errors = remember(name, strings.toList(), takenNames) { TuningValidation.validate(name, strings.toList(), takenNames) }
    val nameError = when {
        TuningError.NameBlank in errors -> if (touchedName) "Give the tuning a name" else null
        TuningError.NameTooLong in errors -> "Max ${TuningValidation.MAX_NAME} characters"
        TuningError.NameTaken in errors -> "You already have a tuning with this name"
        else -> null
    }

    fun close(after: () -> Unit = {}) {
        if (isSaving) return
        isSaving = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { after(); onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(if (initial == null) "New tuning" else "Edit tuning", style = MaterialTheme.typography.titleLarge)
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(TuningValidation.MAX_NAME + 1); touchedName = true },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { Text(nameError ?: "${name.trim().length}/${TuningValidation.MAX_NAME}") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Strings", style = MaterialTheme.typography.titleSmall)
                        Text("Thickest first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = { if (strings.size > 1) strings.removeAt(strings.lastIndex) },
                            enabled = strings.size > 1,
                        ) { Icon(Icons.Rounded.Remove, "Remove a string") }
                        Text("${strings.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
                        FilledTonalIconButton(
                            onClick = {
                                if (strings.size < TuningValidation.MAX_STRINGS) {
                                    strings += strings.lastOrNull()?.let { shifted(it, 5, flats) } ?: DEFAULT_STRINGS.first()
                                }
                            },
                            enabled = strings.size < TuningValidation.MAX_STRINGS,
                        ) { Icon(Icons.Rounded.Add, "Add a string") }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !flats, onClick = { flats = false; respell(strings, false) }, label = { Text("Sharps ♯") },
                    )
                    FilterChip(
                        selected = flats, onClick = { flats = true; respell(strings, true) }, label = { Text("Flats ♭") },
                    )
                }
            }
            itemsIndexed(strings, key = { index, _ -> index }) { index, string ->
                StringEditorRow(
                    number = strings.size - index,
                    value = string,
                    a4Hz = a4Hz,
                    flats = flats,
                    onChange = { strings[index] = it },
                )
            }
            item {
                val messages = errors.mapNotNull {
                    when (it) {
                        TuningError.NoStrings -> "Add at least one string"
                        TuningError.TooManyStrings -> "At most ${TuningValidation.MAX_STRINGS} strings"
                        is TuningError.BadNote -> "String ${strings.size - it.index} has an invalid note"
                        is TuningError.OctaveRange -> "String ${strings.size - it.index} is outside A0–G6"
                        is TuningError.CentsRange -> "Detune must be within ±${TuningValidation.MAX_CENTS.toInt()}¢"
                        else -> null
                    }
                }.distinct()
                if (messages.isNotEmpty()) {
                    Text(messages.joinToString("\n"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { close() }, enabled = !isSaving, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { close { onSave(name.trim(), strings.toList()) } },
                        enabled = errors.isEmpty() && !isSaving,
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun StringEditorRow(number: Int, value: CustomString, a4Hz: Float, flats: Boolean, onChange: (CustomString) -> Unit) {
    val midi = Notes.midi(value.note, value.octave) ?: 69
    val hz = Notes.hz(midi, value.cents, a4Hz)
    val label = Notes.label(midi, flats)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("String $number", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                FilledTonalIconButton(
                    onClick = { onChange(shifted(value, -1, flats)) },
                    enabled = midi > Notes.MIN_MIDI,
                ) { Icon(Icons.Rounded.Remove, "String $number down a semitone") }
                Column(
                    Modifier.padding(horizontal = 8.dp).semantics(mergeDescendants = true) { contentDescription = "$label, %.1f hertz".format(hz) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text("%.2f Hz".format(hz), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(
                    onClick = { onChange(shifted(value, 1, flats)) },
                    enabled = midi < Notes.MAX_MIDI,
                ) { Icon(Icons.Rounded.Add, "String $number up a semitone") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Detune", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                Slider(
                    value = value.cents,
                    onValueChange = { onChange(value.copy(cents = (it * 2).roundToInt() / 2f)) },
                    valueRange = -TuningValidation.MAX_CENTS..TuningValidation.MAX_CENTS,
                    modifier = Modifier.weight(1f).semantics { stateDescription = "%+.1f cents".format(value.cents) },
                )
                TextButton(onClick = { onChange(value.copy(cents = 0f)) }, enabled = value.cents != 0f) {
                    Text("%+.1f¢".format(value.cents))
                }
            }
        }
    }
}

private val DEFAULT_STRINGS = listOf(
    CustomString("E", 2), CustomString("A", 2), CustomString("D", 3),
    CustomString("G", 3), CustomString("B", 3), CustomString("E", 4),
)

private fun shifted(s: CustomString, semitones: Int, flats: Boolean): CustomString {
    val midi = ((Notes.midi(s.note, s.octave) ?: 69) + semitones).coerceIn(Notes.MIN_MIDI, Notes.MAX_MIDI)
    return s.copy(note = Notes.name(midi, flats), octave = Notes.octave(midi))
}

private fun respell(strings: MutableList<CustomString>, flats: Boolean) {
    for (i in strings.indices) strings[i] = shifted(strings[i], 0, flats)
}

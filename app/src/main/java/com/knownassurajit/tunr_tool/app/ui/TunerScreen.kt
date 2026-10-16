package com.knownassurajit.tunr_tool.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knownassurajit.tunr_tool.app.ui.components.InstrumentGraphic
import com.knownassurajit.tunr_tool.app.ui.components.MetronomeScreen
import com.knownassurajit.tunr_tool.app.ui.components.PresetsBottomSheet
import com.knownassurajit.tunr_tool.app.ui.components.SettingsBottomSheet
import com.knownassurajit.tunr_tool.app.ui.components.StringMatrix
import com.knownassurajit.tunr_tool.app.ui.components.TunerHUD
import com.knownassurajit.tunr_tool.app.theme.AmoledBlack
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

@Composable
fun TunerScreen(
    viewModel: TunerViewModel,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showPresetsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AmoledBlack,
        topBar = {
            TopTunerBar(
                presetName = uiState.currentPreset.name,
                instrumentType = uiState.currentPreset.instrumentType,
                onOpenPresets = { showPresetsSheet = true },
                onOpenSettings = { showSettingsSheet = true }
            )
        },
        bottomBar = {
            FloatingPillNavBar(
                activeTab = uiState.activeTab,
                onTabSelected = { tab ->
                    viewModel.setActiveTab(tab)
                    if (tab == NavigationTab.TUNER) {
                        viewModel.startListening(onRequestPermission)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmoledBlack)
        ) {
            when (uiState.activeTab) {
                NavigationTab.TUNER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Core Pitch Guidance HUD
                        TunerHUD(
                            currentNote = uiState.currentNote,
                            detectedFrequencyHz = uiState.detectedFrequencyHz,
                            targetFrequencyHz = uiState.targetFrequencyHz,
                            centsDeviation = uiState.centsDeviation,
                            isVoiced = uiState.isVoiced,
                            rms = uiState.rms,
                            tuningMode = uiState.tuningMode
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Bento String Matrix
                        if (!uiState.currentPreset.isChromatic) {
                            StringMatrix(
                                strings = uiState.currentPreset.strings,
                                activeStringNumber = uiState.activeStringNumber,
                                lockedStringNumbers = uiState.lockedStringNumbers,
                                a4CalibrationHz = uiState.a4CalibrationHz,
                                isManualLockMode = uiState.isManualLockMode,
                                onStringSelected = { stringItem ->
                                    viewModel.selectString(stringItem)
                                }
                            )
                        } else {
                            // Chromatic Free-Tuning Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PillShape)
                                    .background(MatteSurfaceLow)
                                    .border(1.dp, BorderSubtle, PillShape)
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CHROMATIC TUNER • ALL 12 TONES DETECTED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                NavigationTab.METRONOME -> {
                    MetronomeScreen(
                        bpm = uiState.metronomeBpm,
                        beatsPerMeasure = uiState.metronomeBeats,
                        isPlaying = uiState.isMetronomePlaying,
                        currentBeat = uiState.metronomeCurrentBeat,
                        onBpmChanged = { viewModel.setMetronomeBpm(it) },
                        onBeatsPerMeasureChanged = { viewModel.setMetronomeBeats(it) },
                        onTogglePlay = { viewModel.toggleMetronome() }
                    )
                }
            }

            // Bottom Sheets
            if (showPresetsSheet) {
                PresetsBottomSheet(
                    currentPreset = uiState.currentPreset,
                    onPresetSelected = { preset -> viewModel.selectPreset(preset) },
                    onDismiss = { showPresetsSheet = false }
                )
            }

            if (showSettingsSheet) {
                SettingsBottomSheet(
                    tuningMode = uiState.tuningMode,
                    a4CalibrationHz = uiState.a4CalibrationHz,
                    noiseGateThreshold = uiState.noiseGateThreshold,
                    hapticFeedbackEnabled = uiState.hapticFeedbackEnabled,
                    onTuningModeChanged = { viewModel.setTuningMode(it) },
                    onA4CalibrationChanged = { viewModel.setA4Calibration(it) },
                    onNoiseGateChanged = { viewModel.setNoiseGate(it) },
                    onHapticFeedbackToggle = { viewModel.toggleHapticFeedback(it) },
                    onDismiss = { showSettingsSheet = false }
                )
            }
        }
    }
}

@Composable
private fun TopTunerBar(
    presetName: String,
    instrumentType: com.knownassurajit.tunr_tool.app.model.InstrumentType,
    onOpenPresets: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(AmoledBlack)
            .border(width = 0.5.dp, color = BorderSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(LockEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = AmoledBlack,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "TUNR",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
        }

        // Instrument Preset Pill Selector Trigger
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(MatteSurfaceLow)
                .border(1.dp, BorderSubtle, PillShape)
                .clickable(onClick = onOpenPresets)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InstrumentGraphic(
                instrumentType = instrumentType,
                size = 18.dp,
                tintColor = LockMint
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = presetName.split("(").first().trim(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // Settings Button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MatteSurfaceLow)
                .border(1.dp, BorderSubtle, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FloatingPillNavBar(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(MatteSurface)
                .border(1.2.dp, BorderMedium, PillShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tuner Tab
            val isTuner = activeTab == NavigationTab.TUNER
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isTuner) LockEmerald else Color.Transparent)
                    .clickable { onTabSelected(NavigationTab.TUNER) }
                    .padding(horizontal = 22.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = "Tuner",
                    tint = if (isTuner) AmoledBlack else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TUNER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTuner) AmoledBlack else TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            // Metronome Tab
            val isMetronome = activeTab == NavigationTab.METRONOME
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isMetronome) LockEmerald else Color.Transparent)
                    .clickable { onTabSelected(NavigationTab.METRONOME) }
                    .padding(horizontal = 22.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Metronome",
                    tint = if (isMetronome) AmoledBlack else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "METRONOME",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMetronome) AmoledBlack else TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

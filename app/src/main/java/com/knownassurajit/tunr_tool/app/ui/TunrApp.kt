package com.knownassurajit.tunr_tool.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knownassurajit.tunr_tool.app.MainActivity
import com.knownassurajit.tunr_tool.app.model.ScreenDimTimeout
import com.knownassurajit.tunr_tool.app.model.ScreenTimeout
import com.knownassurajit.tunr_tool.app.theme.TunrTheme
import com.knownassurajit.tunr_tool.app.ui.components.TunrTopBar
import com.knownassurajit.tunr_tool.app.ui.metronome.MetronomeScreen
import com.knownassurajit.tunr_tool.app.ui.sheets.PresetsSheet
import com.knownassurajit.tunr_tool.app.ui.sheets.SettingsSheet
import com.knownassurajit.tunr_tool.app.ui.tuner.TunerScreen
import kotlinx.coroutines.delay

private enum class Destination(val label: String, val icon: ImageVector) {
    Tuner("Tuner", Icons.Rounded.GraphicEq),
    Metronome("Metronome", Icons.Rounded.Speed),
}

private enum class TunrSheet { Presets, Settings }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TunrApp(vm: TunrViewModel = viewModel()) {
    val tuner by vm.tuner.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val metronome by vm.metronome.collectAsStateWithLifecycle()

    val lastDest = remember { vm.getLastDestination() }
    var destination by rememberSaveable {
        mutableStateOf(if (lastDest == "metronome") Destination.Metronome else Destination.Tuner)
    }
    var sheet by rememberSaveable { mutableStateOf<TunrSheet?>(null) }

    val context = LocalContext.current
    val activity = context as? MainActivity
    val view = LocalView.current

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        micGranted = it
    }

    // Inactivity watchdog for screen dimming and screen timeout
    var lastActivityTimeMs by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var isDimmed by remember { mutableStateOf(false) }

    fun onUserActivity() {
        lastActivityTimeMs = SystemClock.elapsedRealtime()
        if (isDimmed) {
            isDimmed = false
            activity?.setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    // Reset activity timer when note is voiced or metronome is playing
    LaunchedEffect(tuner.reading != null, metronome.playing) {
        if (tuner.reading != null || metronome.playing) {
            onUserActivity()
        }
    }

    // Inactivity watchdog loop
    LaunchedEffect(settings.screenDimTimeout, settings.screenTimeout) {
        while (true) {
            delay(1000L)
            val now = SystemClock.elapsedRealtime()
            val inactiveSec = (now - lastActivityTimeMs) / 1000

            // 1. Screen Dimming / Energy Saver
            if (settings.screenDimTimeout != ScreenDimTimeout.Never && inactiveSec >= settings.screenDimTimeout.seconds) {
                if (!isDimmed) {
                    isDimmed = true
                    activity?.setScreenBrightness(0.04f)
                }
            } else if (inactiveSec < settings.screenDimTimeout.seconds) {
                if (isDimmed) {
                    isDimmed = false
                    activity?.setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
                }
            }

            // 2. Screen Keep Awake / Timeout
            when (settings.screenTimeout) {
                ScreenTimeout.AlwaysOn -> view.keepScreenOn = true
                ScreenTimeout.SystemDefault -> view.keepScreenOn = false
                else -> {
                    val timeoutSec = settings.screenTimeout.minutes * 60
                    view.keepScreenOn = (inactiveSec < timeoutSec)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
            view.keepScreenOn = false
        }
    }

    // Requirement 2: Function must stop when moving between Tuner and Metronome
    LaunchedEffect(destination, micGranted) {
        val destName = if (destination == Destination.Metronome) "metronome" else "tuner"
        vm.onDestinationSelected(destName, micGranted)
    }

    val wide = LocalConfiguration.current.screenWidthDp >= 600
    TunrTheme(themeScheme = settings.themeScheme, customHex = settings.customThemeHex) {
        val motion = MaterialTheme.motionScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            onUserActivity()
                        }
                    }
                }
        ) {
            Scaffold(
                topBar = {
                    TunrTopBar(
                        tuningName = tuner.tuning.name,
                        onPresets = { sheet = TunrSheet.Presets },
                        onSettings = { sheet = TunrSheet.Settings },
                    )
                },
                bottomBar = {
                    if (!wide) {
                        ShortNavigationBar {
                            Destination.entries.forEach { d ->
                                ShortNavigationBarItem(
                                    selected = destination == d,
                                    onClick = {
                                        if (destination != d) {
                                            destination = d
                                            val destName = if (d == Destination.Metronome) "metronome" else "tuner"
                                            vm.onDestinationSelected(destName, micGranted)
                                        }
                                    },
                                    icon = { Icon(d.icon, contentDescription = null) },
                                    label = { Text(d.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Row(Modifier.padding(padding).fillMaxSize()) {
                    if (wide) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            Spacer(Modifier.weight(1f))
                            Destination.entries.forEach { d ->
                                NavigationRailItem(
                                    selected = destination == d,
                                    onClick = {
                                        if (destination != d) {
                                            destination = d
                                            val destName = if (d == Destination.Metronome) "metronome" else "tuner"
                                            vm.onDestinationSelected(destName, micGranted)
                                        }
                                    },
                                    icon = { Icon(d.icon, contentDescription = null) },
                                    label = { Text(d.label) },
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    AnimatedContent(
                        targetState = destination,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            fadeIn(motion.defaultEffectsSpec<Float>()) togetherWith
                                fadeOut(motion.fastEffectsSpec<Float>())
                        },
                        label = "destination",
                    ) { target ->
                        when (target) {
                            Destination.Tuner -> TunerScreen(
                                state = tuner,
                                settings = settings,
                                micGranted = micGranted,
                                onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                onAutoDetect = vm::setAutoDetect,
                                onSelectString = vm::selectString,
                            )
                            Destination.Metronome -> MetronomeScreen(
                                state = metronome,
                                settings = settings,
                                onBpm = vm::setBpm,
                                onNudge = vm::nudgeBpm,
                                onTap = { vm.tapTempo() },
                                onSignature = vm::setSignature,
                                onToggleAccent = vm::toggleBeatAccent,
                                onPlaying = vm::setPlaying,
                            )
                        }
                    }
                }
            }

            // Ambient Energy Saver Overlay (Requirement 3)
            if (isDimmed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.86f))
                        .clickable { onUserActivity() },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            "ENERGY SAVER ACTIVE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            "Display dimmed to 4% · Tap or pluck to wake",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        when (sheet) {
            TunrSheet.Presets -> PresetsSheet(
                selectedId = tuner.tuning.id,
                onSelect = vm::selectTuning,
                onDismiss = { sheet = null },
            )
            TunrSheet.Settings -> SettingsSheet(
                settings = settings,
                onChange = vm::updateSettings,
                onDismiss = { sheet = null },
            )
            null -> Unit
        }
    }
}

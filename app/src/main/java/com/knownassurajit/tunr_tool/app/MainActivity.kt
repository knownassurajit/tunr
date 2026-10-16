package com.knownassurajit.tunr_tool.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.knownassurajit.tunr_tool.app.theme.TunrTheme
import com.knownassurajit.tunr_tool.app.ui.TunerScreen
import com.knownassurajit.tunr_tool.app.ui.TunerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TunerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TunrTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.startListening {
                            // Already handled
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (hasRecordAudioPermission()) {
                        viewModel.startListening {}
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                TunerScreen(
                    viewModel = viewModel,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        if (hasRecordAudioPermission()) {
            viewModel.startListening {}
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopListening()
    }
}

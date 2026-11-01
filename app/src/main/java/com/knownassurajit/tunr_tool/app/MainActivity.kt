package com.knownassurajit.tunr_tool.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.knownassurajit.tunr_tool.app.theme.TunrTheme
import com.knownassurajit.tunr_tool.app.ui.TunrApp

/**
 * Manifest needs: <uses-permission android:name="android.permission.RECORD_AUDIO" />
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Content draws behind transparent system bars; Scaffold insets keep controls clear of them.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { TunrTheme { TunrApp() } }
    }

    fun setScreenBrightness(brightness: Float) {
        val lp = window.attributes
        lp.screenBrightness = brightness
        window.attributes = lp
    }
}

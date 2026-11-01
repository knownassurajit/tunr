package com.knownassurajit.tunr_tool.app.theme

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the user turned animations off (Settings ▸ Accessibility ▸ Remove animations). */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Tuner physics. The detector delivers ~21 updates/s, so the springs are deliberately softer than
 * the Expressive "fast spatial" spec: natural frequency ≈ 2.7 Hz low-passes the residual jitter,
 * and ζ ≈ 0.7 gives a ~5% overshoot that reads as a physical needle without ringing.
 */
object TunrMotion {
    const val NEEDLE_STIFFNESS = 280f
    const val NEEDLE_DAMPING = 0.7f
    const val DIAL_STIFFNESS = 180f
    const val DIAL_DAMPING = 0.65f

    fun needle(reduced: Boolean): FiniteAnimationSpec<Float> =
        if (reduced) snap() else spring(NEEDLE_DAMPING, NEEDLE_STIFFNESS, 0.01f)

    fun dialRotation(reduced: Boolean): FiniteAnimationSpec<Float> =
        if (reduced) snap() else spring(DIAL_DAMPING, DIAL_STIFFNESS, 0.01f)
}

/** Observes the system animator-duration scale (0 = animations removed) for the composition's lifetime. */
@Composable
fun rememberReducedMotion(): State<Boolean> {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val state = remember { mutableStateOf(readScale(resolver) == 0f) }
    DisposableEffect(resolver) {
        val uri: Uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { state.value = readScale(resolver) == 0f }
        }
        resolver.registerContentObserver(uri, false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return state
}

private fun readScale(resolver: android.content.ContentResolver): Float =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

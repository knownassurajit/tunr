package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.knownassurajit.tunr_tool.app.model.InstrumentType

/**
 * Exact, minimalist vector instrument silhouettes inspired by contemporary architectural
 * and acoustic line art (as seen in refined industrial design and Pinterest vector references).
 */
@Composable
fun InstrumentGraphic(
    instrumentType: InstrumentType,
    modifier: Modifier = Modifier,
    tintColor: Color = Color(0xFF94A3B8),
    accentColor: Color = Color(0xFF38BDF8),
    size: Dp = 56.dp,
    iconSize: Dp = size
) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height

        when (instrumentType) {
            InstrumentType.GUITAR_ACOUSTIC -> {
                // Hourglass Acoustic Dreadnought Silhouette
                val bodyPath = Path().apply {
                    // Start at neck joint top center
                    moveTo(w * 0.44f, h * 0.15f)
                    lineTo(w * 0.56f, h * 0.15f)
                    // Upper right bout
                    cubicTo(w * 0.72f, h * 0.18f, w * 0.82f, h * 0.32f, w * 0.76f, h * 0.44f)
                    // Right waist inward curve
                    cubicTo(w * 0.70f, h * 0.52f, w * 0.68f, h * 0.56f, w * 0.74f, h * 0.65f)
                    // Lower right wide bout
                    cubicTo(w * 0.86f, h * 0.78f, w * 0.78f, h * 0.92f, w * 0.50f, h * 0.92f)
                    // Lower left wide bout
                    cubicTo(w * 0.22f, h * 0.92f, w * 0.14f, h * 0.78f, w * 0.26f, h * 0.65f)
                    // Left waist inward curve
                    cubicTo(w * 0.32f, h * 0.56f, w * 0.30f, h * 0.52f, w * 0.24f, h * 0.44f)
                    // Upper left bout
                    cubicTo(w * 0.18f, h * 0.32f, w * 0.28f, h * 0.18f, w * 0.44f, h * 0.15f)
                    close()
                }
                drawPath(path = bodyPath, color = tintColor, style = Stroke(width = 2.2f))

                // Acoustic Rosette Soundhole
                drawCircle(
                    color = tintColor,
                    radius = w * 0.11f,
                    center = Offset(w * 0.50f, h * 0.45f),
                    style = Stroke(width = 1.8f)
                )

                // Rectangular Acoustic Pin Bridge
                drawRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.40f, h * 0.72f),
                    size = Size(w * 0.20f, h * 0.05f)
                )

                // 6 Acoustic Strings across body
                val stringSpacing = (w * 0.12f) / 5f
                val startX = w * 0.44f
                for (i in 0 until 6) {
                    val x = startX + i * stringSpacing
                    drawLine(
                        color = tintColor.copy(alpha = 0.35f),
                        start = Offset(x, h * 0.15f),
                        end = Offset(x, h * 0.72f),
                        strokeWidth = 1.0f
                    )
                }
            }

            InstrumentType.GUITAR_ELECTRIC -> {
                // Double Cutaway Solidbody Electric Guitar Silhouette
                val bodyPath = Path().apply {
                    // Neck joint top
                    moveTo(w * 0.44f, h * 0.22f)
                    // Horn right cutaway
                    lineTo(w * 0.55f, h * 0.22f)
                    cubicTo(w * 0.62f, h * 0.15f, w * 0.72f, h * 0.18f, w * 0.72f, h * 0.30f)
                    cubicTo(w * 0.65f, h * 0.36f, w * 0.76f, h * 0.48f, w * 0.78f, h * 0.62f)
                    // Lower right bout
                    cubicTo(w * 0.82f, h * 0.82f, w * 0.68f, h * 0.90f, w * 0.50f, h * 0.90f)
                    // Lower left bout
                    cubicTo(w * 0.32f, h * 0.90f, w * 0.18f, h * 0.82f, w * 0.22f, h * 0.62f)
                    // Left waist & Extended Horn cutaway
                    cubicTo(w * 0.24f, h * 0.48f, w * 0.35f, h * 0.36f, w * 0.28f, h * 0.30f)
                    cubicTo(w * 0.28f, h * 0.16f, w * 0.38f, h * 0.12f, w * 0.44f, h * 0.22f)
                    close()
                }
                drawPath(path = bodyPath, color = tintColor, style = Stroke(width = 2.2f))

                // Dual Pickups (Bridge & Neck)
                drawRect(
                    color = tintColor,
                    topLeft = Offset(w * 0.41f, h * 0.48f),
                    size = Size(w * 0.18f, h * 0.05f),
                    style = Stroke(width = 1.5f)
                )
                drawRect(
                    color = tintColor,
                    topLeft = Offset(w * 0.41f, h * 0.58f),
                    size = Size(w * 0.18f, h * 0.05f),
                    style = Stroke(width = 1.5f)
                )

                // Tune-O-Matic Bridge
                drawRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.42f, h * 0.72f),
                    size = Size(w * 0.16f, h * 0.04f)
                )
            }

            InstrumentType.BASS_GUITAR -> {
                // Offset Contoured Electric Bass Silhouette
                val bodyPath = Path().apply {
                    moveTo(w * 0.46f, h * 0.12f)
                    // Upper horn
                    cubicTo(w * 0.52f, h * 0.06f, w * 0.62f, h * 0.08f, w * 0.64f, h * 0.22f)
                    cubicTo(w * 0.58f, h * 0.35f, w * 0.76f, h * 0.48f, w * 0.78f, h * 0.65f)
                    // Lower bout
                    cubicTo(w * 0.82f, h * 0.86f, w * 0.65f, h * 0.94f, w * 0.48f, h * 0.92f)
                    cubicTo(w * 0.28f, h * 0.90f, w * 0.18f, h * 0.80f, w * 0.22f, h * 0.60f)
                    // Lower horn
                    cubicTo(w * 0.24f, h * 0.42f, w * 0.38f, h * 0.32f, w * 0.34f, h * 0.24f)
                    cubicTo(w * 0.34f, h * 0.16f, w * 0.40f, h * 0.12f, w * 0.46f, h * 0.12f)
                    close()
                }
                drawPath(path = bodyPath, color = tintColor, style = Stroke(width = 2.2f))

                // Split P-Bass Pickup Blocks
                drawRect(
                    color = tintColor,
                    topLeft = Offset(w * 0.40f, h * 0.48f),
                    size = Size(w * 0.10f, h * 0.045f)
                )
                drawRect(
                    color = tintColor,
                    topLeft = Offset(w * 0.50f, h * 0.52f),
                    size = Size(w * 0.10f, h * 0.045f)
                )

                // 4 Heavy Bass Saddles
                drawRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.42f, h * 0.74f),
                    size = Size(w * 0.16f, h * 0.04f)
                )
            }

            InstrumentType.UKULELE -> {
                // Petite Soprano Figure-8 Ukulele Silhouette
                val bodyPath = Path().apply {
                    moveTo(w * 0.44f, h * 0.22f)
                    lineTo(w * 0.56f, h * 0.22f)
                    // Upper rounded bout
                    cubicTo(w * 0.72f, h * 0.26f, w * 0.74f, h * 0.40f, w * 0.68f, h * 0.48f)
                    // Waist
                    cubicTo(w * 0.62f, h * 0.54f, w * 0.66f, h * 0.60f, w * 0.72f, h * 0.68f)
                    // Lower rounded bout
                    cubicTo(w * 0.80f, h * 0.80f, w * 0.70f, h * 0.90f, w * 0.50f, h * 0.90f)
                    cubicTo(w * 0.30f, h * 0.90f, w * 0.20f, h * 0.80f, w * 0.28f, h * 0.68f)
                    // Left waist
                    cubicTo(w * 0.34f, h * 0.60f, w * 0.38f, h * 0.54f, w * 0.32f, h * 0.48f)
                    cubicTo(w * 0.26f, h * 0.40f, w * 0.28f, h * 0.26f, w * 0.44f, h * 0.22f)
                    close()
                }
                drawPath(path = bodyPath, color = tintColor, style = Stroke(width = 2.2f))

                // Centered Ukulele Soundhole
                drawCircle(
                    color = tintColor,
                    radius = w * 0.09f,
                    center = Offset(w * 0.50f, h * 0.46f),
                    style = Stroke(width = 1.8f)
                )

                // Tie-bar bridge
                drawRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.41f, h * 0.72f),
                    size = Size(w * 0.18f, h * 0.045f)
                )
            }

            InstrumentType.CHROMATIC -> {
                // Minimalist Harmonic Wave & Tuning Fork Icon
                val forkPath = Path().apply {
                    moveTo(w * 0.38f, h * 0.20f)
                    lineTo(w * 0.38f, h * 0.50f)
                    cubicTo(w * 0.38f, h * 0.64f, w * 0.62f, h * 0.64f, w * 0.62f, h * 0.50f)
                    lineTo(w * 0.62f, h * 0.20f)
                }
                drawPath(path = forkPath, color = tintColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                drawLine(
                    color = tintColor,
                    start = Offset(w * 0.50f, h * 0.62f),
                    end = Offset(w * 0.50f, h * 0.82f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = accentColor,
                    radius = w * 0.04f,
                    center = Offset(w * 0.50f, h * 0.86f)
                )

                // Resonating sound arc
                val arcPath = Path().apply {
                    moveTo(w * 0.24f, h * 0.30f)
                    cubicTo(w * 0.20f, h * 0.40f, w * 0.20f, h * 0.50f, w * 0.24f, h * 0.60f)
                }
                drawPath(path = arcPath, color = accentColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round))
            }
        }
    }
}

package com.knownassurajit.tunr_tool.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.knownassurajit.tunr_tool.app.R

/**
 * One top bar for both destinations. The wordmark badge uses the cookie shape matching the
 * tuner dial, so the brand and hero element are visibly unified.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TunrTopBar(
    tuningName: String,
    onPresets: () -> Unit,
    onSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tuning_fork),
                        contentDescription = "TUNR tuning fork icon",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    "TUNR",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                )
            }
        },
        actions = {
            FilledTonalButton(
                onClick = onPresets,
                shapes = ButtonDefaults.shapes(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = "Tuning library",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    tuningName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 130.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = "Change tuning",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Calibration and settings")
            }
        },
    )
}

/** Expressive slider: clean track and thumb with generous touch target and accent color matching. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunrSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    colors: SliderColors? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val sliderColors = colors ?: SliderDefaults.colors(
        thumbColor = scheme.primary,
        activeTrackColor = scheme.primary,
        activeTickColor = scheme.onPrimary,
        inactiveTrackColor = scheme.surfaceContainerHighest,
        inactiveTickColor = scheme.onSurfaceVariant.copy(alpha = 0.38f),
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = sliderColors,
        modifier = modifier,
    )
}

/** One card style for every settings group. */
@Composable
fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Converts a graphics-shapes [Morph] at [progress] into a Compose path scaled to [size]. */
fun Morph.toComposePath(progress: Float, size: Size): Path = Path().apply {
    var first = true
    asCubics(progress).forEach { c ->
        if (first) {
            moveTo(c.anchor0X * size.width, c.anchor0Y * size.height)
            first = false
        }
        cubicTo(
            c.control0X * size.width, c.control0Y * size.height,
            c.control1X * size.width, c.control1Y * size.height,
            c.anchor1X * size.width, c.anchor1Y * size.height,
        )
    }
    close()
}

package com.knownassurajit.tunr_tool.app.model

/**
 * Precision tuning modes selectable from Settings.
 */
enum class TuningMode(
    val title: String,
    val subtitle: String,
    val inTuneThresholdCents: Float,
    val showFractionalCents: Boolean
) {
    DEFAULT(
        title = "Standard Tuning",
        subtitle = "Optimal for daily play with ±3.0¢ lock window",
        inTuneThresholdCents = 3.0f,
        showFractionalCents = false
    ),
    HIGH_PRECISION(
        title = "High-Precision Tuning",
        subtitle = "Granular studio grade with ±1.0¢ window and 0.1¢ telemetry",
        inTuneThresholdCents = 1.0f,
        showFractionalCents = true
    )
}

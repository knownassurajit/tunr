package com.knownassurajit.tunr_tool.app.model

/**
 * Types of supported musical instruments.
 */
enum class InstrumentType(
    val title: String,
    val subtitle: String,
    val stringCount: Int
) {
    GUITAR_ACOUSTIC(
        title = "Acoustic Guitar",
        subtitle = "6-String Dreadnought",
        stringCount = 6
    ),
    GUITAR_ELECTRIC(
        title = "Electric Guitar",
        subtitle = "6-String Solidbody",
        stringCount = 6
    ),
    BASS_GUITAR(
        title = "Bass Guitar",
        subtitle = "4-String Electric Bass",
        stringCount = 4
    ),
    UKULELE(
        title = "Ukulele",
        subtitle = "4-String Standard GCEA",
        stringCount = 4
    ),
    CHROMATIC(
        title = "Chromatic Master",
        subtitle = "All 12 Tones (Piano, Wind, Strings)",
        stringCount = 0
    )
}

package com.knownassurajit.tunr_tool.app.theme

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
// Deep AMOLED Canvas & Matte Charcoal Surfaces
// Organized from darkest → lightest for consistent elevation
// ──────────────────────────────────────────────────────────────
val AmoledBlack = Color(0xFF060709)
val MatteBackground = Color(0xFF0B0D12)
val MatteSurface = Color(0xFF10131A)
val MatteSurfaceLow = Color(0xFF141822)
val MatteSurfaceContainer = Color(0xFF191F2C)
val MatteSurfaceContainerHigh = Color(0xFF202738)
val MatteSurfaceContainerHighest = Color(0xFF283144)

// ──────────────────────────────────────────────────────────────
// Monochromatic Matte Blue Spectrum
// ──────────────────────────────────────────────────────────────
val MatteBlue = Color(0xFF38BDF8)
val MatteBlueSubtle = Color(0xFF0284C7)
val MatteBlueContainer = Color(0xFF0F2338)
val OnMatteBlueContainer = Color(0xFFBAE6FD)
val MatteBlueGlow = Color(0x3338BDF8)

// ──────────────────────────────────────────────────────────────
// Monochromatic Matte Emerald Spectrum (In-Tune / Primary)
// ──────────────────────────────────────────────────────────────
val LockEmerald = Color(0xFF10B981)
val LockMint = Color(0xFF34D399)
val LockContainer = Color(0xFF064E3B)
val OnLockContainer = Color(0xFF6EE7B7)
val LockGlow = Color(0x3310B981)

// ──────────────────────────────────────────────────────────────
// Directional Tuning Guidance
// ──────────────────────────────────────────────────────────────
val FlatBlue = Color(0xFF38BDF8)   // Flat: needs tuning UP
val SharpAmber = Color(0xFFFBBF24)  // Sharp: needs tuning DOWN
val WarningRed = Color(0xFFF87171)

// ──────────────────────────────────────────────────────────────
// Typography & Borders — unified across all components
// ──────────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)
val BorderSubtle = Color(0xFF1E293B)
val BorderMedium = Color(0xFF334155)

// ──────────────────────────────────────────────────────────────
// Shared semantic aliases for consistent component styling
// ──────────────────────────────────────────────────────────────
val CardBackground = MatteSurfaceLow
val CardBorder = BorderSubtle
val CardBorderActive = MatteBlue
val CardBorderLocked = LockEmerald
val SectionLabelColor = TextTertiary
val SliderActiveTrack = LockEmerald.copy(alpha = 0.85f)
val SliderInactiveTrack = MatteSurfaceContainer
val SliderThumb = LockMint

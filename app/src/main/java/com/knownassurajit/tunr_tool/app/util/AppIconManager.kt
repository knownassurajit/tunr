package com.knownassurajit.tunr_tool.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.knownassurajit.tunr_tool.app.model.AppThemeScheme
import kotlin.math.pow

object AppIconManager {
    private const val TAG = "AppIconManager"

    private const val ALIAS_EMERALD = ".MainActivityEmerald"
    private const val ALIAS_AZURE = ".MainActivityAzure"
    private const val ALIAS_MONOCHROME = ".MainActivityMonochrome"
    private const val ALIAS_AMBER = ".MainActivityAmber"
    private const val ALIAS_VIOLET = ".MainActivityViolet"
    private const val ALIAS_ROSE = ".MainActivityRose"
    private const val ALIAS_ORANGE = ".MainActivityOrange"

    private val ALL_ALIASES = listOf(
        ALIAS_EMERALD,
        ALIAS_AZURE,
        ALIAS_MONOCHROME,
        ALIAS_AMBER,
        ALIAS_VIOLET,
        ALIAS_ROSE,
        ALIAS_ORANGE,
    )

    private fun rgb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

    private fun red(c: Int): Int = (c shr 16) and 0xFF
    private fun green(c: Int): Int = (c shr 8) and 0xFF
    private fun blue(c: Int): Int = c and 0xFF

    private val PALETTE_MAP = listOf(
        ALIAS_EMERALD to rgb(0xB9, 0xF2, 0x3C), // Emerald Lime
        ALIAS_AZURE to rgb(0x38, 0xBD, 0xF8),   // Electric Cyan / Azure
        ALIAS_MONOCHROME to rgb(0xEE, 0xEE, 0xEE), // Monochrome Slate
        ALIAS_AMBER to rgb(0xFB, 0xBF, 0x24),   // Stage Amber
        ALIAS_VIOLET to rgb(0xA8, 0x55, 0xF7),  // Radiant Violet
        ALIAS_ROSE to rgb(0xF4, 0x3F, 0x5E),    // Crimson Rose
        ALIAS_ORANGE to rgb(0xFB, 0x92, 0x3C),  // Sunset Orange
    )

    fun getTargetAlias(scheme: AppThemeScheme, customHex: String): String {
        return when (scheme) {
            AppThemeScheme.Emerald -> ALIAS_EMERALD
            AppThemeScheme.Azure -> ALIAS_AZURE
            AppThemeScheme.Monochrome -> ALIAS_MONOCHROME
            AppThemeScheme.Amber -> ALIAS_AMBER
            AppThemeScheme.Violet -> ALIAS_VIOLET
            AppThemeScheme.Custom -> {
                val parsed = parseHexColor(customHex) ?: return ALIAS_EMERALD
                val r = red(parsed)
                val g = green(parsed)
                val b = blue(parsed)

                var closestAlias = ALIAS_EMERALD
                var minDistance = Double.MAX_VALUE

                for ((alias, color) in PALETTE_MAP) {
                    val cr = red(color)
                    val cg = green(color)
                    val cb = blue(color)
                    val dist = (r - cr).toDouble().pow(2) + (g - cg).toDouble().pow(2) + (b - cb).toDouble().pow(2)
                    if (dist < minDistance) {
                        minDistance = dist
                        closestAlias = alias
                    }
                }
                closestAlias
            }
        }
    }

    fun updateLauncherIcon(context: Context, scheme: AppThemeScheme, customHex: String) {
        try {
            val targetAliasSuffix = getTargetAlias(scheme, customHex)
            val pm = context.packageManager
            val packageName = context.packageName

            val targetComponent = ComponentName(packageName, "$packageName$targetAliasSuffix")

            // Ensure ONLY targetComponent is enabled and ALL other aliases are explicitly disabled.
            // Do not early-return solely on targetComponent state: another alias might also be enabled
            // (e.g. after install, update, or theme change), creating duplicate icons in the app library/launcher.
            val targetState = pm.getComponentEnabledSetting(targetComponent)
            val hasOtherEnabled = ALL_ALIASES.any { alias ->
                alias != targetAliasSuffix &&
                    pm.getComponentEnabledSetting(ComponentName(packageName, "$packageName$alias")) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            val targetAlreadyEnabled = targetState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (targetState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && targetAliasSuffix == ALIAS_EMERALD)

            if (targetAlreadyEnabled && !hasOtherEnabled) {
                return
            }

            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            for (alias in ALL_ALIASES) {
                if (alias != targetAliasSuffix) {
                    val comp = ComponentName(packageName, "$packageName$alias")
                    if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
            Log.d(TAG, "Switched launcher icon to $targetAliasSuffix and disabled others")
        } catch (e: Exception) {
            Log.w(TAG, "Could not update launcher icon dynamically: ${e.message}")
        }
    }

    fun parseHexColor(hex: String): Int? {
        val clean = hex.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> {
                    val rgbLong = clean.toLong(16)
                    ((0xFFL shl 24) or rgbLong).toInt()
                }
                8 -> clean.toLong(16).toInt()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

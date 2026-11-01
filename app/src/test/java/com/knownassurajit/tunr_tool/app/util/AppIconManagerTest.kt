package com.knownassurajit.tunr_tool.app.util

import com.knownassurajit.tunr_tool.app.model.AppThemeScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppIconManagerTest {

    @Test
    fun testParseHexColorValid() {
        assertNotNull(AppIconManager.parseHexColor("#B9F23C"))
        assertNotNull(AppIconManager.parseHexColor("B9F23C"))
        assertNotNull(AppIconManager.parseHexColor("#38BDF8"))
        assertNotNull(AppIconManager.parseHexColor("FF0055"))
        assertNotNull(AppIconManager.parseHexColor("#FFFF0055"))
    }

    @Test
    fun testParseHexColorInvalid() {
        assertNull(AppIconManager.parseHexColor(""))
        assertNull(AppIconManager.parseHexColor("#12"))
        assertNull(AppIconManager.parseHexColor("ZZZZZZ"))
        assertNull(AppIconManager.parseHexColor("not_a_color"))
    }

    @Test
    fun testPresetThemeAliases() {
        assertEquals(".MainActivityEmerald", AppIconManager.getTargetAlias(AppThemeScheme.Emerald, "#B9F23C"))
        assertEquals(".MainActivityAzure", AppIconManager.getTargetAlias(AppThemeScheme.Azure, "#38BDF8"))
        assertEquals(".MainActivityMonochrome", AppIconManager.getTargetAlias(AppThemeScheme.Monochrome, "#FFFFFF"))
        assertEquals(".MainActivityAmber", AppIconManager.getTargetAlias(AppThemeScheme.Amber, "#FBBF24"))
        assertEquals(".MainActivityViolet", AppIconManager.getTargetAlias(AppThemeScheme.Violet, "#A855F7"))
    }

    @Test
    fun testCustomThemeClosestAliasMapping() {
        // Bright lime should map to Emerald
        assertEquals(".MainActivityEmerald", AppIconManager.getTargetAlias(AppThemeScheme.Custom, "#B0F030"))

        // Sky cyan should map to Azure
        assertEquals(".MainActivityAzure", AppIconManager.getTargetAlias(AppThemeScheme.Custom, "#00C0FF"))

        // Pure white should map to Monochrome
        assertEquals(".MainActivityMonochrome", AppIconManager.getTargetAlias(AppThemeScheme.Custom, "#FFFFFF"))

        // Warm yellow-gold should map to Amber
        assertEquals(".MainActivityAmber", AppIconManager.getTargetAlias(AppThemeScheme.Custom, "#FFCC00"))

        // Radiant purple should map to Violet
        assertEquals(".MainActivityViolet", AppIconManager.getTargetAlias(AppThemeScheme.Custom, "#9933FF"))

        // Vivid crimson red should map to Rose
        assertEquals(".MainActivityRose", AppIconManager.getTargetAlias(AppThemeScheme.Custom, "#FF0033"))

        // Deep sunset orange should map to Orange
        assertEquals(".MainActivityOrange", AppIconManager.getTargetAlias(AppThemeScheme.Custom, "#FF6600"))
    }
}

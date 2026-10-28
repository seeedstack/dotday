package com.dotday.app.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddonTest {
    private val validTheme = """
        {"schemaVersion":1,"id":"you.habit.theme","name":"Night","version":"1.0.0","type":"theme",
         "theme":{"paper":"#111111","card":"#222222","ink":"#FFFFFF","muted":"#AAAAAA",
                  "primary":"#E63946","yellow":"#FFD23F","blue":"#1D4ED8","ok":"#7FD8BE"}}
    """.trimIndent()

    @Test fun acceptsValidManifest() {
        assertIs<AddonParseResult.Ok>(parseAddonManifest(validTheme))
    }

    @Test fun rejectsMalformedJson() {
        assertIs<AddonParseResult.Error>(parseAddonManifest("{not json"))
    }

    @Test fun rejectsUnsupportedSchemaVersion() {
        val r = parseAddonManifest(validTheme.replace("\"schemaVersion\":1", "\"schemaVersion\":99"))
        assertIs<AddonParseResult.Error>(r)
    }

    @Test fun rejectsNonReverseDnsId() {
        val r = parseAddonManifest(validTheme.replace("\"id\":\"you.habit.theme\"", "\"id\":\"theme\""))
        assertIs<AddonParseResult.Error>(r)
    }

    @Test fun rejectsReservedId() {
        val r = parseAddonManifest(validTheme.replace("\"id\":\"you.habit.theme\"", "\"id\":\"dotday.core.foo\""))
        assertIs<AddonParseResult.Error>(r)
    }

    @Test fun rejectsBadSemver() {
        val r = parseAddonManifest(validTheme.replace("\"version\":\"1.0.0\"", "\"version\":\"v1\""))
        assertIs<AddonParseResult.Error>(r)
    }

    @Test fun rejectsUnknownTopLevelField() {
        val r = parseAddonManifest(validTheme.dropLast(1) + ",\"script\":\"alert(1)\"}")
        assertIs<AddonParseResult.Error>(r)
    }

    @Test fun rejectsBadHexColor() {
        val r = parseAddonManifest(validTheme.replace("#111111", "red"))
        assertIs<AddonParseResult.Error>(r)
    }

    @Test fun rejectsOversizedManifest() {
        val r = parseAddonManifest(validTheme + " ".repeat(9_000))
        assertIs<AddonParseResult.Error>(r)
    }

    @Test fun contrastRatioOfBlackOnWhiteIsMax() {
        assertEquals(21.0, contrastRatio("#000000", "#FFFFFF"), absoluteTolerance = 0.01)
    }

    @Test fun lowContrastThemeFailsPassesContrast() {
        val low = (parseAddonManifest(
            validTheme.replace("\"ink\":\"#FFFFFF\"", "\"ink\":\"#F0F0F0\"").replace("\"paper\":\"#111111\"", "\"paper\":\"#FFFFFF\"")
        ) as AddonParseResult.Ok).manifest.theme!!
        assertTrue(!low.passesContrast())
    }

    @Test fun enabledOnlyFeedsRegistry() {
        val ok = (parseAddonManifest(validTheme) as AddonParseResult.Ok).manifest
        val list = listOf(InstalledAddon(ok, enabled = false))
        assertEquals(null, list.activeTheme())
    }
}

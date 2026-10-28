package com.dotday.app.sdk

import kotlin.math.pow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Level A declarative add-ons: themes, dashboard cards, navigation entries.
 * Strict parser — unknown fields are rejected (kotlinx default), not ignored.
 * No scripts, expressions, or executable content anywhere in this schema.
 */
private const val MAX_MANIFEST_CHARS = 8_000
private const val SUPPORTED_SCHEMA_VERSION = 1
private val ID_RE = Regex("^[a-z0-9]+(\\.[a-z0-9-]+)+$")
private val SEMVER_RE = Regex("^\\d+\\.\\d+\\.\\d+$")
private val HEX_RE = Regex("^#[0-9A-Fa-f]{6}$")
private val CONTROL_CHAR_RE = Regex("[\\p{Cc}]")

@Serializable
data class AddonTheme(
    val paper: String, val card: String, val ink: String, val muted: String,
    val primary: String, val yellow: String, val blue: String, val ok: String,
)

@Serializable
data class AddonDashboardCard(val title: String, val text: String)

@Serializable
data class AddonNavEntry(val label: String, val text: String)

@Serializable
data class AddonManifest(
    val schemaVersion: Int,
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val type: String, // "theme" | "dashboard" | "navigation"
    val theme: AddonTheme? = null,
    val dashboardCard: AddonDashboardCard? = null,
    val navEntry: AddonNavEntry? = null,
)

@Serializable
data class InstalledAddon(val manifest: AddonManifest, val enabled: Boolean = true)

sealed class AddonParseResult {
    data class Ok(val manifest: AddonManifest) : AddonParseResult()
    data class Error(val message: String) : AddonParseResult()
}

fun parseAddonManifest(raw: String): AddonParseResult {
    if (raw.length > MAX_MANIFEST_CHARS) return AddonParseResult.Error("Package is too large.")
    val m = try {
        Json.decodeFromString<AddonManifest>(raw)
    } catch (e: Exception) {
        return AddonParseResult.Error("Invalid package: ${e.message ?: "malformed JSON"}")
    }
    if (m.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
        return AddonParseResult.Error("Unsupported schema version ${m.schemaVersion}.")
    }
    if (m.id.length > 64 || !ID_RE.matches(m.id)) return AddonParseResult.Error("Invalid add-on id (expected reverse-DNS, e.g. you.habit.widget).")
    if (m.id == "dotday.core" || m.id.startsWith("dotday.core.")) return AddonParseResult.Error("That id is reserved.")
    if (m.name.isBlank() || m.name.length > 60 || CONTROL_CHAR_RE.containsMatchIn(m.name)) return AddonParseResult.Error("Invalid name.")
    if (!SEMVER_RE.matches(m.version)) return AddonParseResult.Error("Invalid version (expected semver, e.g. 1.0.0).")
    if (m.description.length > 300 || m.author.length > 80) return AddonParseResult.Error("Description or author is too long.")
    when (m.type) {
        "theme" -> {
            val t = m.theme ?: return AddonParseResult.Error("Missing theme block.")
            if (m.dashboardCard != null || m.navEntry != null) return AddonParseResult.Error("Unexpected fields for type 'theme'.")
            val colors = listOf(t.paper, t.card, t.ink, t.muted, t.primary, t.yellow, t.blue, t.ok)
            if (colors.any { !HEX_RE.matches(it) }) return AddonParseResult.Error("Theme colors must be #RRGGBB.")
        }
        "dashboard" -> {
            val d = m.dashboardCard ?: return AddonParseResult.Error("Missing dashboardCard block.")
            if (m.theme != null || m.navEntry != null) return AddonParseResult.Error("Unexpected fields for type 'dashboard'.")
            if (d.title.isBlank() || d.title.length > 40 || d.text.length > 400) return AddonParseResult.Error("dashboardCard fields out of bounds.")
        }
        "navigation" -> {
            val n = m.navEntry ?: return AddonParseResult.Error("Missing navEntry block.")
            if (m.theme != null || m.dashboardCard != null) return AddonParseResult.Error("Unexpected fields for type 'navigation'.")
            if (n.label.isBlank() || n.label.length > 24 || n.text.length > 2000) return AddonParseResult.Error("navEntry fields out of bounds.")
        }
        else -> return AddonParseResult.Error("Unsupported type '${m.type}'.")
    }
    return AddonParseResult.Ok(m)
}

/** WCAG 2.x contrast ratio between two #RRGGBB colors. */
fun contrastRatio(hexA: String, hexB: String): Double {
    fun channel(c: Double) = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    fun relLum(hex: String): Double {
        val v = hex.removePrefix("#")
        val r = v.substring(0, 2).toInt(16) / 255.0
        val g = v.substring(2, 4).toInt(16) / 255.0
        val b = v.substring(4, 6).toInt(16) / 255.0
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }
    val l1 = relLum(hexA) + 0.05
    val l2 = relLum(hexB) + 0.05
    return if (l1 > l2) l1 / l2 else l2 / l1
}

/** AA body-text threshold against both surface colors; auto-fallback applies when this fails. */
fun AddonTheme.passesContrast(): Boolean = contrastRatio(ink, paper) >= 4.5 && contrastRatio(ink, card) >= 4.5

// --- in-process registry: pure views over the persisted, enabled add-on list ---
fun List<InstalledAddon>.activeTheme(): AddonTheme? =
    firstOrNull { it.enabled && it.manifest.type == "theme" }?.manifest?.theme

fun List<InstalledAddon>.activeDashboardCards(): List<AddonDashboardCard> =
    filter { it.enabled && it.manifest.type == "dashboard" }.mapNotNull { it.manifest.dashboardCard }

fun List<InstalledAddon>.activeNavEntries(): List<AddonNavEntry> =
    filter { it.enabled && it.manifest.type == "navigation" }.mapNotNull { it.manifest.navEntry }

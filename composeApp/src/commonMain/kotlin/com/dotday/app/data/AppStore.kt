package com.dotday.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dotday.app.domain.Habit
import com.dotday.app.domain.UserProfile
import com.dotday.app.domain.addDays
import com.dotday.app.domain.isDue
import com.dotday.app.sdk.AddonParseResult
import com.dotday.app.sdk.InstalledAddon
import com.dotday.app.sdk.parseAddonManifest
import com.russhwolf.settings.Settings
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class Persisted(
    val habits: List<Habit> = emptyList(),
    val logs: Map<String, List<LocalDate>> = emptyMap(),
    val demo: Boolean = false,
    val weekStart: Int = 1,
    val calm: Boolean = false,
    val theme: String = "auto",
    val addons: List<InstalledAddon> = emptyList(),
    val profile: UserProfile? = null,
)

@Serializable
private data class Backup(
    val version: Int = 1,
    val habits: List<Habit>,
    val logs: Map<String, List<LocalDate>>,
)

/** Local-first store. Persists to SharedPreferences / NSUserDefaults / java.util.prefs. */
class AppStore(private val settings: Settings) {
    var habits by mutableStateOf<List<Habit>>(emptyList()); private set
    var logs by mutableStateOf<Map<String, Set<LocalDate>>>(emptyMap()); private set
    var demo by mutableStateOf(false); private set
    var weekStart by mutableIntStateOf(1); private set
    var calm by mutableStateOf(false); private set
    var theme by mutableStateOf("auto"); private set
    var addons by mutableStateOf<List<InstalledAddon>>(emptyList()); private set
    var profile by mutableStateOf<UserProfile?>(null); private set

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }

    init {
        runCatching {
            settings.getStringOrNull(KEY)?.let { json.decodeFromString<Persisted>(it) }
        }.getOrNull()?.let { p ->
            habits = p.habits
            logs = p.logs.mapValues { it.value.toSet() }
            demo = p.demo
            weekStart = p.weekStart
            calm = p.calm
            theme = p.theme
            addons = p.addons
            profile = p.profile
        }
    }

    private fun save() {
        runCatching {
            settings.putString(
                KEY,
                json.encodeToString(
                    Persisted(habits, logs.mapValues { it.value.toList() }, demo, weekStart, calm, theme, addons, profile)
                )
            )
        }
    }

    fun done(id: String): Set<LocalDate> = logs[id] ?: emptySet()

    /** Returns true if the date is now checked. */
    fun toggle(id: String, date: LocalDate): Boolean {
        val s = done(id)
        val now = !s.contains(date)
        logs = logs + (id to if (now) s + date else s - date)
        save()
        return now
    }

    fun upsert(h: Habit) {
        habits = if (habits.any { it.id == h.id }) habits.map { if (it.id == h.id) h else it } else habits + h
        save()
    }

    /** Returns the new archived state. */
    fun toggleArchive(id: String, today: LocalDate): Boolean {
        var archivedNow = false
        habits = habits.map {
            if (it.id == id) {
                val n = it.copy(archived = if (it.archived != null) null else today)
                archivedNow = n.archived != null
                n
            } else it
        }
        save()
        return archivedNow
    }

    fun delete(id: String) {
        habits = habits.filter { it.id != id }
        logs = logs - id
        save()
    }

    /** Undoes delete(): re-adds a previously deleted habit and its check-ins. */
    fun restore(h: Habit, log: Set<LocalDate>) {
        habits = habits + h
        logs = logs + (h.id to log)
        save()
    }

    fun wipe() {
        habits = emptyList(); logs = emptyMap(); demo = false; save()
    }

    /** One atomic write: the finished profile and its starter habits land together or not at all. */
    fun completeOnboarding(profile: UserProfile, starterHabits: List<Habit>) {
        habits = habits + starterHabits
        this.profile = profile
        save()
    }

    fun updateProfile(transform: (UserProfile) -> UserProfile) {
        val current = profile ?: return
        profile = transform(current)
        save()
    }

    fun updateTheme(v: String) { theme = v; save() }
    fun updateWeekStart(v: Int) { weekStart = v; save() }
    fun updateCalm(v: Boolean) { calm = v; save() }

    fun loadDemo(today: LocalDate) {
        val st = today.addDays(-60)
        fun mk(id: String, name: String, cat: String, c: String, days: List<Int>) =
            Habit(id, name, cat, c, days, st)
        val hs = listOf(
            mk("d1", "Drink water", "Health", "#7FD8BE", listOf(0, 1, 2, 3, 4, 5, 6)),
            mk("d2", "Run 5 km", "Fitness", "#E63946", listOf(1, 3, 5)),
            mk("d3", "Read 20 pages", "Learning", "#1D4ED8", listOf(0, 1, 2, 3, 4, 5, 6)),
            mk("d4", "Meditate", "Mindfulness", "#9B5DE5", listOf(1, 2, 3, 4, 5)),
        )
        val p = mapOf("d1" to .85, "d2" to .7, "d3" to .6, "d4" to .75)
        var seed = 7L
        fun rnd(): Double { seed = (seed * 16807L) % 2147483647L; return seed / 2147483647.0 }
        val nl = mutableMapOf<String, Set<LocalDate>>()
        for (h in hs) {
            val s = mutableSetOf<LocalDate>()
            var d = st
            while (d < today) {
                if (isDue(h, d) && rnd() < p.getValue(h.id)) s.add(d)
                d = d.addDays(1)
            }
            nl[h.id] = s
        }
        habits = hs; logs = nl; demo = true; save()
    }

    /** Parses, validates, and installs a pasted add-on manifest. Returns a user-facing status message. */
    fun installAddon(raw: String): String = when (val r = parseAddonManifest(raw)) {
        is AddonParseResult.Error -> r.message
        is AddonParseResult.Ok -> {
            if (addons.any { it.manifest.id == r.manifest.id }) {
                "An add-on with id ${r.manifest.id} is already installed."
            } else {
                addons = addons + InstalledAddon(r.manifest)
                save()
                "Installed ${r.manifest.name}."
            }
        }
    }

    fun setAddonEnabled(id: String, enabled: Boolean) {
        addons = addons.map { if (it.manifest.id == id) it.copy(enabled = enabled) else it }
        save()
    }

    /** Uninstall always deletes the add-on's own settings; it never touches core habit/log data. */
    fun uninstallAddon(id: String) {
        addons = addons.filter { it.manifest.id != id }
        save()
    }

    fun exportJson(): String =
        prettyJson.encodeToString(Backup(1, habits, logs.mapValues { it.value.toList() }))

    private companion object { const val KEY = "dotday.proto.v1" }
}

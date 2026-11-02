package com.dotday.app.data

import com.dotday.app.domain.ActivityLevel
import com.dotday.app.domain.Focus
import com.dotday.app.domain.Gender
import com.dotday.app.domain.Habit
import com.dotday.app.domain.UnitSystem
import com.dotday.app.domain.UserProfile
import com.russhwolf.settings.Settings
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Minimal in-memory [Settings]; the project has no test artifact for multiplatform-settings. */
private class FakeSettings : Settings {
    private val map = mutableMapOf<String, Any?>()
    override val keys: Set<String> get() = map.keys
    override val size: Int get() = map.size
    override fun clear() = map.clear()
    override fun remove(key: String) { map.remove(key) }
    override fun hasKey(key: String) = map.containsKey(key)
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun getInt(key: String, defaultValue: Int) = map[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String) = map[key] as? Int
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun getLong(key: String, defaultValue: Long) = map[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String) = map[key] as? Long
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String, defaultValue: String) = map[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String) = map[key] as? String
    override fun putFloat(key: String, value: Float) { map[key] = value }
    override fun getFloat(key: String, defaultValue: Float) = map[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String) = map[key] as? Float
    override fun putDouble(key: String, value: Double) { map[key] = value }
    override fun getDouble(key: String, defaultValue: Double) = map[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String) = map[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean) = map[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String) = map[key] as? Boolean
}

class AppStoreTest {
    private val today = LocalDate(2026, 1, 1)
    private fun habit(id: String = "h1") = Habit(id, "Run", "Fitness", "#E63946", listOf(0, 1, 2, 3, 4, 5, 6), today)

    private fun profile(name: String = "Ada") = UserProfile(
        name = name, birthYear = 2000, gender = Gender.PREFER_NOT_TO_SAY,
        heightCm = 170f, weightKg = 65f, unitSystem = UnitSystem.METRIC,
        activityLevel = ActivityLevel.MODERATE,
        wakeTime = LocalTime(7, 0), sleepTime = LocalTime(23, 0), reminderTime = LocalTime(9, 0),
        focus = Focus.GYM, avatarId = "avatar_01", accentHex = "#E63946",
        notificationsGranted = true, createdAt = Instant.fromEpochMilliseconds(0),
    )

    @Test fun toggleArchiveTogglesAndKeepsTheHabit() {
        val store = AppStore(FakeSettings())
        store.upsert(habit())
        assertTrue(store.toggleArchive("h1", today))
        assertEquals(today, store.habits.single().archived)
        assertFalse(store.toggleArchive("h1", today))
        assertEquals(null, store.habits.single().archived)
    }

    @Test fun deleteThenRestoreUndoesTheDeletion() {
        val store = AppStore(FakeSettings())
        val h = habit()
        store.upsert(h)
        store.toggle("h1", today)
        val log = store.done("h1")

        store.delete("h1")
        assertTrue(store.habits.isEmpty())
        assertTrue(store.done("h1").isEmpty())

        store.restore(h, log)
        assertEquals(h, store.habits.single())
        assertEquals(log, store.done("h1"))
    }

    @Test fun completeOnboardingSetsProfileAndHabitsAndPersistsBoth() {
        val settings = FakeSettings()
        val store = AppStore(settings)
        val p = profile()
        store.completeOnboarding(p, listOf(habit("s1")))

        val reloaded = AppStore(settings)
        assertEquals(p, reloaded.profile)
        assertEquals("s1", reloaded.habits.single().id)
    }

    @Test fun updateProfileTransformsAnExistingProfile() {
        val store = AppStore(FakeSettings())
        store.completeOnboarding(profile("Ada"), emptyList())
        store.updateProfile { it.copy(name = "Grace") }
        assertEquals("Grace", store.profile?.name)
    }

    @Test fun updateProfileOnNullProfileIsANoOp() {
        val store = AppStore(FakeSettings())
        store.updateProfile { it.copy(name = "Grace") }
        assertNull(store.profile)
    }
}

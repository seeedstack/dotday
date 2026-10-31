package com.dotday.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dotday.app.domain.ActivityLevel
import com.dotday.app.domain.Focus
import com.dotday.app.domain.Gender
import com.dotday.app.domain.STARTER_HABITS
import com.dotday.app.domain.UnitSystem
import com.dotday.app.domain.UserProfile
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

const val ONBOARDING_STEP_COUNT = 12

/** Transient wizard-in-progress state; not persisted, mirrors [UiState]'s plain-class shape. */
class OnboardingState {
    var step by mutableIntStateOf(0)
    var name by mutableStateOf("")
    var birthYear by mutableIntStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()).year - 25)
    var gender by mutableStateOf<Gender?>(null)
    var heightCm by mutableStateOf(170f)
    var weightKg by mutableStateOf(65f)
    var unitSystem by mutableStateOf(UnitSystem.METRIC)
    var activityLevel by mutableStateOf<ActivityLevel?>(null)
    var wakeTime by mutableStateOf(LocalTime(7, 0))
    var sleepTime by mutableStateOf(LocalTime(23, 0))
    var reminderTime by mutableStateOf(LocalTime(9, 0))
    var focus by mutableStateOf<Focus?>(null)
    var selectedHabitNames by mutableStateOf<Set<String>>(emptySet())
    var avatarId by mutableStateOf<String?>(null)
    var accentHex by mutableStateOf<String?>(null)
    var notificationsGranted by mutableStateOf(false)

    fun next() { step = (step + 1).coerceAtMost(ONBOARDING_STEP_COUNT - 1) }
    fun back() { step = (step - 1).coerceAtLeast(0) }

    fun toProfile(granted: Boolean): UserProfile {
        val f = focus ?: Focus.GYM
        val names = selectedHabitNames.ifEmpty { setOf(STARTER_HABITS.getValue(f).first()) }
        return UserProfile(
            name = name.trim(),
            birthYear = birthYear,
            gender = gender ?: Gender.PREFER_NOT_TO_SAY,
            heightCm = heightCm,
            weightKg = weightKg,
            unitSystem = unitSystem,
            activityLevel = activityLevel ?: ActivityLevel.MODERATE,
            wakeTime = wakeTime,
            sleepTime = sleepTime,
            reminderTime = reminderTime,
            focus = f,
            avatarId = avatarId ?: "avatar_01",
            accentHex = accentHex ?: "#E63946",
            notificationsGranted = granted,
            createdAt = Clock.System.now(),
        ).also { selectedHabitNames = names }
    }
}

package com.dotday.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileTest {
    @Test fun birthYearRangeIs100To13YearsBeforeCurrent() {
        assertEquals(1926, minBirthYear(2026))
        assertEquals(2013, maxBirthYear(2026))
    }

    @Test fun heightRoundTripsAtLowerBound() {
        val (ft, inch) = cmToFtIn(100f)
        assertTrue(kotlin.math.abs(ftInToCm(ft, inch) - 100f) < 1f)
    }

    @Test fun heightRoundTripsAtUpperBound() {
        val (ft, inch) = cmToFtIn(250f)
        assertTrue(kotlin.math.abs(ftInToCm(ft, inch) - 250f) < 1f)
    }

    @Test fun weightRoundTripsAtLowerBound() {
        assertTrue(kotlin.math.abs(lbToKg(kgToLb(30f)) - 30f) < 0.1f)
    }

    @Test fun weightRoundTripsAtUpperBound() {
        assertTrue(kotlin.math.abs(lbToKg(kgToLb(300f)) - 300f) < 0.1f)
    }

    @Test fun nameValidAtLengthOneAndTwentyFour() {
        assertTrue(isValidName("a"))
        assertTrue(isValidName("a".repeat(24)))
    }

    @Test fun nameInvalidAtLengthZeroAndTwentyFive() {
        assertFalse(isValidName(""))
        assertFalse(isValidName("a".repeat(25)))
    }

    @Test fun scheduleInvalidWhenWakeEqualsSleep() {
        val t = kotlinx.datetime.LocalTime(7, 0)
        assertFalse(isValidSchedule(t, t))
    }

    @Test fun scheduleValidWhenSleepClockTimeIsBeforeWake() {
        // Sleep at 23:30, wake at 06:00 the next day: still a valid (differing) pair.
        val wake = kotlinx.datetime.LocalTime(6, 0)
        val sleep = kotlinx.datetime.LocalTime(23, 30)
        assertTrue(isValidSchedule(wake, sleep))
    }

    @Test fun starterHabitsHasThreeToFiveNamesForEveryFocus() {
        for (f in Focus.entries) {
            val names = STARTER_HABITS.getValue(f)
            assertTrue(names.size in 3..5, "Focus $f has ${names.size} starter habits")
        }
    }

    @Test fun toggleStarterHabitRefusesToDropTheLastSelected() {
        assertEquals(setOf("Hit the gym"), toggleStarterHabit(setOf("Hit the gym"), "Hit the gym"))
    }

    @Test fun toggleStarterHabitRemovesWhenMoreThanOneSelected() {
        assertEquals(setOf("A"), toggleStarterHabit(setOf("A", "B"), "B"))
    }

    @Test fun toggleStarterHabitAddsWhenAbsent() {
        assertEquals(setOf("A", "B"), toggleStarterHabit(setOf("A"), "B"))
    }

    private val validArgs = StepValidationArgs(
        name = "Ada", birthYear = 2000, currentYear = 2026, gender = Gender.FEMALE,
        heightCm = 170f, weightKg = 65f, activityLevel = ActivityLevel.MODERATE,
        wakeTime = kotlinx.datetime.LocalTime(7, 0), sleepTime = kotlinx.datetime.LocalTime(23, 0),
        focus = Focus.GYM, selectedHabitNames = setOf("Hit the gym"), avatarId = "avatar_01",
        notificationRequested = true,
    )

    @Test fun isStepValidNameStepRejectsEmptyAcceptsNonEmpty() {
        assertFalse(isStepValid(1, validArgs.copy(name = "")))
        assertTrue(isStepValid(1, validArgs.copy(name = "Ada")))
    }

    @Test fun isStepValidBirthYearStepRejectsOutOfRange() {
        assertFalse(isStepValid(2, validArgs.copy(birthYear = 1800)))
        assertTrue(isStepValid(2, validArgs.copy(birthYear = 2000)))
    }

    @Test fun isStepValidGenderStepRequiresASelection() {
        assertFalse(isStepValid(3, validArgs.copy(gender = null)))
        assertTrue(isStepValid(3, validArgs.copy(gender = Gender.MALE)))
    }

    @Test fun isStepValidBodyStatsStepRejectsOutOfRangeHeightOrWeight() {
        assertFalse(isStepValid(4, validArgs.copy(heightCm = 50f)))
        assertFalse(isStepValid(4, validArgs.copy(weightKg = 10f)))
        assertTrue(isStepValid(4, validArgs))
    }

    @Test fun isStepValidActivityStepRequiresASelection() {
        assertFalse(isStepValid(5, validArgs.copy(activityLevel = null)))
        assertTrue(isStepValid(5, validArgs.copy(activityLevel = ActivityLevel.LIGHT)))
    }

    @Test fun isStepValidRoutineStepRejectsEqualWakeAndSleep() {
        val t = kotlinx.datetime.LocalTime(7, 0)
        assertFalse(isStepValid(6, validArgs.copy(wakeTime = t, sleepTime = t)))
        assertTrue(isStepValid(6, validArgs))
    }

    @Test fun isStepValidFocusStepRequiresASelection() {
        assertFalse(isStepValid(7, validArgs.copy(focus = null)))
        assertTrue(isStepValid(7, validArgs.copy(focus = Focus.BOOK)))
    }

    @Test fun isStepValidStarterHabitsStepRequiresAtLeastOne() {
        assertFalse(isStepValid(8, validArgs.copy(selectedHabitNames = emptySet())))
        assertTrue(isStepValid(8, validArgs))
    }

    @Test fun isStepValidAvatarStepRequiresASelection() {
        assertFalse(isStepValid(9, validArgs.copy(avatarId = null)))
        assertTrue(isStepValid(9, validArgs.copy(avatarId = "avatar_02")))
    }

    @Test fun isStepValidNotificationsStepRequiresTheRequestToHaveBeenMade() {
        assertFalse(isStepValid(10, validArgs.copy(notificationRequested = false)))
        assertTrue(isStepValid(10, validArgs))
    }

    @Test fun isStepValidWelcomeAndDoneStepsAreAlwaysValid() {
        assertTrue(isStepValid(0, validArgs))
        assertTrue(isStepValid(11, validArgs))
    }

    @Test fun clampHeightCmKeepsLowestImperialWheelValueInRange() {
        // 3 ft 0 in converts to under 100cm; the UI must not be able to produce an invalid height.
        assertEquals(100f, clampHeightCm(ftInToCm(3, 0f)))
    }

    @Test fun clampHeightCmLeavesAnInRangeValueUnchanged() {
        assertEquals(170f, clampHeightCm(170f))
    }

    @Test fun clampWeightKgKeepsLowestImperialWheelValueInRange() {
        // 66 lb converts to under 30kg; the UI must not be able to produce an invalid weight.
        assertEquals(30f, clampWeightKg(lbToKg(66f)))
    }

    @Test fun clampWeightKgLeavesAnInRangeValueUnchanged() {
        assertEquals(65f, clampWeightKg(65f))
    }
}

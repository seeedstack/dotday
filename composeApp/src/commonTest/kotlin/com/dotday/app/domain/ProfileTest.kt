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
}

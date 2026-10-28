package com.dotday.app.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DomainTest {
    private fun habit(id: String, start: LocalDate, archived: LocalDate? = null, days: List<Int> = (0..6).toList()) =
        Habit(id, id, "Health", "#E63946", days, start, archived)

    @Test fun dayProgressRoundsOneThirdDown() {
        val d = LocalDate(2026, 10, 26)
        val start = LocalDate(2026, 1, 1)
        val habits = listOf(habit("a", start), habit("b", start), habit("c", start))
        val done = mapOf("a" to setOf(d))
        assertEquals(33, dayProgress(habits, { done[it.id] ?: emptySet() }, d))
    }

    @Test fun dayProgressRoundsTwoThirdsUp() {
        val d = LocalDate(2026, 10, 26)
        val start = LocalDate(2026, 1, 1)
        val habits = listOf(habit("a", start), habit("b", start), habit("c", start))
        val done = mapOf("a" to setOf(d), "b" to setOf(d))
        assertEquals(67, dayProgress(habits, { done[it.id] ?: emptySet() }, d))
    }

    @Test fun dayProgressNullWhenNothingDue() {
        val d = LocalDate(2026, 10, 26)
        assertNull(dayProgress(emptyList(), { emptySet() }, d))
    }

    @Test fun dayProgressExcludesArchivedAndNotYetStartedHabits() {
        val d = LocalDate(2026, 10, 26)
        val archived = habit("archived", LocalDate(2026, 1, 1), archived = d)
        val notStarted = habit("future", LocalDate(2026, 11, 1))
        val active = habit("active", LocalDate(2026, 1, 1))
        val done = mapOf("active" to setOf(d))
        assertEquals(100, dayProgress(listOf(archived, notStarted, active), { done[it.id] ?: emptySet() }, d))
    }

    @Test fun recentDatesIsAscendingAndEndsAtToday() {
        val today = LocalDate(2026, 10, 26)
        val dates = recentDates(today, 30)
        assertEquals(30, dates.size)
        assertEquals(today, dates.last())
        for (i in 1 until dates.size) assertEquals(dates[i - 1].addDays(1), dates[i])
    }

    @Test fun recentDatesCrossesMonthAndYearBoundary() {
        val today = LocalDate(2026, 1, 5)
        val dates = recentDates(today, 30)
        assertEquals(LocalDate(2025, 12, 7), dates.first())
        assertEquals(today, dates.last())
    }
}

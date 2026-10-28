package com.dotday.app.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val category: String,
    val color: String,          // "#RRGGBB"
    val days: List<Int>,        // 0 = Sunday ... 6 = Saturday
    val start: LocalDate,
    val archived: LocalDate? = null,
    val desc: String = "",
)

/** Sunday = 0 ... Saturday = 6 (same as JS getDay). */
fun LocalDate.dow(): Int = dayOfWeek.isoDayNumber % 7
fun LocalDate.addDays(n: Int): LocalDate = plus(n, DateTimeUnit.DAY)

fun isDue(h: Habit, s: LocalDate): Boolean {
    val a = h.archived
    return s >= h.start && (a == null || s < a) && h.days.contains(s.dow())
}

fun currentStreak(h: Habit, done: Set<LocalDate>, today: LocalDate): Int {
    var n = 0
    var d = today
    while (d >= h.start) {
        if (isDue(h, d)) {
            if (done.contains(d)) n++ else if (d != today) break
        }
        d = d.addDays(-1)
    }
    return n
}

fun longestStreak(h: Habit, done: Set<LocalDate>, today: LocalDate): Int {
    var best = 0
    var run = 0
    var d = h.start
    while (d <= today) {
        if (isDue(h, d)) {
            if (done.contains(d)) {
                run++
                best = max(best, run)
            } else if (d != today) run = 0
        }
        d = d.addDays(1)
    }
    return best
}

data class Rate(val due: Int, val done: Int, val pct: Int?)

fun rate(habits: List<Habit>, logs: Map<String, Set<LocalDate>>, from: LocalDate, to: LocalDate): Rate {
    var due = 0
    var dn = 0
    for (h in habits) {
        val set = logs[h.id] ?: emptySet()
        var d = from
        while (d <= to) {
            if (isDue(h, d)) {
                due++
                if (set.contains(d)) dn++
            }
            d = d.addDays(1)
        }
    }
    return Rate(due, dn, if (due > 0) (dn * 100.0 / due).roundToInt() else null)
}

fun weekOf(s: LocalDate, weekStart: Int): List<LocalDate> {
    val off = (s.dow() - weekStart + 7) % 7
    val a = s.addDays(-off)
    return (0..6).map { a.addDays(it) }
}

/** Rounded completion percentage for `date`, or null when nothing is due. */
fun dayProgress(habits: List<Habit>, doneFor: (Habit) -> Set<LocalDate>, date: LocalDate): Int? {
    val due = habits.filter { isDue(it, date) }
    if (due.isEmpty()) return null
    val done = due.count { doneFor(it).contains(date) }
    return (done * 100.0 / due.size).roundToInt()
}

/** The last `count` days up to and including `today`, ascending. */
fun recentDates(today: LocalDate, count: Int): List<LocalDate> = (count - 1 downTo 0).map { today.addDays(-it) }

/* ===== Copy pack: Pop Press ===== */
object Copy {
    val BURSTS = listOf("POW!", "ZAP!", "BAM!")
    const val MILESTONE = "ON A ROLL!"
    const val ALL = "LEVEL UP!"
    const val STREAK = ""
    const val STAMP = "DONE"
}

val CATS: Map<String, String> = linkedMapOf(
    "Health" to "♥", "Fitness" to "⚡", "Learning" to "✎",
    "Work" to "▣", "Mindfulness" to "☯", "Personal" to "★",
)
val PAL = listOf(
    "#E63946" to "Red", "#FFD23F" to "Yellow", "#1D4ED8" to "Blue",
    "#7FD8BE" to "Mint", "#9B5DE5" to "Purple", "#FF8C42" to "Orange",
)
val DN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
val DAY_FULL = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
val MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

fun sched(h: Habit): String =
    if (h.days.size == 7) "Every day" else h.days.joinToString(", ") { DN[it] }

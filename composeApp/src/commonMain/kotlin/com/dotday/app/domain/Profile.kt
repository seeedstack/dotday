package com.dotday.app.domain

import com.dotday.app.resources.Res
import com.dotday.app.resources.avatar_01
import com.dotday.app.resources.avatar_02
import com.dotday.app.resources.avatar_03
import com.dotday.app.resources.avatar_04
import com.dotday.app.resources.avatar_05
import com.dotday.app.resources.avatar_06
import com.dotday.app.resources.avatar_07
import com.dotday.app.resources.avatar_08
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource

enum class Gender { MALE, FEMALE, NON_BINARY, PREFER_NOT_TO_SAY }
enum class UnitSystem { METRIC, IMPERIAL }
enum class ActivityLevel { COUCH_POTATO, LIGHT, MODERATE, VERY_ACTIVE, BEAST_MODE }

enum class Focus(val label: String) {
    GYM("Gym"), MOVIE("Movie"), BOOK("Book"),
    FINANCE("Finance"), GROCERIES("Groceries"), RECIPES("Recipes"),
}

@Serializable
data class UserProfile(
    val name: String,
    val birthYear: Int,
    val gender: Gender,
    val heightCm: Float,
    val weightKg: Float,
    val unitSystem: UnitSystem,
    val activityLevel: ActivityLevel,
    val wakeTime: LocalTime,
    val sleepTime: LocalTime,
    val reminderTime: LocalTime,
    val focus: Focus,
    val avatarId: String,
    val accentHex: String,
    val notificationsGranted: Boolean,
    val createdAt: Instant,
)

data class Avatar(val id: String, val drawable: DrawableResource)

val AVATARS: List<Avatar> = listOf(
    Avatar("avatar_01", Res.drawable.avatar_01),
    Avatar("avatar_02", Res.drawable.avatar_02),
    Avatar("avatar_03", Res.drawable.avatar_03),
    Avatar("avatar_04", Res.drawable.avatar_04),
    Avatar("avatar_05", Res.drawable.avatar_05),
    Avatar("avatar_06", Res.drawable.avatar_06),
    Avatar("avatar_07", Res.drawable.avatar_07),
    Avatar("avatar_08", Res.drawable.avatar_08),
)

val STARTER_HABITS: Map<Focus, List<String>> = mapOf(
    Focus.GYM to listOf("Hit the gym", "10k steps", "Drink 3L water", "Stretch 10 min"),
    Focus.MOVIE to listOf("Watch 1 film", "Log a review", "No-phone movie time"),
    Focus.BOOK to listOf("Read 20 pages", "Read before bed", "Write 1 takeaway"),
    Focus.FINANCE to listOf("Log expenses", "No impulse buys", "Check budget weekly"),
    Focus.GROCERIES to listOf("Plan weekly list", "Check pantry", "Stick to list"),
    Focus.RECIPES to listOf("Cook 1 new recipe a week", "Meal prep Sunday", "Save a recipe"),
)

fun minBirthYear(currentYear: Int): Int = currentYear - 100
fun maxBirthYear(currentYear: Int): Int = currentYear - 13

fun cmToFtIn(cm: Float): Pair<Int, Float> {
    val totalInches = cm / 2.54f
    val ft = (totalInches / 12f).toInt()
    val inch = totalInches - ft * 12f
    return ft to inch
}

fun ftInToCm(ft: Int, inch: Float): Float = (ft * 12f + inch) * 2.54f

fun kgToLb(kg: Float): Float = kg * 2.2046226f
fun lbToKg(lb: Float): Float = lb / 2.2046226f

fun isValidName(s: String): Boolean = s.trim().length in 1..24
fun isValidHeightCm(cm: Float): Boolean = cm in 100f..250f
fun isValidWeightKg(kg: Float): Boolean = kg in 30f..300f
fun isValidSchedule(wake: LocalTime, sleep: LocalTime): Boolean = wake != sleep

/** Keeps an imperial-to-metric conversion inside the valid height range (e.g. 3ft rounds under 100cm). */
fun clampHeightCm(cm: Float): Float = cm.coerceIn(100f, 250f)

/** Keeps an imperial-to-metric conversion inside the valid weight range (e.g. 66lb rounds under 30kg). */
fun clampWeightKg(kg: Float): Float = kg.coerceIn(30f, 300f)

/** Toggles [name] in [selected]; refuses to drop the last remaining habit. */
fun toggleStarterHabit(selected: Set<String>, name: String): Set<String> =
    if (selected.contains(name)) {
        if (selected.size > 1) selected - name else selected
    } else {
        selected + name
    }

/** Every field `isStepValid` needs across the 12 onboarding steps, bundled so call sites read as one value. */
data class StepValidationArgs(
    val name: String,
    val birthYear: Int,
    val currentYear: Int,
    val gender: Gender?,
    val heightCm: Float,
    val weightKg: Float,
    val activityLevel: ActivityLevel?,
    val wakeTime: LocalTime,
    val sleepTime: LocalTime,
    val focus: Focus?,
    val selectedHabitNames: Set<String>,
    val avatarId: String?,
    val notificationRequested: Boolean,
)

/** Step indices: 0 Welcome, 1 Name, 2 BirthYear, 3 Gender, 4 BodyStats, 5 Activity, 6 Routine,
 *  7 Focus, 8 StarterHabits, 9 Avatar, 10 Notifications, 11 Done. */
fun isStepValid(step: Int, a: StepValidationArgs): Boolean = when (step) {
    0 -> true
    1 -> isValidName(a.name)
    2 -> a.birthYear in minBirthYear(a.currentYear)..maxBirthYear(a.currentYear)
    3 -> a.gender != null
    4 -> isValidHeightCm(a.heightCm) && isValidWeightKg(a.weightKg)
    5 -> a.activityLevel != null
    6 -> isValidSchedule(a.wakeTime, a.sleepTime)
    7 -> a.focus != null
    8 -> a.selectedHabitNames.isNotEmpty()
    9 -> a.avatarId != null
    10 -> a.notificationRequested
    11 -> true
    else -> false
}

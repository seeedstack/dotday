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

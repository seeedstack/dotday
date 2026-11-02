package com.dotday.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.domain.ActivityLevel
import com.dotday.app.domain.AVATARS
import com.dotday.app.domain.Copy
import com.dotday.app.domain.Focus
import com.dotday.app.domain.Gender
import com.dotday.app.domain.Habit
import com.dotday.app.domain.PAL
import com.dotday.app.domain.STARTER_HABITS
import com.dotday.app.domain.StepValidationArgs
import com.dotday.app.domain.UnitSystem
import com.dotday.app.domain.clampHeightCm
import com.dotday.app.domain.clampWeightKg
import com.dotday.app.domain.cmToFtIn
import com.dotday.app.domain.ftInToCm
import com.dotday.app.domain.isStepValid
import com.dotday.app.domain.kgToLb
import com.dotday.app.domain.lbToKg
import com.dotday.app.domain.maxBirthYear
import com.dotday.app.domain.minBirthYear
import com.dotday.app.domain.toggleStarterHabit
import com.dotday.app.platform.rememberNotificationPermissionRequester
import com.dotday.app.platform.scheduleDailyReminder
import com.dotday.app.resources.Res
import com.dotday.app.resources.ic_focus_book
import com.dotday.app.resources.ic_focus_finance
import com.dotday.app.resources.ic_focus_groceries
import com.dotday.app.resources.ic_focus_gym
import com.dotday.app.resources.ic_focus_movie
import com.dotday.app.resources.ic_focus_recipes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun OnboardingScreen(store: AppStore, ui: UiState) {
    val state = remember { OnboardingState() }
    val currentYear = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()).year }
    val args = StepValidationArgs(
        name = state.name, birthYear = state.birthYear, currentYear = currentYear, gender = state.gender,
        heightCm = state.heightCm, weightKg = state.weightKg, activityLevel = state.activityLevel,
        wakeTime = state.wakeTime, sleepTime = state.sleepTime, focus = state.focus,
        selectedHabitNames = state.selectedHabitNames, avatarId = state.avatarId,
        notificationRequested = state.notificationsRequested,
    )
    val valid = isStepValid(state.step, args)

    fun finishOnboarding() {
        val profile = state.toProfile(state.notificationsGranted)
        if (state.notificationsGranted) scheduleDailyReminder(profile.reminderTime.hour, profile.reminderTime.minute)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val habits = state.selectedHabitNames.map { name ->
            Habit(
                id = "h" + Clock.System.now().toEpochMilliseconds().toString(36) + name.hashCode().toString(36),
                name = name, category = profile.focus.label, color = profile.accentHex,
                days = (0..6).toList(), start = today,
            )
        }
        store.completeOnboarding(profile, habits)
        ui.fireBurst(Copy.ALL)
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val minH = maxHeight
                Column(
                    Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = minH)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Panel(modifier = Modifier.fillMaxWidth()) {
                        AnimatedContent(
                            state.step,
                            transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
                        ) { step ->
                            when (step) {
                                0 -> WelcomeStep()
                                1 -> NameStep(state)
                                2 -> BirthYearStep(state, currentYear)
                                3 -> GenderStep(state)
                                4 -> BodyStatsStep(state)
                                5 -> ActivityLevelStep(state)
                                6 -> RoutineStep(state)
                                7 -> FocusStep(state)
                                8 -> StarterHabitsStep(state)
                                9 -> AvatarThemeStep(state)
                                10 -> NotificationsStep(state)
                                11 -> DoneStep(state)
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.step > 0) DButton("Back", kind = BtnKind.Alt, modifier = Modifier.weight(1f)) { state.back() }
            when (state.step) {
                0 -> DButton(Copy.OB_WELCOME_CTA, modifier = Modifier.weight(1f)) { state.next() }
                ONBOARDING_STEP_COUNT - 1 -> DButton(Copy.OB_DONE_CTA, modifier = Modifier.weight(1f)) { finishOnboarding() }
                else -> DButton("NEXT!", modifier = Modifier.weight(1f), enabled = valid) { state.next() }
            }
        }
        ProgressDots(state.step, ONBOARDING_STEP_COUNT, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp))
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        H2(Copy.OB_WELCOME_TITLE, 40.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        T(Copy.OB_WELCOME_SUB, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun NameStep(state: OnboardingState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_NAME_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        T(Copy.OB_NAME_SUB, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        DField(state.name, { state.name = it }, maxLen = 24)
        if (state.name.isNotEmpty()) {
            ErrText(
                if (state.name.trim().length in 1..24) "" else "1–24 characters, please.",
                align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BirthYearStep(state: OnboardingState, currentYear: Int) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_BIRTHYEAR_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        WheelPicker(
            minBirthYear(currentYear)..maxBirthYear(currentYear), state.birthYear,
            { state.birthYear = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Meta("Age: ${currentYear - state.birthYear}", Modifier.fillMaxWidth().padding(top = 4.dp), align = TextAlign.Center)
    }
}

@Composable
private fun BodyStatsStep(state: OnboardingState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_BODYSTATS_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        TilePicker(
            UnitSystem.entries, state.unitSystem, { state.unitSystem = it },
            Modifier.padding(top = 8.dp, bottom = 8.dp),
        ) { u, _ ->
            T(
                if (u == UnitSystem.METRIC) "Metric (cm/kg)" else "Imperial (ft/lb)",
                weight = if (state.unitSystem == u) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            )
        }

        FieldLabel("Height")
        if (state.unitSystem == UnitSystem.METRIC) {
            WheelPicker(100..250, state.heightCm.toInt(), { state.heightCm = it.toFloat() }, Modifier.fillMaxWidth()) { "$it cm" }
        } else {
            val (ft, inch) = cmToFtIn(state.heightCm)
            Row {
                WheelPicker(3..8, ft, { newFt -> state.heightCm = clampHeightCm(ftInToCm(newFt, inch)) }, Modifier.weight(1f)) { "$it ft" }
                WheelPicker(
                    0..11, inch.toInt().coerceIn(0, 11),
                    { newIn -> state.heightCm = clampHeightCm(ftInToCm(ft, newIn.toFloat())) }, Modifier.weight(1f),
                ) { "$it in" }
            }
        }

        FieldLabel("Weight")
        if (state.unitSystem == UnitSystem.METRIC) {
            WheelPicker(30..300, state.weightKg.toInt(), { state.weightKg = it.toFloat() }, Modifier.fillMaxWidth()) { "$it kg" }
        } else {
            WheelPicker(
                66..660, kgToLb(state.weightKg).toInt(),
                { lb -> state.weightKg = clampWeightKg(lbToKg(lb.toFloat())) }, Modifier.fillMaxWidth(),
            ) { "$it lb" }
        }
    }
}

@Composable
private fun ActivityLevelStep(state: OnboardingState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_ACTIVITY_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        TilePicker(
            ActivityLevel.entries, state.activityLevel ?: ActivityLevel.entries.first(), { state.activityLevel = it },
            Modifier.padding(top = 8.dp),
        ) { lvl, _ ->
            T(
                Copy.ACTIVITY_COPY.getValue(lvl.name),
                weight = if (state.activityLevel == lvl) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun RoutineStep(state: OnboardingState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_ROUTINE_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        FieldLabel("Wake up")
        TimeWheelPicker(state.wakeTime, { state.wakeTime = it })
        FieldLabel("Sleep")
        TimeWheelPicker(state.sleepTime, { state.sleepTime = it })
        FieldLabel("Remind me at")
        TimeWheelPicker(state.reminderTime, { state.reminderTime = it })
        if (state.wakeTime == state.sleepTime) {
            ErrText("Wake and sleep times should differ.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

val FOCUS_ICONS: Map<Focus, DrawableResource> = mapOf(
    Focus.GYM to Res.drawable.ic_focus_gym,
    Focus.MOVIE to Res.drawable.ic_focus_movie,
    Focus.BOOK to Res.drawable.ic_focus_book,
    Focus.FINANCE to Res.drawable.ic_focus_finance,
    Focus.GROCERIES to Res.drawable.ic_focus_groceries,
    Focus.RECIPES to Res.drawable.ic_focus_recipes,
)

@Composable
private fun FocusStep(state: OnboardingState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_FOCUS_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        TilePicker(
            Focus.entries, state.focus ?: Focus.entries.first(),
            { f ->
                state.focus = f
                state.selectedHabitNames = STARTER_HABITS.getValue(f).toSet()
            },
            Modifier.padding(top = 8.dp),
        ) { f, _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FOCUS_ICONS[f]?.let { Image(painterResource(it), contentDescription = f.label, modifier = Modifier.size(32.dp)) }
                T(
                    f.label,
                    weight = if (state.focus == f) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun StarterHabitsStep(state: OnboardingState) {
    val focus = state.focus ?: Focus.GYM
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_STARTER_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        STARTER_HABITS.getValue(focus).forEach { name ->
            val on = state.selectedHabitNames.contains(name)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    .clickable { state.selectedHabitNames = toggleStarterHabit(state.selectedHabitNames, name) },
                verticalAlignment = Alignment.CenterVertically,
            ) { Check(on); Spacer(Modifier.width(8.dp)); T(name) }
        }
    }
}

@Composable
private fun AvatarThemeStep(state: OnboardingState) {
    val c = LocalColors.current
    val selectedAvatar = AVATARS.firstOrNull { it.id == state.avatarId } ?: AVATARS.first()
    val selectedAccent = state.accentHex ?: PAL.first().first
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_AVATAR_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        FieldLabel("Avatar")
        TilePicker(AVATARS, selectedAvatar, { state.avatarId = it.id }) { a, _ ->
            Image(painterResource(a.drawable), contentDescription = a.id, modifier = Modifier.size(40.dp))
        }
        FieldLabel("Accent color")
        TilePicker(PAL, PAL.first { it.first == selectedAccent }, { state.accentHex = it.first }) { (hexStr, name), _ ->
            Box(
                Modifier.size(32.dp).background(hex(hexStr), CircleShape).border(2.dp, c.ink, CircleShape)
                    .semantics { contentDescription = name },
            )
        }
        FieldLabel("Preview")
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(hex(selectedAccent), CircleShape).border(3.dp, c.ink, CircleShape))
                Spacer(Modifier.width(12.dp))
                T(state.name.ifEmpty { "Your habit" }, weight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotificationsStep(state: OnboardingState) {
    val requestPermission = rememberNotificationPermissionRequester()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_NOTIFY_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        T(
            Copy.reminderExplainer(state.reminderTime.toString().take(5)),
            align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        DButton(if (state.notificationsRequested) "Done ✓" else "Allow reminders", enabled = !state.notificationsRequested) {
            requestPermission { granted ->
                state.notificationsGranted = granted
                state.notificationsRequested = true
            }
        }
        if (state.notificationsRequested && !state.notificationsGranted) {
            Meta(Copy.OB_NOTIFY_DENIED, Modifier.fillMaxWidth().padding(top = 8.dp), align = TextAlign.Center)
        }
    }
}

@Composable
private fun DoneStep(state: OnboardingState) {
    val focus = state.focus ?: Focus.GYM
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(
            Copy.OB_DONE_TITLE.replace("{name}", state.name.ifEmpty { "friend" }),
            align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        T("Focus: ${focus.label}", align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        T("${state.selectedHabitNames.size} habits ready to go.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GenderStep(state: OnboardingState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        H2(Copy.OB_GENDER_TITLE, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        TilePicker(
            Gender.entries, state.gender ?: Gender.entries.first(), { state.gender = it },
            Modifier.padding(top = 8.dp),
        ) { g, _ ->
            T(
                when (g) {
                    Gender.MALE -> "Male"; Gender.FEMALE -> "Female"
                    Gender.NON_BINARY -> "Non-binary"; Gender.PREFER_NOT_TO_SAY -> "Prefer not to say"
                },
                weight = if (state.gender == g) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            )
        }
    }
}

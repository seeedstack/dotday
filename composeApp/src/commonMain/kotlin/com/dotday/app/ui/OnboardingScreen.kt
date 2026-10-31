package com.dotday.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.domain.ActivityLevel
import com.dotday.app.domain.Copy
import com.dotday.app.domain.Gender
import com.dotday.app.domain.StepValidationArgs
import com.dotday.app.domain.isStepValid
import com.dotday.app.domain.maxBirthYear
import com.dotday.app.domain.minBirthYear
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
        notificationRequested = state.notificationsGranted,
    )
    val valid = isStepValid(state.step, args)

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        ProgressDots(state.step, ONBOARDING_STEP_COUNT, Modifier.padding(bottom = 12.dp))
        Panel {
            AnimatedContent(
                state.step,
                transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
            ) { step ->
                when (step) {
                    0 -> WelcomeStep()
                    1 -> NameStep(state)
                    2 -> BirthYearStep(state, currentYear)
                    3 -> GenderStep(state)
                    else -> {}
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (state.step > 0) DButton("Back", kind = BtnKind.Alt) { state.back() } else Spacer(Modifier.width(1.dp))
            when (state.step) {
                0 -> DButton(Copy.OB_WELCOME_CTA) { state.next() }
                else -> DButton("NEXT!", enabled = valid) { state.next() }
            }
        }
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
    Column {
        H2(Copy.OB_NAME_TITLE)
        T(Copy.OB_NAME_SUB, modifier = Modifier.padding(bottom = 8.dp))
        DField(state.name, { state.name = it }, maxLen = 24)
        if (state.name.isNotEmpty()) ErrText(if (state.name.trim().length in 1..24) "" else "1–24 characters, please.")
    }
}

@Composable
private fun BirthYearStep(state: OnboardingState, currentYear: Int) {
    Column {
        H2(Copy.OB_BIRTHYEAR_TITLE)
        WheelPicker(
            minBirthYear(currentYear)..maxBirthYear(currentYear), state.birthYear,
            { state.birthYear = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Meta("Age: ${currentYear - state.birthYear}", Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun GenderStep(state: OnboardingState) {
    Column {
        H2(Copy.OB_GENDER_TITLE)
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

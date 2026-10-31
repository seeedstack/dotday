package com.dotday.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dotday.app.data.AppStore
import com.dotday.app.domain.*
import com.dotday.app.platform.scheduleDailyReminder
import com.dotday.app.sdk.AddonTheme
import com.dotday.app.sdk.activeTheme
import com.dotday.app.sdk.passesContrast
import com.dotday.app.ui.*
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.math.max

private fun currentDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

private val HeaderTitleFontSize = 44.sp
// Anton: capHeight / unitsPerEm, from anton_regular.ttf (capHeight=1760, unitsPerEm=2048)
private const val HeaderTitleCapHeightRatio = 0.859375f

private fun AddonTheme.toDotColors() =
    DotColors(hex(paper), hex(card), hex(ink), hex(muted), hex(primary), hex(yellow), hex(blue), hex(ok))

@Composable
fun App() {
    val store = remember { AppStore(Settings()) }
    val ui = remember { UiState() }
    var today by remember { mutableStateOf(currentDate()) }
    // refresh at local midnight
    LaunchedEffect(Unit) { while (true) { delay(30_000); val n = currentDate(); if (n != today) today = n } }
    // re-arm the daily reminder on every app start: Android alarms don't survive a reboot/force-stop,
    // and the Desktop scheduler only lives for the process that started it.
    LaunchedEffect(Unit) {
        store.profile?.takeIf { it.notificationsGranted }?.let { scheduleDailyReminder(it.reminderTime.hour, it.reminderTime.minute) }
    }

    val dark = when (store.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val addonTheme = store.addons.activeTheme()
    val colors = if (store.theme == "addon" && addonTheme != null && addonTheme.passesContrast()) {
        addonTheme.toDotColors()
    } else if (dark) DarkColors else LightColors
    val fonts = rememberDotFonts()
    val scroll = rememberScrollState()
    LaunchedEffect(ui.tab) { scroll.scrollTo(0) }
    LaunchedEffect(ui.toastId) { if (ui.toastMsg != null) { delay(2600); ui.clearToast() } }

    CompositionLocalProvider(LocalColors provides colors, LocalFonts provides fonts, LocalCalm provides store.calm) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(colors.paper)
                .then(
                    if (store.calm) Modifier else Modifier.halftone(
                        listOf(
                            HDot(Color(0xFFE63946).copy(alpha = .14f), 0f, 0f, 1.75f),
                            HDot(Color(0xFF1D4ED8).copy(alpha = .12f), .5f, .5f, 1.75f),
                        ), 9f,
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
        ) {
            val compact = maxWidth <= 600.dp
            CompositionLocalProvider(LocalCompact provides compact, LocalVw provides maxWidth) {
                if (store.profile == null) {
                    Column(Modifier.fillMaxSize().verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(16.dp)) {
                            OnboardingScreen(store, ui)
                        }
                    }
                    return@CompositionLocalProvider
                }
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(16.dp)) {
                            Header(store, today)
                            if (!compact) NavBar(ui, Modifier.padding(bottom = 18.dp))
                            when (ui.tab) {
                                Tab.Today -> TodayScreen(store, ui, today)
                                Tab.Habits -> HabitsScreen(store, ui, today)
                                Tab.Insights -> InsightsScreen(store, ui, today)
                                Tab.Settings -> SettingsScreen(store, ui, today)
                            }
                            T(
                                "Single-user, local-first. Data stays on this device.", 13.sp, color = colors.muted,
                                align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(12.dp),
                            )
                        }
                        if (!compact) Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                    if (compact) {
                        NavBar(
                            ui,
                            Modifier.fillMaxWidth().background(colors.paper)
                                .drawBehind { drawRect(colors.ink, size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())) }
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(start = 8.dp, end = 8.dp, top = 8.dp + 3.dp, bottom = 8.dp),
                        )
                    }
                }

                ui.toastMsg?.let { m ->
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars), contentAlignment = Alignment.BottomCenter) {
                        Row(
                            Modifier.padding(bottom = 80.dp).background(colors.ink, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            T(m, color = colors.paper)
                            ui.toastAction?.let { a ->
                                T(
                                    a.label, color = colors.yellow, weight = FontWeight.Bold,
                                    modifier = Modifier.clickable(role = Role.Button) { a.onClick(); ui.clearToast() },
                                )
                            }
                        }
                    }
                }
                ui.burst?.let { b -> BurstOverlay(b) { if (ui.burst?.id == b.id) ui.burst = null } }
                ui.confirm?.let { cf ->
                    Dialog(onDismissRequest = { ui.confirm = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                        Column(
                            Modifier.padding(16.dp).widthIn(max = 420.dp)
                                .comic(colors.card, colors.ink, RoundedCornerShape(8.dp), 3.dp)
                                .padding(16.dp)
                        ) {
                            T(cf.msg)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                                DButton("Cancel", kind = BtnKind.Alt) { ui.confirm = null }
                                DButton(cf.yes, kind = BtnKind.Bad) { ui.confirm = null; cf.onYes() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(store: AppStore, today: LocalDate) {
    val c = LocalColors.current
    val f = LocalFonts.current
    val hr = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    val greet = (if (hr < 12) "Good morning" else if (hr < 18) "Good afternoon" else "Good evening") +
        " · ${DAY_FULL[today.dow()]}, ${MONTHS[today.monthNumber - 1]} ${today.dayOfMonth}"
    val best = max(0, store.habits.maxOfOrNull { currentStreak(it, store.done(it.id), today) } ?: 0)
    val chipHeight = with(LocalDensity.current) { (HeaderTitleFontSize.value * HeaderTitleCapHeightRatio).sp.toDp() }
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            androidx.compose.foundation.text.BasicText(
                "DOTDAY",
                modifier = Modifier.alignBy(FirstBaseline),
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = f.disp, fontSize = HeaderTitleFontSize, lineHeight = 48.sp, letterSpacing = 0.03.em, color = c.primary,
                ),
            )
            StreakChip(
                "🔥 $best ${Copy.STREAK}".trim(), chipHeight,
                Modifier.alignBy { it.measuredHeight },
            )
        }
        T(greet, 14.sp, color = c.muted)
    }
}

@Composable
private fun NavBar(ui: UiState, modifier: Modifier) {
    val c = LocalColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tab.entries.forEach { t ->
            val cur = ui.tab == t
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp)
                    .comic(if (cur) c.ink else c.card, c.ink, BtnShape, 3.dp)
                    .clickable(role = Role.Tab) { ui.go(t) },
                contentAlignment = Alignment.Center,
            ) { T(t.label, 15.sp, FontWeight.Bold, if (cur) c.paper else c.ink) }
        }
    }
}

@Composable
private fun BurstOverlay(b: Burst, onDone: () -> Unit) {
    val c = LocalColors.current
    val p = remember(b.id) { Animatable(0f) }
    LaunchedEffect(b.id) { p.animateTo(1f, tween(800, easing = LinearOutSlowInEasing)); onDone() }
    Box(Modifier.fillMaxSize(), contentAlignment = BiasAlignment(0f, -0.2f)) {
        Box(
            Modifier.graphicsLayer {
                val t = p.value
                val s = if (t < .3f) .3f + .9f * (t / .3f) else 1.2f - .2f * ((t - .3f) / .7f)
                scaleX = s; scaleY = s
                alpha = if (t < .3f) t / .3f else 1f - (t - .3f) / .7f
                translationY = -size.height * 0.3f * max(0f, (t - .3f) / .7f)
                rotationZ = -8f
            }
        ) {
            T(b.text, 56.sp, color = c.primary, disp = true)
        }
    }
}

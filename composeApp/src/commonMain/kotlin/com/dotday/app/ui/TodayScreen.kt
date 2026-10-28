package com.dotday.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.domain.*
import com.dotday.app.sdk.activeDashboardCards
import kotlinx.datetime.LocalDate

@Composable
fun EmptyPanel(store: AppStore, ui: UiState, today: LocalDate) {
    val c = LocalColors.current
    Panel {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            H2("No habits yet!", 34.sp, c.primary)
            T("Add your first habit to start filling in the page.", modifier = Modifier.padding(vertical = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DButton("Create a habit") { ui.go(Tab.Habits, "new") }
            }
        }
    }
}

@Composable
fun TodayScreen(store: AppStore, ui: UiState, today: LocalDate) {
    val hs = store.habits.filter { it.archived == null }
    if (hs.isEmpty()) { EmptyPanel(store, ui, today); return }
    val date = ui.selectedDate ?: today
    val due = hs.filter { isDue(it, date) }

    Column {
        DatePicker(today, date) { d ->
            ui.selectedDate = if (d == today) null else d
            ui.stampNew = null
        }

        H2(if (date == today) "Today's tasks" else dateLabel(date), modifier = Modifier.padding(bottom = 12.dp))
        if (due.isEmpty()) {
            Panel { T(if (date == today) "No habits are due today." else "No habits were due on ${dateLabel(date)}.") }
        }
        due.forEach { h ->
            HabitCard(
                h, date, store.done(h.id),
                stampAnimate = ui.stampNew == h.id && !store.calm,
            ) {
                val on = store.toggle(h.id, date)
                if (on) {
                    val all = hs.filter { isDue(it, date) }.all { store.done(it.id).contains(date) }
                    val st = currentStreak(h, store.done(h.id), date)
                    ui.stampNew = h.id
                    if (!store.calm) ui.fireBurst(
                        if (all) Copy.ALL else if (st > 0 && st % 7 == 0) Copy.MILESTONE else Copy.BURSTS.random()
                    )
                } else ui.stampNew = null
            }
        }
        DButton("+ Add habit") { ui.go(Tab.Habits, "new") }
        Spacer(Modifier.height(22.dp))
        WeekPanel(store, ui, today, hs)
        store.addons.activeDashboardCards().forEach { card ->
            Spacer(Modifier.height(16.dp))
            Panel { H2(card.title, 20.sp); T(card.text, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

private fun dateLabel(d: LocalDate) = "${DN[d.dow()]}, ${MONTHS[d.monthNumber - 1].take(3)} ${d.dayOfMonth}"

/* ---------------- Date picker ---------------- */
private const val RECENT_DAYS = 30
private val DateChipShape = RoundedCornerShape(ChipCornerRadius)

@Composable
private fun DatePicker(today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val dates = remember(today) { recentDates(today, RECENT_DAYS) }
    val listState = rememberLazyListState()
    val selectedIndex = dates.indexOf(selected).coerceAtLeast(0)
    LaunchedEffect(selectedIndex) { listState.animateScrollToItem(selectedIndex) }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        items(dates) { d -> DateChip(d, isSelected = d == selected, isToday = d == today) { onSelect(d) } }
    }
}

@Composable
private fun DateChip(d: LocalDate, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val c = LocalColors.current
    Box(
        Modifier.size(56.dp)
            .comic(if (isSelected) c.yellow else c.card, c.ink, DateChipShape, if (isSelected) 3.dp else 2.dp)
            .selectable(selected = isSelected, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${DAY_FULL[d.dow()]}, ${MONTHS[d.monthNumber - 1]} ${d.dayOfMonth}" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(DN[d.dow()].uppercase(), 11.sp, FontWeight.Bold, if (isSelected) Ink111 else c.ink)
            T(d.dayOfMonth.toString().padStart(2, '0'), 17.sp, FontWeight.Bold, if (isSelected) Ink111 else c.ink)
            if (isToday && !isSelected) Box(Modifier.padding(top = 2.dp).size(4.dp).background(c.primary, CircleShape))
        }
    }
}

/* ---------------- Habit card ---------------- */
@Composable
private fun HabitCard(h: Habit, today: LocalDate, done: Set<LocalDate>, stampAnimate: Boolean, onToggle: () -> Unit) {
    val c = LocalColors.current
    val hc = hex(h.color)
    val isDone = done.contains(today)
    val st = currentStreak(h, done, today)
    val flags = remember(done, today) { List(30) { i -> done.contains(today.addDays(i - 29)) } }
    val count = done.count { it > today.addDays(-30) }

    Panel(overlay = if (isDone) Modifier.halftone(listOf(HDot(hc, .5f, .5f, 3.3f)), 11f) else Modifier) {
        Box(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(hc, CircleShape).border(3.dp, c.ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { T(CATS[h.category] ?: "●", 22.sp, color = Ink111, lineHeight = 24.sp) }
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f)
                        .background(c.card, RoundedCornerShape(8.dp)).border(2.dp, c.ink, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    T(h.name, 18.sp, FontWeight.Bold, lineHeight = 24.sp)
                    Meta("${h.category} · ${sched(h)} · $st ${Copy.STREAK}")
                    DotStrip(
                        flags, hc,
                        Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp)
                            .semantics { contentDescription = "Last 30 days: $count done" },
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.size(56.dp)
                        .comic(if (isDone) c.ink else c.card, c.ink, CircleShape, 3.dp)
                        .clickable(role = Role.Checkbox, onClick = onToggle)
                        .semantics { contentDescription = "${h.name}: ${if (isDone) "done, tap to undo" else "mark done"}" },
                    contentAlignment = Alignment.Center,
                ) { if (isDone) T("✓", 26.sp, FontWeight.Bold, c.paper, lineHeight = 28.sp) }
            }
            if (isDone) Stamp(stampAnimate, Modifier.align(Alignment.TopEnd).offset(x = (-68).dp, y = (-8).dp))
        }
    }
}

@Composable
private fun Stamp(animate: Boolean, modifier: Modifier) {
    val c = LocalColors.current
    val a = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { if (animate) a.animateTo(1f, tween(250)) }
    Box(
        modifier
            .graphicsLayer { rotationZ = -12f; val s = 1.5f - 0.5f * a.value; scaleX = s; scaleY = s; alpha = a.value }
            .background(c.card).border(3.dp, c.primary).padding(horizontal = 8.dp)
    ) { T(Copy.STAMP, 20.sp, color = c.primary, disp = true) }
}

/* ---------------- Weekly progress ---------------- */
private enum class CellKind { Done, Miss, Up, Na }

@Composable
private fun WeekPanel(store: AppStore, ui: UiState, today: LocalDate, hs: List<Habit>) {
    val c = LocalColors.current
    val wk = weekOf(today.addDays(ui.weekOff * 7), store.weekStart)
    Panel {
        H2("Weekly progress", 22.sp, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(
            Modifier.padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DButton("← Previous", kind = BtnKind.Alt) { ui.weekOff -= 1 }
            DButton("This week", kind = BtnKind.Alt) { ui.weekOff = 0 }
            DButton("Next →", kind = BtnKind.Alt) { ui.weekOff += 1 }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val nameW = 120.dp
            val colW = maxOf(62.dp, (maxWidth - nameW) / 7)
            Row {
                Column(Modifier.width(nameW).background(c.card)) {
                    Spacer(Modifier.height(48.dp))
                    hs.forEach { h ->
                        Box(Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            T(h.name, 13.sp, FontWeight.Bold, maxLines = 2, lineHeight = 17.sp)
                        }
                    }
                }
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    wk.forEach { d ->
                        Column(Modifier.width(colW)) {
                            val isToday = d == today
                            Box(
                                Modifier.fillMaxWidth().height(48.dp)
                                    .then(if (isToday) Modifier.background(c.yellow).border(2.dp, c.ink) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                T(
                                    DN[d.dow()] + "\n" + d.dayOfMonth.toString().padStart(2, '0'), 13.sp,
                                    FontWeight.Bold, if (isToday) Ink111 else c.ink,
                                    align = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                            hs.forEach { h ->
                                val s = store.done(h.id).contains(d)
                                val dueD = isDue(h, d)
                                val (kind, label, word) = when {
                                    !dueD -> Triple(CellKind.Na, "–", "Not scheduled")
                                    d > today -> Triple(CellKind.Up, "", "Upcoming")
                                    s -> Triple(CellKind.Done, "✓", "Done")
                                    else -> Triple(CellKind.Miss, "✕", "Missed")
                                }
                                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                                    CircleCell(
                                        kind, label, "${h.name}, $d: $word",
                                        if (kind == CellKind.Up || kind == CellKind.Na) null
                                        else ({ store.toggle(h.id, d); Unit }),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleCell(kind: CellKind, label: String, desc: String, onClick: (() -> Unit)?) {
    val c = LocalColors.current
    val disabled = kind == CellKind.Up || kind == CellKind.Na
    Box(
        Modifier.size(40.dp).alpha(if (disabled) 0.4f else 1f)
            .drawBehind {
                val w = 3.dp.toPx()
                drawCircle(if (kind == CellKind.Done) c.primary else c.card)
                val effect = when (kind) {
                    CellKind.Miss -> PathEffect.dashPathEffect(floatArrayOf(w * 2.5f, w * 1.5f))
                    CellKind.Up, CellKind.Na -> PathEffect.dashPathEffect(floatArrayOf(0.01f, w * 2f))
                    else -> null
                }
                drawCircle(
                    c.ink, size.width / 2 - w / 2,
                    style = Stroke(w, cap = if (disabled) StrokeCap.Round else StrokeCap.Butt, pathEffect = effect),
                )
            }
            .then(if (onClick != null) Modifier.clickable(role = Role.Checkbox, onClick = onClick) else Modifier)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        T(
            label, 16.sp, FontWeight.Bold,
            when (kind) { CellKind.Done -> Color.White; CellKind.Miss -> c.muted; else -> c.ink },
            lineHeight = 18.sp,
        )
    }
}

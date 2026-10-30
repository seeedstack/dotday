package com.dotday.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.domain.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

@Composable
fun HabitsScreen(store: AppStore, ui: UiState, today: LocalDate) {
    Column {
        H2("Habits", modifier = Modifier.padding(bottom = 12.dp))
        if (ui.editing != null) HabitForm(store, ui, today)
        else {
            DButton("+ New habit") { ui.editing = "new" }
            Spacer(Modifier.height(16.dp))
        }
        if (store.habits.isEmpty() && ui.editing == null) EmptyPanel(store, ui, today)
        store.habits.forEach { x -> HabitRow(store, ui, x, today) }
    }
}

/** Where a dragged habit-row offset (<= 0, px) should snap to: open (-maxOffsetPx) past halfway, else closed (0). */
internal fun swipeOpenTarget(offsetPx: Float, maxOffsetPx: Float): Float =
    if (offsetPx <= -maxOffsetPx / 2f) -maxOffsetPx else 0f

@Composable
private fun HabitRow(store: AppStore, ui: UiState, x: Habit, today: LocalDate) {
    Panel(Modifier.alpha(if (x.archived != null) .7f else 1f)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.padding(end = 10.dp)) {
                T(x.name + if (x.archived != null) " (archived)" else "", 18.sp, FontWeight.Bold, lineHeight = 24.sp)
                Meta("${x.category} · ${sched(x)}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DButton("Edit", kind = BtnKind.Alt) { ui.editing = x.id }
                DButton(if (x.archived != null) "Restore" else "Archive", kind = BtnKind.Alt) {
                    val a = store.toggleArchive(x.id, today)
                    ui.toast(if (a) "Archived. History is kept." else "Restored.")
                }
                DButton("Delete", kind = BtnKind.Bad) {
                    ui.confirm = Confirm(
                        "Delete \"${x.name}\" and all its check-ins? This can't be undone. Archiving keeps history and is usually better.",
                        "Delete",
                    ) { store.delete(x.id); ui.toast("Habit deleted.") }
                }
            }
        }
    }
}

@Composable
private fun HabitForm(store: AppStore, ui: UiState, today: LocalDate) {
    val c = LocalColors.current
    val key = ui.editing
    val h = key?.takeIf { it != "new" }?.let { id -> store.habits.find { it.id == id } }
    var name by remember(key) { mutableStateOf(h?.name ?: "") }
    var cat by remember(key) { mutableStateOf(h?.category ?: "Health") }
    var color by remember(key) { mutableStateOf(h?.color ?: PAL[0].first) }
    var days by remember(key) { mutableStateOf(h?.days?.toSet() ?: (0..6).toSet()) }
    var start by remember(key) { mutableStateOf((h?.start ?: today).toString()) }
    var eN by remember(key) { mutableStateOf("") }
    var eD by remember(key) { mutableStateOf("") }
    var eS by remember(key) { mutableStateOf("") }

    Panel {
        H2(if (h != null) "Edit habit" else "New habit")
        FieldLabel("Name (1–40 characters)")
        DField(name, { name = it })
        ErrText(eN)

        FieldLabel("Category")
        DField(cat, { cat = it })
        FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CATS.keys.forEach { k ->
                Box(
                    Modifier.border(2.dp, c.ink, CircleShape).clickable { cat = k }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { T(k, 13.sp, if (cat == k) FontWeight.Bold else FontWeight.Normal) }
            }
        }

        Spacer(Modifier.height(10.dp))
        T("Color", weight = FontWeight.SemiBold)
        FlowRow(Modifier.padding(top = 10.dp, bottom = 6.dp, start = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PAL.forEach { (hexStr, nm) ->
                val sel = color == hexStr
                Box(
                    Modifier.size(44.dp)
                        .drawBehind {
                            if (sel) {
                                val r = size.width / 2
                                drawCircle(c.paper, r + 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
                                drawCircle(c.ink, r + 4.5.dp.toPx(), style = Stroke(3.dp.toPx()))
                            }
                        }
                        .background(hex(hexStr), CircleShape).border(3.dp, c.ink, CircleShape)
                        .clickable(role = Role.RadioButton) { color = hexStr }
                        .semantics { contentDescription = nm }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        T("Days", weight = FontWeight.SemiBold)
        FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2, 3, 4, 5, 6, 0).forEach { d ->
                val on = days.contains(d)
                Row(
                    Modifier.heightIn(min = 44.dp).border(2.dp, c.ink, CircleShape)
                        .clickable(role = Role.Checkbox) { days = if (on) days - d else days + d }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Check(on); Spacer(Modifier.width(4.dp)); T(DN[d]) }
            }
        }
        ErrText(eD)

        FieldLabel("Start date (YYYY-MM-DD)")
        DField(start, { start = it }, maxLen = 10)
        ErrText(eS)

        if (h != null) Meta(
            "Changing days affects past statistics in this prototype (schedule revisions come in the full build).",
            Modifier.padding(vertical = 8.dp),
        )

        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DButton("Save habit") {
                val n = name.trim()
                val sd = runCatching { LocalDate.parse(start.trim()) }.getOrNull()
                eN = when { n.isEmpty() -> "Enter a name."; n.length > 40 -> "Name must be 40 characters or fewer."; else -> "" }
                eD = if (days.isEmpty()) "Pick at least one day." else ""
                eS = if (sd == null) "Choose a start date (YYYY-MM-DD)." else ""
                if (eN.isEmpty() && eD.isEmpty() && sd != null) {
                    val sortedDays = days.toList().sorted()
                    store.upsert(
                        h?.copy(name = n, category = cat.trim().ifEmpty { "Personal" }, color = color, days = sortedDays, start = sd)
                            ?: Habit(
                                "h" + Clock.System.now().toEpochMilliseconds().toString(36), n,
                                cat.trim().ifEmpty { "Personal" }, color, sortedDays, sd,
                            )
                    )
                    ui.toast("Habit saved."); ui.editing = null
                }
            }
            DButton("Cancel", kind = BtnKind.Alt) { ui.editing = null }
        }
    }
}

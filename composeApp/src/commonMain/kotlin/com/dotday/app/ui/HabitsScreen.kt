package com.dotday.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

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
private fun HabitCardContent(x: Habit, onEdit: () -> Unit) {
    Panel(Modifier.alpha(if (x.archived != null) .7f else 1f), gap = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                T(x.name + if (x.archived != null) " (archived)" else "", 18.sp, FontWeight.Bold, lineHeight = 24.sp)
                Meta("${x.category} · ${sched(x)}")
            }
            Box(
                Modifier.size(44.dp).clickable(role = Role.Button, onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) { T("✎", 20.sp) }
        }
    }
}

@Composable
private fun SwipeActionButton(emoji: String, label: String, bg: androidx.compose.ui.graphics.Color, width: Dp, onClick: () -> Unit) {
    val c = LocalColors.current
    Box(
        Modifier.fillMaxHeight().width(width).comic(bg, c.ink, RoundedCornerShape(0.dp), 3.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(emoji, 20.sp)
            T(label, 12.sp, FontWeight.Bold, androidx.compose.ui.graphics.Color.White)
        }
    }
}

@Composable
private fun HabitRow(store: AppStore, ui: UiState, x: Habit, today: LocalDate) {
    val density = LocalDensity.current
    val c = LocalColors.current
    val maxOffsetPx = with(density) { 160.dp.toPx() }
    val offsetX = remember(x.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var removed by remember(x.id) { mutableStateOf(false) }

    LaunchedEffect(ui.swipedHabitId) {
        if (ui.swipedHabitId != x.id && offsetX.value != 0f) offsetX.animateTo(0f, spring())
    }
    LaunchedEffect(removed) {
        if (removed) {
            delay(220)
            val log = store.done(x.id)
            store.delete(x.id)
            ui.toast("Habit deleted.", SnackAction("Undo") { store.restore(x, log) })
        }
    }

    AnimatedVisibility(
        visible = !removed,
        exit = shrinkVertically(tween(220)) + fadeOut(tween(220)),
    ) {
        Box(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
            Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
                SwipeActionButton("📦", if (x.archived != null) "Restore" else "Archive", c.blue, 80.dp) {
                    scope.launch { offsetX.animateTo(0f, spring()) }
                    ui.swipedHabitId = null
                    val nowArchived = store.toggleArchive(x.id, today)
                    ui.toast(
                        if (nowArchived) "Archived. History is kept." else "Restored.",
                        SnackAction("Undo") { store.toggleArchive(x.id, today) },
                    )
                }
                SwipeActionButton("🗑", "Delete", c.primary, 80.dp) {
                    scope.launch { offsetX.animateTo(0f, spring()) }
                    ui.swipedHabitId = null
                    ui.confirm = Confirm(
                        "Delete \"${x.name}\" and all its check-ins? This can't be undone. Archiving keeps history and is usually better.",
                        "Delete",
                    ) { removed = true }
                }
            }
            Box(
                Modifier.fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-maxOffsetPx, 0f)) }
                        },
                        onDragStopped = {
                            val target = swipeOpenTarget(offsetX.value, maxOffsetPx)
                            scope.launch { offsetX.animateTo(target, spring()) }
                            ui.swipedHabitId = if (target != 0f) x.id else null
                        },
                    )
                    .clickable(
                        enabled = ui.swipedHabitId != null && ui.swipedHabitId != x.id,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { ui.swipedHabitId = null },
            ) { HabitCardContent(x) { ui.editing = x.id } }
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

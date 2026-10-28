package com.dotday.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.domain.*
import kotlinx.datetime.LocalDate
import kotlin.math.floor
import kotlin.math.roundToInt

private fun fmtPct(r: Rate) = r.pct?.let { "$it%" } ?: "—"

@Composable
fun InsightsScreen(store: AppStore, ui: UiState, today: LocalDate) {
    val c = LocalColors.current
    val hs = store.habits.filter { it.archived == null }
    if (hs.isEmpty()) { EmptyPanel(store, ui, today); return }
    val logs = store.logs
    fun r(n: Int) = rate(hs, logs, today.addDays(-n + 1), today)
    val total = store.habits.sumOf { store.done(it.id).size }
    val longest = hs.maxOf { longestStreak(it, store.done(it.id), today) }
    val bestNow = hs.maxOf { currentStreak(it, store.done(it.id), today) }
    val days = List(14) { i -> today.addDays(i - 13).let { d -> d to rate(hs, logs, d, d) } }
    val rank = hs.map { h ->
        Ranked(h, rate(listOf(h), logs, today.addDays(-29), today),
            currentStreak(h, store.done(h.id), today), longestStreak(h, store.done(h.id), today))
    }.sortedByDescending { it.p.pct ?: -1 }

    Column {
        H2("Insights", modifier = Modifier.padding(bottom = 12.dp))

        val stats = listOf(
            "Today" to fmtPct(r(1)), "Last 7 days" to fmtPct(r(7)), "Last 30 days" to fmtPct(r(30)),
            "Best current streak" to bestNow.toString(), "Longest streak" to longest.toString(), "Total check-ins" to total.toString(),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cols = maxOf(1, floor((maxWidth + 12.dp) / (130.dp + 12.dp)).toInt())
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.chunked(cols).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (a, b) ->
                            Panel(Modifier.weight(1f), gap = 10.dp) {
                                Meta(a)
                                T(b, 34.sp, disp = true, lineHeight = 42.sp)
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Panel {
            H2("Daily completion, last 14 days", 20.sp)
            Spacer(Modifier.height(8.dp))
            BarChart(days)
        }

        Panel {
            H2("Most consistent, 30 days", 20.sp)
            rank.forEach { (h, p, cur, best) ->
                FlowRow(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    T(h.name, weight = FontWeight.Bold, modifier = Modifier.widthIn(min = 130.dp))
                    Chip(fmtPct(p))
                    Meta("now $cur · best $best")
                    DotStrip(
                        List(28) { i -> store.done(h.id).contains(today.addDays(i - 27)) }, hex(h.color),
                        Modifier.width((28 * 6 + 27 * 2).dp).height(6.dp)
                            .semantics { contentDescription = "Last 28 days history" },
                    )
                }
            }
        }
    }
}

private data class Ranked(val h: Habit, val p: Rate, val cur: Int, val best: Int)

/** Port of the 430x130 SVG bar chart with the yellow/red halftone fill. */
@Composable
private fun BarChart(days: List<Pair<LocalDate, Rate>>) {
    val c = LocalColors.current
    val f = LocalFonts.current
    val measurer = rememberTextMeasurer()
    val desc = "Bar chart. " + days.joinToString(", ") { (d, x) -> "$d ${fmtPct(x)}" }
    Box(
        Modifier.fillMaxWidth().aspectRatio(430f / 130f)
            .semantics { contentDescription = desc }
            .drawWithCache {
                val s = size.width / 430f
                val tilePx = (6 * s).roundToInt().coerceAtLeast(2)
                val bmp = ImageBitmap(tilePx, tilePx)
                val cv = androidx.compose.ui.graphics.Canvas(bmp)
                val paint = Paint().apply { isAntiAlias = true }
                paint.color = c.yellow; cv.drawRect(0f, 0f, tilePx.toFloat(), tilePx.toFloat(), paint)
                paint.color = c.primary; cv.drawCircle(Offset(tilePx / 2f, tilePx / 2f), 1.8f * s, paint)
                val brush = ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
                val label = TextStyle(fontFamily = f.body, fontSize = (9f * s / density).sp, color = c.ink)
                onDrawBehind {
                    days.forEachIndexed { i, (d, x) ->
                        val pct = (x.pct ?: 0).toFloat()
                        val tl = Offset((i * 30 + 6) * s, (110 - pct) * s)
                        val sz = Size(22 * s, pct * s)
                        drawRect(brush, tl, sz)
                        drawRect(c.ink, tl, sz, style = Stroke(2 * s))
                        val txt = d.dayOfMonth.toString().padStart(2, '0')
                        val m = measurer.measure(txt, label)
                        drawText(m, topLeft = Offset((i * 30 + 17) * s - m.size.width / 2f, 124 * s - m.firstBaseline))
                    }
                }
            }
    )
}

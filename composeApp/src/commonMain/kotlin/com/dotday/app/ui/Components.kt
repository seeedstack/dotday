package com.dotday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

val PanelShape = RoundedCornerShape(topStart = 6.dp, topEnd = 14.dp, bottomEnd = 8.dp, bottomStart = 12.dp)
val BtnShape = RoundedCornerShape(topStart = 6.dp, topEnd = 12.dp, bottomEnd = 6.dp, bottomStart = 10.dp)
val ChipCornerRadius = 10.dp

/** Fill + clip + border. */
fun Modifier.comic(
    bg: Color, border: Color, shape: Shape,
    borderWidth: Dp = 3.dp, overlay: Modifier = Modifier,
): Modifier =
    this
        .background(bg, shape)
        .clip(shape)
        .then(overlay)
        .border(borderWidth, border, shape)

class HDot(val color: Color, val fx: Float, val fy: Float, val radiusDp: Float)

/** Repeating halftone dot pattern (CSS radial-gradient tiles). */
fun Modifier.halftone(dots: List<HDot>, spacingDp: Float): Modifier = drawWithCache {
    val tile = (spacingDp * density).roundToInt().coerceAtLeast(2)
    val bmp = ImageBitmap(tile, tile)
    val cv = androidx.compose.ui.graphics.Canvas(bmp)
    val paint = Paint().apply { isAntiAlias = true }
    for (d in dots) {
        paint.color = d.color
        for (ox in 0..1) for (oy in 0..1) {
            cv.drawCircle(Offset((d.fx + ox) * tile, (d.fy + oy) * tile), d.radiusDp * density, paint)
        }
    }
    val brush = ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush) }
}

/* ---------- Text ---------- */
@Composable
fun T(
    text: String,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalColors.current.ink,
    modifier: Modifier = Modifier,
    disp: Boolean = false,
    align: TextAlign = TextAlign.Unspecified,
    spacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val f = LocalFonts.current
    BasicText(
        text, modifier,
        style = TextStyle(
            fontFamily = if (disp) f.disp else f.body,
            fontSize = size,
            fontWeight = weight,
            color = color,
            textAlign = align,
            letterSpacing = spacing,
            lineHeight = if (lineHeight.isSpecified) lineHeight else size * (if (disp) 1.2f else 1.45f),
        ),
        maxLines = maxLines,
    )
}

@Composable
fun H2(text: String, size: TextUnit = 24.sp, color: Color = LocalColors.current.ink, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Unspecified) =
    T(text.uppercase(), size, color = color, modifier = modifier, disp = true, spacing = 0.03.em, align = align)

@Composable
fun Meta(text: String, modifier: Modifier = Modifier) = T(text, 13.sp, color = LocalColors.current.muted, modifier = modifier)

@Composable
fun ErrText(text: String) {
    if (text.isNotEmpty()) T(text, weight = FontWeight.Bold, color = LocalColors.current.primary)
}

/* ---------- Containers ---------- */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    bg: Color? = null,
    gap: Dp = 22.dp,
    overlay: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalColors.current
    Column(
        modifier.fillMaxWidth().padding(bottom = gap)
            .comic(bg ?: c.card, c.ink, PanelShape, 3.dp, overlay)
            .padding(16.dp),
        content = content,
    )
}

enum class BtnKind { Primary, Alt, Bad }

@Composable
fun DButton(text: String, modifier: Modifier = Modifier, kind: BtnKind = BtnKind.Primary, onClick: () -> Unit) {
    val c = LocalColors.current
    val bg = when (kind) { BtnKind.Primary -> c.yellow; BtnKind.Alt -> c.card; BtnKind.Bad -> c.primary }
    val fg = when (kind) { BtnKind.Primary -> Ink111; BtnKind.Alt -> c.ink; BtnKind.Bad -> Color.White }
    Box(
        modifier.heightIn(min = 44.dp)
            .comic(bg, c.ink, BtnShape, 3.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 15.sp, FontWeight.Bold, fg) }
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    Box(
        modifier.background(c.yellow, CircleShape).border(1.dp, c.ink, CircleShape)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) { T(text, 14.sp, FontWeight.Bold, Ink111) }
}

/** Rounded-square streak chip, sized to match the header title's capital-letter height. */
@Composable
fun StreakChip(text: String, height: Dp, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    Box(
        modifier.height(height).widthIn(min = height)
            .comic(c.yellow, c.ink, RoundedCornerShape(ChipCornerRadius), 3.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 20.sp, FontWeight.Bold, Ink111, align = TextAlign.Center) }
}

/* ---------- Form controls ---------- */
@Composable
fun FieldLabel(text: String) =
    T(text, weight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))

@Composable
fun DField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, maxLen: Int = 60) {
    val c = LocalColors.current
    val f = LocalFonts.current
    val shape = RoundedCornerShape(6.dp)
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLen) onChange(it) },
        singleLine = true,
        textStyle = TextStyle(fontFamily = f.body, fontSize = 16.sp, color = c.ink),
        cursorBrush = SolidColor(c.ink),
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp)
            .background(c.card, shape).border(3.dp, c.ink, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        decorationBox = { inner -> Box(Modifier.heightIn(min = 30.dp), contentAlignment = Alignment.CenterStart) { inner() } },
    )
}

@Composable
fun Check(checked: Boolean) {
    val c = LocalColors.current
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier.size(20.dp).background(if (checked) c.ink else c.card, shape).border(2.dp, c.ink, shape),
        contentAlignment = Alignment.Center,
    ) { if (checked) T("✓", 13.sp, FontWeight.Bold, c.paper, lineHeight = 14.sp) }
}

/** Replacement for <select>: tap to expand an inline option list. */
@Composable
fun SelectField(options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit) {
    val c = LocalColors.current
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                .background(c.card, shape).border(3.dp, c.ink, shape)
                .clickable(role = Role.DropdownList) { open = !open }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            T(options.firstOrNull { it.first == value }?.second ?: value, modifier = Modifier.weight(1f))
            T(if (open) "▴" else "▾")
        }
        if (open) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp).background(c.card, shape).border(3.dp, c.ink, shape)) {
                options.forEach { (k, label) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            .clickable { onChange(k); open = false }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        T(label, weight = if (k == value) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        if (k == value) T("✓")
                    }
                }
            }
        }
    }
}

/* ---------- Mini history strip (the little 30-day dots) ---------- */
@Composable
fun DotStrip(flags: List<Boolean>, color: Color, modifier: Modifier = Modifier) {
    val ink = LocalColors.current.ink
    Canvas(modifier) {
        val n = flags.size
        val gap = 2.dp.toPx()
        val d = minOf(6.dp.toPx(), (size.width - gap * (n - 1)) / n)
        flags.forEachIndexed { i, on ->
            val ctr = Offset(i * (d + gap) + d / 2, size.height / 2)
            if (on) drawCircle(color, d / 2, ctr)
            drawCircle(ink, d / 2 - 0.5.dp.toPx(), ctr, style = Stroke(1.dp.toPx()))
        }
    }
}

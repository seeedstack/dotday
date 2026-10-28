package com.dotday.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotday.app.resources.Res
import com.dotday.app.resources.anton_regular
import com.dotday.app.resources.worksans_bold
import com.dotday.app.resources.worksans_regular
import com.dotday.app.resources.worksans_semibold
import org.jetbrains.compose.resources.Font

@Immutable
class DotColors(
    val paper: Color, val card: Color, val ink: Color, val muted: Color,
    val primary: Color, val yellow: Color, val blue: Color, val ok: Color,
)

val LightColors = DotColors(
    paper = Color(0xFFFFF4DF), card = Color(0xFFFFFAF0), ink = Color(0xFF111111), muted = Color(0xFF5A5246),
    primary = Color(0xFFE63946), yellow = Color(0xFFFFD23F), blue = Color(0xFF1D4ED8), ok = Color(0xFF7FD8BE),
)
val DarkColors = DotColors(
    paper = Color(0xFF1B1814), card = Color(0xFF27221B), ink = Color(0xFFF4E9D2), muted = Color(0xFFBDB29C),
    primary = Color(0xFFE63946), yellow = Color(0xFFFFD23F), blue = Color(0xFF6C8CFF), ok = Color(0xFF7FD8BE),
)

/** Literal #111 used where the CSS hard-codes it (mission panel, chips, yellow buttons, bubble). */
val Ink111 = Color(0xFF111111)

class DotFonts(val disp: FontFamily, val body: FontFamily)

val LocalColors = staticCompositionLocalOf { LightColors }
val LocalFonts = staticCompositionLocalOf<DotFonts> { error("fonts not provided") }
val LocalCompact = compositionLocalOf { false }   // viewport <= 600dp
val LocalCalm = compositionLocalOf { false }
val LocalVw = compositionLocalOf { 400.dp }

@Composable
fun rememberDotFonts(): DotFonts {
    val anton = FontFamily(Font(Res.font.anton_regular, FontWeight.Normal))
    val work = FontFamily(
        Font(Res.font.worksans_regular, FontWeight.Normal),
        Font(Res.font.worksans_semibold, FontWeight.SemiBold),
        Font(Res.font.worksans_bold, FontWeight.Bold),
    )
    return DotFonts(anton, work)
}

fun hex(s: String): Color = Color(0xFF000000L or s.removePrefix("#").toLong(16))


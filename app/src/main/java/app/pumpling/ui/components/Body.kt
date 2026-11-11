package app.pumpling.ui.components

import android.content.Context
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import app.pumpling.domain.Muscle
import app.pumpling.domain.tr
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T

/**
 * The body, front and back, as outlined muscle shapes from react-native-body-highlighter
 * (MIT, © 2022 ELABBASSI Hicham; assets/body.txt and body-LICENSE.txt). Each side sits in a 724×1448 box;
 * back paths are drawn shifted left by 724.
 */
object BodyArt {
    class Part(val front: Boolean, val muscle: Muscle?, val slug: String, val path: Path)

    private val MAP = mapOf(
        "chest" to Muscle.CHEST, "deltoids" to Muscle.DELTS, "biceps" to Muscle.BI, "triceps" to Muscle.TRI, "forearm" to Muscle.FORE,
        "abs" to Muscle.ABS, "obliques" to Muscle.ABS, "trapezius" to Muscle.UPPER, "upper-back" to Muscle.LATS, "lower-back" to Muscle.LOWER,
        "gluteal" to Muscle.GLUTES, "quadriceps" to Muscle.QUADS, "adductors" to Muscle.QUADS, "hamstring" to Muscle.HAMS,
        "calves" to Muscle.CALVES, "tibialis" to Muscle.CALVES)

    @Volatile private var parts: List<Part>? = null

    fun parts(ctx: Context): List<Part> = parts ?: synchronized(this) {
        parts ?: ctx.assets.open("body.txt").bufferedReader().readLines().filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { l ->
            val a = l.split('|', limit = 3); if (a.size < 3) return@mapNotNull null
            val p = runCatching { PathParser.createPathFromPathData(a[2]) }.getOrNull() ?: return@mapNotNull null
            if (a[0] == "b") p.offset(-724f, 0f)
            Part(a[0] == "f", MAP[a[1]], a[1], p)
        }.also { parts = it }
    }

    /** Draws both sides into an android Canvas box, for the share cards. [col] is null for plain skin. */
    fun draw(ctx: Context, c: android.graphics.Canvas, box: RectF, skin: Int, line: Int, col: (Muscle) -> Int?) {
        val k = minOf(box.width() / (724f * 2 + 20f), box.height() / 1448f)
        val ox = box.left + (box.width() - (724f * 2 + 20f) * k) / 2
        val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 1.2f; color = line }
        for (front in listOf(true, false)) {
            c.save(); c.translate(ox + if (front) 0f else (724f + 20f) * k, box.top); c.scale(k, k)
            fill.color = skin
            parts(ctx).filter { it.front == front && it.slug == "outline" }.forEach { c.drawPath(it.path, fill) }
            for (p in parts(ctx).filter { it.front == front }) {
                if (p.slug == "outline") continue
                fill.color = (p.muscle?.let(col)) ?: if (p.slug == "hair") line else skin
                c.drawPath(p.path, fill)
            }
            stroke.strokeWidth = 2.2f / k * .5f
            parts(ctx).filter { it.front == front && it.slug == "outline" }.forEach { c.drawPath(it.path, stroke) }
            c.restore()
        }
    }
}

/** Both sides on a Compose canvas: [col] picks each muscle's colour, null for plain skin. */
@Composable
private fun Figure(h: Dp, col: (Muscle) -> Color?) {
    val ctx = LocalContext.current
    val parts = BodyArt.parts(ctx)
    val skin = lerp(C.card, C.mute, if (C.dark) .22f else .12f)
    val line = lerp(skin, C.ink, .4f)
    Canvas(Modifier.fillMaxWidth().height(h)) {
        val k = minOf(size.width / (724f * 2 + 20f), size.height / 1448f)
        val ox = (size.width - (724f * 2 + 20f) * k) / 2
        for (front in listOf(true, false)) {
            withTransform({ translate(ox + if (front) 0f else (724f + 20f) * k, 0f); scale(k, k, androidx.compose.ui.geometry.Offset.Zero) }) {
                for (p in parts) if (p.front == front && p.slug == "outline") drawPath(p.path.asComposePath(), skin)
                for (p in parts) if (p.front == front && p.slug != "outline")
                    drawPath(p.path.asComposePath(), (p.muscle?.let(col)) ?: if (p.slug == "hair") line else skin)
                for (p in parts) if (p.front == front && p.slug == "outline") drawPath(p.path.asComposePath(), line, style = Stroke(1.4f / k))
            }
        }
    }
}

/** Each muscle coloured by [v] (0..1): plain when untouched, [color] when worked hard. */
@Composable
fun BodyMap(v: Map<Muscle, Double>, modifier: Modifier = Modifier, color: Color = C.acc, h: Dp = 220.dp) {
    val rest = lerp(C.card, C.mute, if (C.dark) .6f else .45f); val low = lerp(rest, color, .25f)
    Column(modifier) { Figure(h) { m -> val t = (v[m] ?: 0.0).toFloat().coerceIn(0f, 1f); if (t <= .02f) rest else lerp(low, color, .3f + .7f * t) } }
}

val PRIMARY = Color(0xFFE0533A)
val SECONDARY = Color(0xFF6E9FE0)

/** The target muscles of an exercise: main ones in red, helpers in blue, with a small legend. */
@Composable
fun TargetMap(primary: List<Muscle>, secondary: List<Muscle>, modifier: Modifier = Modifier, h: Dp = 220.dp, legend: Boolean = true) {
    val rest = lerp(C.card, C.mute, if (C.dark) .6f else .45f)
    Column(modifier) {
        Figure(h) { m -> when (m) { in primary -> PRIMARY; in secondary -> SECONDARY; else -> rest } }
        if (legend) Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Dot(PRIMARY, 10.dp); Text(" " + tr("основные", "main"), style = T.small); Spacer(Modifier.width(16.dp))
            Dot(SECONDARY, 10.dp); Text(" " + tr("помогают", "helping"), style = T.small)
        }
    }
}


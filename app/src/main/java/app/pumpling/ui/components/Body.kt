package app.pumpling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pumpling.domain.Muscle
import app.pumpling.ui.theme.C

/**
 * A figure, front and back side by side, on a 220×200 grid. Base shapes make the silhouette;
 * muscle shapes are painted over it. Shared by the screens and the share cards.
 */
object BodyShapes {
    /** A rounded rectangle (r > 0) or an oval (r < 0), centred on x,y. */
    class S(val m: Muscle?, val x: Float, val y: Float, val w: Float, val h: Float, val r: Float)
    private fun both(m: Muscle?, x: Float, y: Float, w: Float, h: Float, r: Float) = listOf(S(m, x, y, w, h, r), S(m, 100 - x, y, w, h, r))
    private val base = listOf(S(null, 50f, 15f, 21f, 25f, -1f), S(null, 50f, 31f, 11f, 8f, 3f), S(null, 50f, 70f, 46f, 62f, 14f), S(null, 50f, 106f, 40f, 24f, 10f)) +
        both(null, 23f, 58f, 13f, 34f, 6.5f) + both(null, 18f, 90f, 11f, 30f, 5.5f) + both(null, 16f, 109f, 9f, 10f, -1f) +
        both(null, 41.5f, 138f, 19f, 50f, 9.5f) + both(null, 41.5f, 180f, 14f, 36f, 7f) + both(null, 41.5f, 197f, 15f, 6f, 3f)
    val front: List<S> = base + both(Muscle.DELTS, 27f, 44f, 17f, 15f, -1f) + both(Muscle.CHEST, 40f, 53f, 19f, 17f, 7f) + both(Muscle.BI, 22.5f, 63f, 10f, 22f, 5f) +
        both(Muscle.FORE, 18f, 90f, 9f, 24f, 4.5f) + listOf(S(Muscle.ABS, 50f, 85f, 21f, 32f, 6f)) + both(Muscle.QUADS, 41.5f, 136f, 16f, 42f, 8f) +
        both(Muscle.CALVES, 41.5f, 180f, 10f, 26f, 5f)
    val back: List<S> = base + listOf(S(Muscle.UPPER, 50f, 45f, 32f, 15f, 6f)) + both(Muscle.DELTS, 27f, 44f, 17f, 15f, -1f) + both(Muscle.LATS, 38f, 71f, 14f, 30f, 7f) +
        both(Muscle.TRI, 22.5f, 63f, 10f, 22f, 5f) + both(Muscle.FORE, 18f, 90f, 9f, 24f, 4.5f) + listOf(S(Muscle.LOWER, 50f, 92f, 18f, 12f, 5f)) +
        both(Muscle.GLUTES, 41f, 112f, 18f, 18f, 8f) + both(Muscle.HAMS, 41.5f, 141f, 16f, 36f, 8f) + both(Muscle.CALVES, 41.5f, 178f, 12f, 28f, 6f)
}

/** Each muscle coloured by [v] (0..1): pale when untouched, [color] when worked hard. */
@Composable
fun BodyMap(v: Map<Muscle, Double>, modifier: Modifier = Modifier, color: Color = C.acc, h: Dp = 220.dp) {
    // the silhouette stands out on light and dark cards alike
    val base = lerp(C.card, C.mute, if (C.dark) .32f else .22f); val low = lerp(base, C.acc, .25f)
    Canvas(modifier.fillMaxWidth().height(h)) {
        val k = minOf(size.width / 230f, size.height / 204f); val ox = (size.width - 230 * k) / 2
        fun draw(list: List<BodyShapes.S>, dx: Float) = list.forEach { s ->
            val col = if (s.m == null) base else { val t = (v[s.m] ?: 0.0).toFloat().coerceIn(0f, 1f); if (t <= .02f) lerp(base, C.mute, .18f) else lerp(low, color, .35f + .65f * t) }
            val o = Offset(ox + (dx + s.x - s.w / 2) * k, (2 + s.y - s.h / 2) * k); val sz = Size(s.w * k, s.h * k)
            if (s.r < 0) drawOval(col, o, sz) else drawRoundRect(col, o, sz, CornerRadius(s.r * k))
        }
        draw(BodyShapes.front, 5f); draw(BodyShapes.back, 125f)
    }
}

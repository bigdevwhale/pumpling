package app.pumpling.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.pumpling.data.Pet
import app.pumpling.domain.Shop
import kotlin.math.roundToInt

/**
 * The basement gym: the generated room, the things bought for it, and the team on the mat –
 * the leader in front, companions either side.
 */
@Composable
fun GymScene(team: List<Pet>, items: Set<String>, pose: Pose, modifier: Modifier = Modifier, preview: String? = null, sky: Sky? = null) {
    val ctx = LocalContext.current
    val tt by androidx.compose.animation.core.rememberInfiniteTransition(label = "sky").animateFloat(0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(9000, easing = androidx.compose.animation.core.LinearEasing)), label = "t")
    val skyT = if (app.pumpling.BuildConfig.SHOTS) .3f else tt
    val bg = Art.get(ctx, "gym_bg")
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.5f)) {
        val wDp = maxWidth; val hDp = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            if (bg != null) drawImage(bg, IntOffset.Zero, IntSize(bg.width, bg.height), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Medium)
            else {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFC9876A), Color(0xFFB0705A))), size = size.copy(height = size.height * .68f))
                drawRect(Color(0xFF8A5A3E), Offset(0f, size.height * .68f), size.copy(height = size.height * .32f))
                drawOval(Color(0xFF5E8F5A), Offset(size.width * .22f, size.height * .78f), size.copy(width = size.width * .56f, height = size.height * .16f))
            }
            if (sky != null) drawIntoCanvas { drawSky(it.nativeCanvas, android.graphics.RectF(0f, 0f, size.width, size.height), sky, skyT) }
            val shown = (Shop.all.filter { (it.id in items || it.id == preview) && it.id != "lights" }).sortedBy { if (it.wall) 0 else 1 }
            for (it in shown) {
                val img = Art.item(ctx, it.id) ?: continue
                val w = size.width * it.w; val h = w * img.height / img.width
                val x = size.width * it.x - w / 2; val y = if (it.wall) size.height * it.y - h / 2 else size.height * it.y - h
                drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(w.roundToInt(), h.roundToInt()),
                    alpha = if (it.id == preview && it.id !in items) .85f else 1f, filterQuality = FilterQuality.Medium)
            }
            if ("lights" in items || preview == "lights") garland(Size(size.width, size.height))
        }
        // the leader in the middle, companions in their own spots either side – they never overlap
        team.filter { it.slot in 1..2 }.forEach { p ->
            key(p.id) {
                val s = hDp * .34f
                Box(Modifier.offset(x = wDp * (if (p.slot == 1) .03f else .74f), y = hDp * .95f - s)) { PetView(p, s, if (pose == Pose.WORK) Pose.CHEER else pose) }
            }
        }
        team.firstOrNull { it.slot == 0 }?.let { lead -> key(lead.id) { val s = hDp * .58f; Box(Modifier.offset(x = (wDp - s) / 2, y = hDp * .98f - s)) { PetView(lead, s, pose) } } }
    }
}


/** Fairy lights strung under the ceiling: small warm bulbs on a sagging wire. */
fun DrawScope.garland(sz: Size) {
    val x0 = sz.width * .14f; val x1 = sz.width * .86f; val y0 = sz.height * .06f; val sag = sz.height * .05f
    fun at(t: Float) = Offset(x0 + (x1 - x0) * t, y0 + sag * 4 * t * (1 - t))
    val path = Path().apply { val a = at(0f); moveTo(a.x, a.y); for (i in 1..40) { val p = at(i / 40f); lineTo(p.x, p.y) } }
    drawPath(path, Color(0xFF3B2A22), style = Stroke(sz.width * .003f))
    val cols = listOf(Color(0xFFFFD36B), Color(0xFFFF9F6B), Color(0xFFFFE9A8), Color(0xFFFF7F7F))
    val n = 13; val r = sz.width * .009f
    for (i in 0 until n) { val p = at((i + .5f) / n); val c = cols[i % cols.size]
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .45f), Color.Transparent), Offset(p.x, p.y + r * 1.6f), r * 4.5f), r * 4.5f, Offset(p.x, p.y + r * 1.6f))
        drawRect(Color(0xFF3B2A22), Offset(p.x - r * .45f, p.y), Size(r * .9f, r * .7f))
        drawCircle(c, r, Offset(p.x, p.y + r * 1.6f)) }
}

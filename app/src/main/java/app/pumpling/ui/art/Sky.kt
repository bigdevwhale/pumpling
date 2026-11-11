package app.pumpling.ui.art

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.pumpling.data.Outside
import app.pumpling.data.Settings
import app.pumpling.data.Sun
import app.pumpling.data.Weather
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * What the gym window shows: [day] 0 night .. 1 full day, [glow] 0..1 how much dawn or dusk colours it,
 * and the weather (CLEAR when unknown).
 */
data class Sky(val day: Float, val glow: Float, val weather: Weather) {
    val night get() = day < .25f

    companion object {
        /** The sky now in the user's town: the real weather and the sun by its rise and set there. */
        fun of(s: Settings, now: Long = System.currentTimeMillis()): Sky {
            val o = Outside.of(s.outside)?.takeIf { now - it.at < 6 * 3600_000L }
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val (rise, set) = when {
                o != null && o.sunrise in today..(today + 86400_000) -> o.sunrise to o.sunset
                !s.lat.isNaN() -> Sun.riseSet(s.lat, s.lon, now)
                else -> (today + 7 * 3600_000L) to (today + 19 * 3600_000L)
            }
            val h = 3600_000f
            // light rises over the hour around sunrise and falls over the hour around sunset
            val day = min(((now - rise) / h + .5f).coerceIn(0f, 1f), ((set - now) / h + .5f).coerceIn(0f, 1f))
            val glow = max(1f - abs(now - rise) / (1.2f * h), 1f - abs(now - set) / (1.2f * h)).coerceIn(0f, 1f)
            return Sky(day, glow, o?.weather ?: Weather.CLEAR)
        }
    }
}

/** The three panes of the window in the gym picture, as parts of its width and height. */
private val PANES = listOf(.3646f to .4447f, .4557f to .5449f, .5553f to .6354f)
private const val PANE_TOP = .1885f; private const val PANE_BOTTOM = .2393f

private fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(c: Int, s: Int) = (c shr s) and 0xFF
    fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t.coerceIn(0f, 1f)).toInt()
    return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

/** Paints the sky into the window panes of a gym picture drawn in [r]. [t] 0..1 loops for rain, snow and twinkling. */
fun drawSky(c: Canvas, r: RectF, sky: Sky, t: Float) {
    val w = r.width(); val h = r.height()
    val y0 = r.top + h * PANE_TOP; val y1 = r.top + h * PANE_BOTTOM
    val grey = sky.weather == Weather.OVERCAST || sky.weather == Weather.RAIN || sky.weather == Weather.STORM || sky.weather == Weather.FOG || sky.weather == Weather.SNOW
    var top = mix(0xFF0B1430.toInt(), 0xFF4F9BE0.toInt(), sky.day); var bottom = mix(0xFF1D2B57.toInt(), 0xFFA9D8F5.toInt(), sky.day)
    if (sky.glow > 0) { top = mix(top, 0xFF5A4C9C.toInt(), sky.glow * .6f); bottom = mix(bottom, 0xFFF59A62.toInt(), sky.glow * .8f) }
    if (grey) { top = mix(top, mix(0xFF262C38.toInt(), 0xFF8C96A2.toInt(), sky.day), .75f); bottom = mix(bottom, mix(0xFF2E3442.toInt(), 0xFFB3BBC4.toInt(), sky.day), .75f) }
    val sky0 = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, y0, 0f, y1, top, bottom, Shader.TileMode.CLAMP) }
    val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    for ((i, p) in PANES.withIndex()) {
        val x0 = r.left + w * p.first; val x1 = r.left + w * p.second
        val pane = RectF(x0, y0, x1, y1)
        c.save(); c.clipRect(pane)
        c.drawRect(pane, sky0)
        val pw = x1 - x0; val ph = y1 - y0
        if (sky.night && !grey) {
            dot.color = 0xFFFFF6D8.toInt()
            for (k in 0 until 6) { val tw = .5f + .5f * sin((t * 2 * PI + k * 1.7 + i).toFloat()); dot.alpha = (120 + 120 * tw).toInt()
                c.drawCircle(x0 + pw * ((k * 37 + i * 13) % 100) / 100f, y0 + ph * ((k * 53 + i * 29) % 70) / 100f + ph * .08f, w * .0016f, dot) }
            if (i == 2) { dot.color = 0xFFFFF1C2.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .7f, y0 + ph * .38f, ph * .2f, dot)
                dot.color = top; c.drawCircle(x0 + pw * .7f + ph * .08f, y0 + ph * .32f, ph * .17f, dot) }
        }
        if (!sky.night && sky.weather == Weather.CLEAR && i == 0) { dot.color = 0xFFFFE89A.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .3f, y0 + ph * .4f, ph * .22f, dot) }
        if (sky.weather == Weather.CLOUDY || sky.weather == Weather.CLEAR && i == 1) {
            dot.color = if (sky.night) 0xFF4A5576.toInt() else 0xFFFFFFFF.toInt(); dot.alpha = 230
            val cx = x0 + pw * ((.2f + t * .6f + i * .33f) % 1.2f - .1f); val cy = y0 + ph * .62f
            c.drawCircle(cx, cy, ph * .18f, dot); c.drawCircle(cx + ph * .22f, cy - ph * .08f, ph * .22f, dot); c.drawCircle(cx + ph * .44f, cy, ph * .16f, dot)
        }
        if (grey && sky.weather != Weather.FOG) {
            dot.color = mix(0xFF39404E.toInt(), 0xFFD5DAE0.toInt(), sky.day); dot.alpha = 210
            for (k in 0 until 3) { val cx = x0 + pw * (((k * .37f + i * .23f + t * .3f) % 1.3f) - .15f); val cy = y0 + ph * (.25f + k * .22f)
                c.drawCircle(cx, cy, ph * .2f, dot); c.drawCircle(cx + ph * .25f, cy - ph * .06f, ph * .24f, dot); c.drawCircle(cx + ph * .5f, cy, ph * .18f, dot) }
        }
        if (sky.weather == Weather.RAIN || sky.weather == Weather.STORM) {
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99C8D8EE.toInt(); strokeWidth = w * .0012f }
            for (k in 0 until 9) { val fx = ((k * 0.137f + i * .31f) % 1f); val fy = ((t * 3 + k * .23f) % 1f)
                val x = x0 + pw * fx; val y = y0 - ph * .2f + ph * 1.3f * fy; c.drawLine(x, y, x - ph * .08f, y + ph * .22f, line) }
            if (sky.weather == Weather.STORM && t % .5f < .025f) { dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 160; c.drawRect(pane, dot) }
        }
        if (sky.weather == Weather.SNOW) {
            dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 230
            for (k in 0 until 10) { val fx = ((k * 0.173f + i * .29f) % 1f) + .03f * sin((t * 6 + k).toFloat()); val fy = ((t + k * .1f) % 1f)
                c.drawCircle(x0 + pw * fx, y0 - ph * .1f + ph * 1.2f * fy, w * .0022f, dot) }
        }
        if (sky.weather == Weather.FOG) { dot.color = 0xFFE6E9EE.toInt(); dot.alpha = 150; c.drawRect(pane, dot) }
        c.restore()
    }
}

/** The room darkens at night (over the room and its things, under the garland and the pets); the ceiling lamp keeps it warm. */
fun drawNight(c: Canvas, r: RectF, sky: Sky) {
    val w = r.width(); val h = r.height()
    if (sky.day < .9f) {
        val dark = ((1 - sky.day) * .32f * 255).toInt()
        c.drawRect(r, Paint().apply { color = (dark shl 24) or 0x101830 })
        c.drawCircle(r.left + w * .5f, r.top + h * .12f, w * .5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(r.left + w * .5f, r.top + h * .12f, w * .5f, (((1 - sky.day) * .22f * 255).toInt() shl 24) or 0xFFC977, 0x00FFC977, Shader.TileMode.CLAMP) })
    }
}

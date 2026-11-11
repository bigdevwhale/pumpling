package app.pumpling.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.pumpling.R
import app.pumpling.data.Pet
import app.pumpling.domain.Catalog
import app.pumpling.domain.Form
import app.pumpling.domain.Kind
import app.pumpling.domain.Muscle
import app.pumpling.domain.Pets
import app.pumpling.domain.Rec
import app.pumpling.domain.Records
import app.pumpling.domain.Shop
import app.pumpling.domain.Span
import app.pumpling.domain.Stage
import app.pumpling.domain.Stats
import app.pumpling.domain.clock
import app.pumpling.domain.count
import app.pumpling.domain.dayOf
import app.pumpling.domain.dayShort
import app.pumpling.domain.dur
import app.pumpling.domain.e1rm
import app.pumpling.domain.fmt
import app.pumpling.domain.fmt1
import app.pumpling.domain.fmtDayYear
import app.pumpling.domain.hours
import app.pumpling.domain.km
import app.pumpling.domain.nat
import app.pumpling.domain.setLine
import app.pumpling.domain.sp
import app.pumpling.domain.stage
import app.pumpling.domain.tonnage
import app.pumpling.domain.tr
import app.pumpling.domain.weekStart
import app.pumpling.domain.wu
import app.pumpling.ui.Data
import app.pumpling.ui.art.Art
import app.pumpling.ui.art.Pose
import app.pumpling.ui.art.frameOf
import app.pumpling.ui.components.BodyShapes
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class CardKind(val ru: String, val en: String) {
    WORKOUT("Тренировка", "Workout"), RECORD("Рекорд", "Record"), WEEK("Неделя", "Week"), MONTH("Месяц", "Month"), YEAR("Год", "Year"),
    PET("Питомец", "Pet"), GYM("Зал", "Gym"), BESTIARY("Бестиарий", "Bestiary"), EXERCISE("Упражнение", "Exercise"), MUSCLES("Мышцы", "Muscles"),
    RECORDS("Рекорды", "Records"), PERIOD("Итоги", "Summary"), CARDIO("Кардио", "Cardio"), LIFTS("Упражнения", "Lifts");
    val label get() = tr(ru, en)
}

/** What to draw: a card kind and what it is about (a workout or set id, an exercise, a span). */
data class ShareReq(val kind: CardKind, val id: Long = 0, val ex: String = "", val span: Span? = null)

/** How a card looks: the format, the background, whether weights are shown. */
data class Look(val story: Boolean = false, val bg: Int = 0, val hideWeights: Boolean = false)

/** Backgrounds: two colours top to bottom and whether the text is light; -1 is the gym photo. */
val BACKS = listOf(
    Triple(0xFFE8613C.toInt(), 0xFF9C2F4A.toInt(), true), Triple(0xFF2F8F83.toInt(), 0xFF173F4F.toInt(), true), Triple(0xFF3A2E5C.toInt(), 0xFF161227.toInt(), true),
    Triple(0xFFF5EFE6.toInt(), 0xFFEADCC8.toInt(), false), Triple(0xFFFFD36B.toInt(), 0xFFE8873C.toInt(), false), Triple(-1, -1, true),
)

class Painter(private val ctx: Context, private val d: Data, private val look: Look) {
    private val W = 1080; private val H = if (look.story) 1920 else 1350
    private val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD
    private val reg: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_regular) ?: Typeface.DEFAULT
    private val num: Typeface = ResourcesCompat.getFont(ctx, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD
    private val back = BACKS[look.bg.coerceIn(0, BACKS.lastIndex)]
    private val light = back.third
    private val ink = if (light) 0xFFFFFFFF.toInt() else 0xFF2A2420.toInt()
    private val sub = if (light) 0xC8FFFFFF.toInt() else 0xA02A2420.toInt()
    private val accent = if (light) 0xFFFFD36B.toInt() else 0xFFC94C27.toInt()
    private val panel = if (light) 0x24FFFFFF else 0x142A2420
    private val inkOnAccent = if (light) 0xFF2A2420.toInt() else 0xFFFFFFFF.toInt()
    /** Top of the content: lower in the tall story format, below the header. */
    private val top = if (look.story) 300f else 200f
    private val bottom get() = H - 150f

    private fun hide(s: String) = if (look.hideWeights) "•••" else s
    private fun p(color: Int = ink) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    // ---------------- the frame every card shares ----------------

    private fun base(kicker: String): Pair<Bitmap, Canvas> {
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        if (back.first == -1) {
            Art.get(ctx, "gym_bg")?.asAndroidBitmap()?.let { g -> val s = H.toFloat() / g.height; val w = g.width * s
                c.drawBitmap(g, Rect(0, 0, g.width, g.height), RectF((W - w) / 2, 0f, (W + w) / 2, H.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG)) }
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xA0140C08.toInt(), 0xE6140C08.toInt(), Shader.TileMode.CLAMP) })
        } else {
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, W * .4f, H.toFloat(), back.first, back.second, Shader.TileMode.CLAMP) })
            // soft blobs for depth
            for ((x, y, r) in listOf(Triple(.92f, .08f, .42f), Triple(.05f, .62f, .36f), Triple(.8f, .95f, .3f)))
                c.drawCircle(W * x, H * y, W * r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W * x, H * y, W * r, if (light) 0x30FFFFFF else 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP) })
        }
        // header: the brand pill and what the card is
        val hy = if (look.story) 130f else 70f
        c.drawRoundRect(RectF(60f, hy, 60f + 250f, hy + 64f), 32f, 32f, p(panel))
        Art.pet(ctx, d.leader?.species ?: "bull", 0)?.asAndroidBitmap()?.let { img -> val s = 48f / max(img.width, img.height)
            c.drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(76f, hy + 8f, 76f + img.width * s, hy + 8f + img.height * s), Paint(Paint.FILTER_BITMAP_FLAG)) }
        c.t("Pumpling", 136f, hy + 44f, 32f, bold)
        c.t(kicker.uppercase(), W - 60f, hy + 44f, 30f, bold, sub, Paint.Align.RIGHT, maxW = W - 400f)
        return b to c
    }

    private fun Canvas.t(s: String, x: Float, y: Float, size: Float, face: Typeface = bold, color: Int = ink, align: Paint.Align = Paint.Align.LEFT, maxW: Float = when (align) { Paint.Align.CENTER -> W - 120f; Paint.Align.RIGHT -> x - 60f; else -> W - x - 60f }) {
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = face; this.color = color; textAlign = align }
        var str = s; if (pt.measureText(str) > maxW) { while (pt.measureText("$str…") > maxW && str.length > 2) str = str.dropLast(1); str += "…" }
        drawText(str, x, y, pt)
    }
    private fun Canvas.panel(x: Float, y: Float, w: Float, h: Float, r: Float = 36f, color: Int = panel) = drawRoundRect(RectF(x, y, x + w, y + h), r, r, p(color))
    private fun Canvas.facts(items: List<Pair<String, String>>, y: Float, h: Float = 170f) {
        val n = items.size; val gap = 18f; val w = (W - 120f - gap * (n - 1)) / n
        items.forEachIndexed { i, (v, l) -> val x = 60f + i * (w + gap); panel(x, y, w, h)
            t(v, x + w / 2, y + h * .52f, if (v.length > 7) 44f else 58f, num, ink, Paint.Align.CENTER, w - 24); t(l, x + w / 2, y + h * .8f, 28f, reg, sub, Paint.Align.CENTER, w - 24) }
    }
    private fun Canvas.grid(items: List<Pair<String, String>>, y: Float, cols: Int = 2, h: Float = 150f): Float {
        val gap = 18f; val w = (W - 120f - gap * (cols - 1)) / cols
        items.forEachIndexed { i, (v, l) -> val x = 60f + (i % cols) * (w + gap); val yy = y + (i / cols) * (h + gap); panel(x, yy, w, h)
            t(v, x + 32, yy + h * .5f, 56f, num, ink, maxW = w - 50); t(l, x + 32, yy + h * .8f, 28f, reg, sub, maxW = w - 50) }
        return y + ((items.size + cols - 1) / cols) * (h + gap)
    }
    /** A pet standing in a square box of [size] whose bottom edge is at [foot]. */
    private fun Canvas.pet(p: Pet?, pose: Pose, cx: Float, foot: Float, size: Float, shadow: Boolean = true) {
        p ?: return
        val st = p.stage()
        val img = (if (st == Stage.EGG) Art.egg(ctx, p.species) else Art.pet(ctx, p.species, frameOf(st, pose)))?.asAndroidBitmap() ?: return
        val scale = when (st) { Stage.EGG -> .7f; Stage.BABY -> .62f; Stage.TEEN -> .8f; else -> .96f }
        val s = min(size / img.width, size * .9f / img.height) * scale; val w = img.width * s; val h = img.height * s
        if (shadow) drawOval(RectF(cx - w * .36f, foot - h * .04f, cx + w * .36f, foot + h * .04f), p(0x33000000))
        drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(cx - w / 2, foot - h, cx + w / 2, foot), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
    /** The pet at the bottom, as big as the room left under [y] allows; nothing when it would be too small. */
    private fun Canvas.roomPet(pt: Pet?, pose: Pose, y: Float, maxSize: Float) { val room = bottom - y - 10; if (room >= 220) pet(pt, pose, W / 2f, bottom, min(room, maxSize)) }
    private fun Canvas.footer() {
        t(tr("Pumpling · питомец, который тренируется с тобой", "Pumpling · a pet that trains with you"), W / 2f, H - 64f, 28f, reg, sub, Paint.Align.CENTER)
    }
    private fun Canvas.line(pts: List<Pair<Double, Double>>, x: Float, y: Float, w: Float, h: Float) {
        if (pts.size < 2) return
        val x0 = pts.minOf { it.first }; val x1 = max(pts.maxOf { it.first }, x0 + 1e-9); var y0 = pts.minOf { it.second }; var y1 = pts.maxOf { it.second }
        val pad = max((y1 - y0) * .15, 1e-6); y0 -= pad; y1 += pad
        fun o(q: Pair<Double, Double>) = (x + (q.first - x0) / (x1 - x0) * w).toFloat() to (y + h - (q.second - y0) / (y1 - y0) * h).toFloat()
        val path = Path(); val fill = Path()
        pts.forEachIndexed { i, q -> val (a, b) = o(q); if (i == 0) { path.moveTo(a, b); fill.moveTo(a, y + h); fill.lineTo(a, b) } else { path.lineTo(a, b); fill.lineTo(a, b) } }
        fill.lineTo(o(pts.last()).first, y + h); fill.close()
        drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, y, 0f, y + h, (accent and 0xFFFFFF) or 0x66000000, (accent and 0xFFFFFF), Shader.TileMode.CLAMP) })
        drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.STROKE; strokeWidth = 7f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND })
        val (lx, ly) = o(pts.last()); drawCircle(lx, ly, 12f, p(accent)); drawCircle(lx, ly, 5f, p(inkOnAccent))
    }
    private fun Canvas.bars(values: List<Pair<String, Double>>, x: Float, y: Float, w: Float, h: Float) {
        val mx = max(values.maxOfOrNull { it.second } ?: 0.0, 1e-9); val gap = 10f; val bw = (w - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, (l, v) -> val bh = max(8f, ((h - 50) * v / mx).toFloat()); val left = x + i * (bw + gap)
            drawRoundRect(RectF(left, y + h - 50 - bh, left + bw, y + h - 50), 12f, 12f, p(if (v > 0) accent else panel))
            if (values.size <= 14) t(l, left + bw / 2, y + h - 12, 24f, reg, sub, Paint.Align.CENTER, bw + gap) }
    }
    private fun Canvas.body(v: Map<Muscle, Double>, x: Float, y: Float, w: Float) {
        val k = w / 230f
        fun draw(list: List<BodyShapes.S>, dx: Float) = list.forEach { s ->
            val col = if (s.m == null) panel or 0x22000000 else { val t = (v[s.m] ?: 0.0).toFloat().coerceIn(0f, 1f); if (t < .03f) panel or 0x33000000 else blend(0x66FFFFFF and (accent or 0xFF000000.toInt()), accent, t) }
            val r = RectF(x + (dx + s.x - s.w / 2) * k, y + (s.y - s.h / 2) * k, x + (dx + s.x + s.w / 2) * k, y + (s.y + s.h / 2) * k)
            if (s.r < 0) drawOval(r, p(col)) else drawRoundRect(r, s.r * k, s.r * k, p(col))
        }
        draw(BodyShapes.front, 5f); draw(BodyShapes.back, 125f)
    }
    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toInt()
        return (m(24) shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
    }

    /** The whole gym in [r]: the room, the things, the garland and the team. */
    private fun Canvas.gym(r: RectF) {
        save(); clipPath(Path().apply { addRoundRect(r, 40f, 40f, Path.Direction.CW) })
        Art.get(ctx, "gym_bg")?.asAndroidBitmap()?.let { drawBitmap(it, Rect(0, 0, it.width, it.height), r, Paint(Paint.FILTER_BITMAP_FLAG)) } ?: drawRect(r, p(0xFFB0705A.toInt()))
        val items = d.s.items - d.s.hidden
        val sky = app.pumpling.ui.art.Sky.of(d.s)
        app.pumpling.ui.art.drawSky(this, r, sky, .3f)
        for (it in Shop.all.filter { it.id in items && it.id != "lights" }.sortedBy { if (it.wall) 0 else 1 }) {
            val img = Art.item(ctx, it.id)?.asAndroidBitmap() ?: continue
            val w = r.width() * it.w; val h = w * img.height / img.width
            val x = r.left + r.width() * it.x - w / 2; val y = r.top + if (it.wall) r.height() * it.y - h / 2 else r.height() * it.y - h
            drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(x, y, x + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        if ("lights" in items) {
            val x0 = r.left + r.width() * .14f; val x1 = r.left + r.width() * .86f; val y0 = r.top + r.height() * .06f; val sag = r.height() * .05f
            fun at(t: Float) = (x0 + (x1 - x0) * t) to (y0 + sag * 4 * t * (1 - t))
            val wire = Path(); for (i in 0..40) { val (a, b) = at(i / 40f); if (i == 0) wire.moveTo(a, b) else wire.lineTo(a, b) }
            drawPath(wire, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B2A22.toInt(); style = Paint.Style.STROKE; strokeWidth = r.width() * .003f })
            val cols = intArrayOf(0xFFFFD36B.toInt(), 0xFFFF9F6B.toInt(), 0xFFFFE9A8.toInt(), 0xFFFF7F7F.toInt()); val rr = r.width() * .009f
            for (i in 0 until 13) { val (a, b) = at((i + .5f) / 13); val col = cols[i % 4]
                drawCircle(a, b + rr * 1.6f, rr * 4.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(a, b + rr * 1.6f, rr * 4.5f, (col and 0xFFFFFF) or 0x73000000, col and 0xFFFFFF, Shader.TileMode.CLAMP) })
                drawCircle(a, b + rr * 1.6f, rr, p(col)) }
        }
        val h = r.height(); val w = r.width()
        d.team.filter { it.slot in 1..2 }.forEach { pt -> val s = h * .34f; pet(pt, Pose.CHEER, r.left + w * (if (pt.slot == 1) .03f else .74f) + s / 2, r.top + h * .95f, s) }
        d.leader?.let { pet(it, Pose.IDLE, r.centerX(), r.top + h * .98f, h * .58f) }
        restore()
    }

    // ---------------- the cards ----------------

    fun render(r: ShareReq): Pair<Bitmap, String> = when (r.kind) {
        CardKind.WORKOUT -> workout(r.id); CardKind.RECORD -> record(r.id); CardKind.WEEK -> period(Span.of(Span.WEEK)); CardKind.MONTH -> period(Span.of(Span.MONTH))
        CardKind.YEAR -> year(r.span ?: Span.of(Span.YEAR)); CardKind.PET -> pet(r.id); CardKind.GYM -> gymCard(); CardKind.BESTIARY -> bestiary()
        CardKind.EXERCISE -> exercise(r.ex); CardKind.MUSCLES -> muscles(r.span ?: Span.of(Span.MONTH)); CardKind.RECORDS -> records(r.span ?: Span.of(Span.MONTH))
        CardKind.PERIOD -> period(r.span ?: Span.of(Span.MONTH)); CardKind.CARDIO -> cardio(r.span ?: Span.of(Span.MONTH)); CardKind.LIFTS -> lifts(r.span ?: Span.of(Span.MONTH))
    }

    private fun stats(span: Span) = Stats(d.workouts, d.sets, d.bodies, d.s, span)
    private val allTime get() = Span.of(Span.ALL, 0, d.finished.minOfOrNull { dayOf(it.start) } ?: 0L)
    private fun spanName(sp: Span) = when (sp.kind) { Span.WEEK -> tr("Неделя", "Week") + " · " + sp.label(); else -> sp.label() }

    private fun workout(id: Long): Pair<Bitmap, String> {
        val w = d.finished.firstOrNull { it.id == id } ?: d.finished.maxByOrNull { it.start }
        val (b, c) = base(w?.let { fmtDayYear(dayOf(it.start)) } ?: ""); if (w == null) return b to ""
        val st = stats(allTime); val sets = d.setsBy[w.id].orEmpty().filter { it.done && !it.warmup }
        c.t(w.title.ifEmpty { tr("Тренировка", "Workout") }, 60f, top + 70, 80f, maxW = 620f)
        c.t(tr("${sets.map { it.exId }.distinct().size} упражнений", "${sets.map { it.exId }.distinct().size} exercises") + (if (w.prs > 0) tr(" · ★ рекордов: ${w.prs}", " · ★ records: ${w.prs}") else ""), 60f, top + 130, 36f, reg, if (w.prs > 0) accent else sub, maxW = 620f)
        c.pet(d.leader, Pose.CHEER, W - 200f, top + 250, 320f)
        val ton = sets.sumOf { st.tonnageOf(it) }; val dist = sets.sumOf { it.meters }
        c.facts(listOf(app.pumpling.domain.span(w.end - w.start) to tr("время", "time"),
            (if (ton > 0 || dist == 0.0) hide(tonnage(ton)) else km(dist)) to (if (ton > 0 || dist == 0.0) tr("тоннаж", "tonnage") else tr("дистанция", "distance")), "${sets.size}" to tr("подходов", "sets")), top + 290)
        var y = top + 520; val rows = if (look.story) 14 else 8
        val groups = sets.groupBy { it.exId }.entries.toList()
        c.panel(60f, y - 30, W - 120f, min(groups.size, rows) * 70f + 40)
        y += 30
        groups.take(rows).forEach { (ex, l) -> val e = Catalog.of(ex)
            val best = l.maxBy { it.weight * 1000 + it.reps + it.secs + it.meters }
            val pr = l.any { it.pr.isNotEmpty() }
            c.t("${l.size}×", 96f, y, 36f, num, sub)
            c.t(e.name, 170f, y, 38f, bold, ink, maxW = 520f)
            c.t((if (pr) "★ " else "") + if (look.hideWeights && e.kind == Kind.WEIGHT) "× ${best.reps}" else setLine(e, best), W - 96f, y, 36f, num, if (pr) accent else ink, Paint.Align.RIGHT)
            y += 70 }
        if (groups.size > rows) c.t(tr("и ещё ${groups.size - rows}", "and ${groups.size - rows} more"), 96f, y, 30f, reg, sub)
        // room left: the team cheers at the bottom
        val room = bottom - y - 10
        if (room > 260) { val ls = min(room, 460f); d.team.filter { it.slot != 0 }.forEachIndexed { i, pt -> c.pet(pt, Pose.CHEER, if (i == 0) W * .2f else W * .8f, bottom, ls * .62f) }
            c.pet(d.leader, Pose.WORK, W / 2f, bottom, ls) }
        c.footer()
        return b to tr("Тренировка в Pumpling", "A workout in Pumpling")
    }

    private fun record(setId: Long): Pair<Bitmap, String> {
        val s = d.sets.firstOrNull { it.id == setId }; val w = s?.let { x -> d.finished.firstOrNull { it.id == x.workoutId } }
        val (b, c) = base(tr("новый рекорд", "new record")); if (s == null) return b to ""
        val ex = Catalog.of(s.exId); val r = s.pr.split(',').mapNotNull { Rec.of(it) }.firstOrNull() ?: Rec.WEIGHT
        // rays behind the pet
        val cx = W / 2f; val cy = top + (if (look.story) 420 else 330)
        val rays = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (accent and 0xFFFFFF) or 0x30000000 }
        for (i in 0 until 16) { val a = Math.toRadians(i * 22.5); val a2 = Math.toRadians(i * 22.5 + 9)
            c.drawPath(Path().apply { moveTo(cx, cy); lineTo(cx + (900 * Math.cos(a)).toFloat(), cy + (900 * Math.sin(a)).toFloat()); lineTo(cx + (900 * Math.cos(a2)).toFloat(), cy + (900 * Math.sin(a2)).toFloat()); close() }, rays) }
        c.pet(d.leader, Pose.WORK, cx, cy + (if (look.story) 380 else 300), if (look.story) 700f else 560f)
        val y = cy + (if (look.story) 520 else 400)
        c.t(ex.name, cx, y, 60f, bold, ink, Paint.Align.CENTER)
        val value = if (look.hideWeights && (r == Rec.WEIGHT || r == Rec.E1RM)) tr("тяжелее, чем когда-либо", "heavier than ever") else Records.show(ex, r, s).substringBefore(" (")
        c.t(value, cx, y + 120, if (value.length > 14) 70f else 110f, num, accent, Paint.Align.CENTER)
        c.t(r.label + (w?.let { " · " + fmtDayYear(dayOf(it.start)) } ?: ""), cx, y + 190, 36f, reg, sub, Paint.Align.CENTER)
        val before = d.doneSets.filter { it.exId == s.exId && it.id != s.id && !it.warmup && (d.finished.firstOrNull { f -> f.id == it.workoutId }?.start ?: 0) < (w?.start ?: 0) }
        if (before.isNotEmpty() && !look.hideWeights) c.t(tr("было: ", "before: ") + setLine(ex, before.maxBy { e1rm(it.weight, it.reps) + it.reps * .01 + it.secs + it.meters }), cx, y + 250, 34f, reg, sub, Paint.Align.CENTER)
        c.footer()
        return b to tr("Новый рекорд: ${ex.name}", "New record: ${ex.name}")
    }

    private fun period(sp: Span): Pair<Bitmap, String> {
        val (b, c) = base(spanName(sp)); val st = stats(sp)
        c.t(count(st.count, "тренировка", "тренировки", "тренировок", "workout", "workouts"), 60f, top + 70, 84f)
        val goalLine = if (sp.kind == Span.WEEK) tr("цель недели – ${d.s.weekGoal}", "weekly goal – ${d.s.weekGoal}") else tr("${st.activeDays} дней в движении", "${st.activeDays} active days")
        c.t(goalLine + (if (st.weeks.streak > 1) tr(" · ${st.weeks.streak} нед. подряд", " · ${st.weeks.streak} weeks in a row") else ""), 60f, top + 130, 36f, reg, sub)
        var y = top + 180
        if (sp.kind == Span.WEEK) {
            val days = st.workouts.map { dayOf(it.start) }.toSet(); val mon = weekStart(sp.from); val cell = (W - 120f) / 7
            (0..6).forEach { i -> val x = 60f + i * cell + cell / 2; val on = mon + i in days
                c.drawCircle(x, y + 70, cell * .36f, p(if (on) accent else panel)); if (on) c.t("✓", x, y + 88, 50f, bold, inkOnAccent, Paint.Align.CENTER)
                c.t(dayShort(i), x, y + 160, 28f, reg, sub, Paint.Align.CENTER) }
            y += 200
        } else if (sp.kind == Span.MONTH) {
            val dd = LocalDate.ofEpochDay(sp.from); val cell = (W - 120f) / 7; val first = dd.dayOfWeek.value - 1; val active = st.workouts.map { dayOf(it.start) }.toSet()
            val rh = if (look.story) cell * .7f else cell * .52f
            (0 until dd.lengthOfMonth()).forEach { i -> val k = i + first; val x = 60f + (k % 7) * cell; val yy = y + (k / 7) * rh
                val on = sp.from + i in active
                c.drawRoundRect(RectF(x + 6, yy, x + cell - 6, yy + rh - 10), 18f, 18f, p(if (on) accent else panel))
                c.t("${i + 1}", x + cell / 2, yy + rh * .6f, 28f, num, if (on) inkOnAccent else sub, Paint.Align.CENTER) }
            y += ((first + dd.lengthOfMonth() + 6) / 7) * rh + 20
        } else {
            c.panel(60f, y, W - 120f, 300f); c.bars(st.buckets { ws, _ -> ws.size.toDouble() }, 90f, y + 30, W - 180f, 260f); y += 330
        }
        val four = listOf(hours(st.secs) to tr("в зале", "training"), (if (st.tonnage > 0) hide(tonnage(st.tonnage)) else "${st.prs}") to (if (st.tonnage > 0) tr("поднято", "lifted") else tr("рекордов", "records")),
            fmt(st.setsN) to tr("рабочих подходов", "working sets"), (if (st.meters > 0) km(st.meters) else "${st.prs}") to (if (st.meters > 0) tr("кардио", "cardio") else tr("рекордов", "records")))
        y = if (look.story || sp.kind != Span.MONTH) c.grid(four, y + 10) else { c.facts(four.take(2) + four.last(), y + 10, 150f); y + 180 }
        if (look.story || sp.kind == Span.WEEK) c.roomPet(d.leader, Pose.CHEER, y, 520f)
        else st.funFacts().firstOrNull()?.let { c.t(it, W / 2f, y + 50, 32f, reg, sub, Paint.Align.CENTER) }
        c.footer()
        return b to tr("Мои тренировки: ${sp.label()}", "My training: ${sp.label()}")
    }

    private fun year(sp: Span): Pair<Bitmap, String> {
        val st = stats(sp); val year = LocalDate.ofEpochDay(sp.from).year
        val (b, c) = base(tr("мой $year", "my $year"))
        c.t(count(st.count, "тренировка", "тренировки", "тренировок", "workout", "workouts"), 60f, top + 70, 84f)
        c.t(tr("${st.activeDays} дней в движении · лучшая серия ${st.weeks.best} нед.", "${st.activeDays} active days · best streak ${st.weeks.best} weeks"), 60f, top + 130, 34f, reg, sub)
        val weeks = 53; val cell = (W - 120f) / weeks; val start = weekStart(sp.from); val mx = (st.heat.values.maxOrNull() ?: 1).coerceAtLeast(1)
        for (w in 0 until weeks) for (k in 0 until 7) { val day = start + w * 7 + k; if (day < sp.from || day > sp.to) continue
            val v = st.heat[day] ?: 0
            c.drawRoundRect(RectF(60 + w * cell, top + 180 + k * cell, 60 + w * cell + cell - 3, top + 180 + k * cell + cell - 3), 4f, 4f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (v == 0) panel else accent; if (v > 0) alpha = (90 + 165 * v / mx).coerceAtMost(255) }) }
        var y = c.grid(listOf(hours(st.secs) to tr("в зале", "training"), hide(tonnage(st.tonnage)) to tr("поднято", "lifted"), "${st.prs}" to tr("рекордов", "records"),
            (if (st.meters > 0) km(st.meters) else fmt(st.setsN)) to (if (st.meters > 0) tr("кардио", "cardio") else tr("подходов", "sets"))), top + 220 + 7 * cell)
        c.t(tr("Любимые упражнения", "Favourite exercises"), 60f, y + 30, 34f, bold, sub); y += 90
        st.exercises.take(3).forEachIndexed { i, e -> c.t("${i + 1}. ${e.ex.name}", 60f, y, 42f, bold, maxW = 700f); c.t(tr("${e.sets} подх.", "${e.sets} sets"), W - 60f, y, 34f, num, sub, Paint.Align.RIGHT); y += 62 }
        val team = d.pets.filter { it.hatched > 0 }.sortedByDescending { it.growth }.take(5); val ps = min(if (look.story) 210f else 170f, bottom - y)
        if (ps >= 110) team.forEachIndexed { i, pt -> c.pet(pt, Pose.CHEER, W / 2f + (i - (team.size - 1) / 2f) * (ps + 6), bottom, ps) }
        c.footer()
        return b to tr("Мой $year в Pumpling", "My $year in Pumpling")
    }

    private fun pet(id: Long): Pair<Bitmap, String> {
        val pt = d.pets.firstOrNull { it.id == id } ?: d.leader; val (b, c) = base(tr("мой питомец", "my pet")); if (pt == null) return b to ""
        val col = Pets.of(pt.species).color.toInt()
        val cy = top + (if (look.story) 450 else 290)
        c.drawCircle(W / 2f, cy, if (look.story) 400f else 320f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W / 2f, cy, 420f, (col and 0xFFFFFF) or 0x88000000.toInt(), col and 0xFFFFFF, Shader.TileMode.CLAMP) })
        c.pet(pt, Pose.IDLE, W / 2f, cy + (if (look.story) 400 else 290), if (look.story) 820f else 560f)
        var y = cy + (if (look.story) 520 else 380)
        c.t(pt.name, W / 2f, y, 96f, bold, ink, Paint.Align.CENTER)
        c.t("${pt.sp().name} · ${pt.stage().label}" + (Form.of(pt.form)?.let { " · ${it.label}" } ?: "") + if (pt.shiny) tr(" · редкий окрас", " · rare") else "", W / 2f, y + 64, 38f, reg, sub, Paint.Align.CENTER)
        y += 110
        c.facts(listOf("${pt.workouts}" to tr("тренировок вместе", "workouts together"), fmt(pt.growth.toLong()) to tr("рост", "growth"), pt.nat().label to tr("характер", "nature")), y, 150f)
        c.t(pt.sp().ability, W / 2f, y + 200, 30f, reg, sub, Paint.Align.CENTER)
        c.footer()
        return b to "${pt.name} – ${pt.sp().name}"
    }

    private fun gymCard(): Pair<Bitmap, String> {
        val (b, c) = base(tr("мой зал", "my gym")); val st = stats(allTime)
        val gh = (W - 120f) / 1.5f
        c.gym(RectF(60f, top + 20, W - 60f, top + 20 + gh))
        var y = top + 70 + gh
        c.t(tr("Зал команды ${d.leader?.name ?: ""}", "${d.leader?.name ?: "The team"}'s gym"), 60f, y + 40, 64f, maxW = W - 120f)
        c.t(d.team.joinToString(" · ") { "${it.name} (${it.sp().name.lowercase()})" }, 60f, y + 100, 32f, reg, sub)
        y += 140
        c.facts(listOf("${st.count}" to tr("тренировок", "workouts"), "${(d.s.items - d.s.hidden).size}/${Shop.all.size}" to tr("вещей в зале", "things"),
            "${d.pets.count { it.hatched > 0 }}" to tr("питомцев", "pets")), y, 150f)
        c.footer()
        return b to tr("Мой зал в Pumpling", "My gym in Pumpling")
    }

    private fun bestiary(): Pair<Bitmap, String> {
        val (b, c) = base(tr("бестиарий", "bestiary"))
        val owned = d.pets.associateBy { it.species }
        c.t(tr("${owned.size} из ${Pets.all.size} найдено", "${owned.size} of ${Pets.all.size} found"), 60f, top + 70, 72f)
        c.t(tr("${d.pets.count { it.hatched > 0 }} вылупились · ${d.pets.count { it.stage() >= Stage.ADULT }} взрослых", "${d.pets.count { it.hatched > 0 }} hatched · ${d.pets.count { it.stage() >= Stage.ADULT }} grown"), 60f, top + 125, 34f, reg, sub)
        val cols = if (look.story) 4 else 5; val gap = 16f; val cw = (W - 120f - gap * (cols - 1)) / cols; val ch = if (look.story) cw * 1.05f else cw
        val sil = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, .35f, 0f))) }
        Pets.all.forEachIndexed { i, spc -> val x = 60f + (i % cols) * (cw + gap); val y = top + 170 + (i / cols) * (ch + gap)
            c.panel(x, y, cw, ch, 28f)
            val pt = owned[spc.id]
            if (pt != null) c.pet(pt, Pose.IDLE, x + cw / 2, y + ch - 14, min(cw, ch) * .95f, shadow = false)
            else Art.pet(ctx, spc.id, 2)?.asAndroidBitmap()?.let { img -> val s = min(cw * .7f / img.width, ch * .7f / img.height)
                c.drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(x + cw / 2 - img.width * s / 2, y + ch - 14 - img.height * s, x + cw / 2 + img.width * s / 2, y + ch - 14), sil) }
            if (pt?.shiny == true) c.t("★", x + cw - 34, y + 46, 34f, bold, accent) }
        c.footer()
        return b to tr("Мой бестиарий в Pumpling", "My Pumpling bestiary")
    }

    private fun exercise(exId: String): Pair<Bitmap, String> {
        val ex = Catalog.of(exId); val st = stats(allTime); val hist = st.history(exId)
        val (b, c) = base(tr("прогресс", "progress"))
        c.t(ex.name, 60f, top + 70, 70f, maxW = W - 120f)
        c.t(ex.primary.joinToString(", ") { it.label.lowercase() } + " · " + count(hist.size, "тренировка", "тренировки", "тренировок", "workout", "workouts"), 60f, top + 125, 34f, reg, sub)
        val weight = ex.kind == Kind.WEIGHT
        val pts = hist.map { (day, _, s) -> day.toDouble() to when (ex.kind) { Kind.WEIGHT -> e1rm(s.weight, s.reps); Kind.CARDIO -> s.meters / 1000; Kind.HOLD, Kind.MINUTES -> s.secs.toDouble(); else -> s.reps.toDouble() } }.filter { it.second > 0 }
        val first = pts.firstOrNull()?.second ?: 0.0; val last = pts.lastOrNull()?.second ?: 0.0; val best = pts.maxOfOrNull { it.second } ?: 0.0
        fun v(x: Double) = when (ex.kind) { Kind.WEIGHT -> hide(wu(Math.round(x).toDouble())); Kind.CARDIO -> km(x * 1000); Kind.HOLD, Kind.MINUTES -> clock(x.toInt()); else -> fmt(x.toLong()) }
        val growth = if (first > 0) ((last / first - 1) * 100).toInt() else 0
        c.panel(60f, top + 170, W - 120f, 460f)
        c.t(if (weight) tr("Расчётный максимум", "Estimated max") else tr("Лучшее за тренировку", "Best per workout"), 96f, top + 225, 30f, reg, sub)
        c.t(v(best), 96f, top + 300, 72f, num, ink)
        if (growth != 0) c.t((if (growth > 0) "+" else "−") + "${abs(growth)}%", W - 96f, top + 300, 64f, num, accent, Paint.Align.RIGHT)
        c.line(pts, 96f, top + 340, W - 192f, 250f)
        if (hist.isNotEmpty()) c.t("${fmtDayYear(hist.first().first)} – ${fmtDayYear(hist.last().first)}", W / 2f, top + 620 - 6, 26f, reg, sub, Paint.Align.CENTER)
        val all = st.allDone.filter { it.exId == exId && !it.warmup }
        val y = c.grid(listOf("${all.size}" to tr("подходов", "sets"), (if (weight) hide(tonnage(all.sumOf { st.tonnageOf(it) })) else fmt(all.sumOf { it.reps }.toLong())) to (if (weight) tr("тоннаж", "tonnage") else tr("повторений", "reps")),
            "${all.count { it.pr.isNotEmpty() }}" to tr("рекордов", "records"), (all.maxByOrNull { e1rm(it.weight, it.reps) + it.secs + it.meters + it.reps * .01 }?.let { if (look.hideWeights && weight) "× ${it.reps}" else setLine(ex, it) } ?: "–") to tr("лучший подход", "best set")), top + 660)
        c.roomPet(d.leader, Pose.WORK, y, 460f)
        c.footer()
        return b to tr("${ex.name}: мой прогресс", "${ex.name}: my progress")
    }

    private fun muscles(sp: Span): Pair<Bitmap, String> {
        val (b, c) = base(spanName(sp)); val st = stats(sp)
        val mx = (st.muscleSets.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        c.t(tr("Нагрузка на мышцы", "Muscles worked"), 60f, top + 70, 64f)
        c.t(tr("${fmt(st.setsN)} рабочих подходов", "${fmt(st.setsN)} working sets"), 60f, top + 125, 34f, reg, sub)
        val ph = if (look.story) 640f else 500f
        c.panel(60f, top + 170, W - 120f, ph)
        val bw = (ph - 40) * 230 / 204; c.body(st.muscleSets.mapValues { it.value / mx }, (W - bw) / 2, top + 190, bw)
        var y = top + 170 + ph + 60
        Muscle.entries.sortedByDescending { st.perWeek(it) }.take(if (look.story) 8 else 4).forEach { m -> val v = st.perWeek(m)
            c.t(m.label, 60f, y, 34f, bold, ink, maxW = 300f)
            c.drawRoundRect(RectF(380f, y - 22, W - 200f, y - 2), 10f, 10f, p(panel))
            c.drawRoundRect(RectF(380f, y - 22, 380f + (W - 580f) * (v / 20).toFloat().coerceIn(.02f, 1f), y - 2), 10f, 10f, p(accent))
            c.t(tr("${fmt1(v)} в нед.", "${fmt1(v)}/wk"), W - 60f, y, 30f, num, sub, Paint.Align.RIGHT); y += 60 }
        c.footer()
        return b to tr("Мышцы: ${sp.label()}", "Muscles: ${sp.label()}")
    }

    private fun records(sp: Span): Pair<Bitmap, String> {
        val (b, c) = base(spanName(sp)); val st = stats(sp); val recs = st.recordSets
        c.t(count(recs.size, "рекорд", "рекорда", "рекордов", "record", "records"), 60f, top + 70, 84f)
        c.t(tr("за ${if (sp.kind == Span.MONTH) sp.label().lowercase() else sp.label()}", "in ${sp.label()}"), 60f, top + 125, 34f, reg, sub)
        var y = top + 190; val rows = if (look.story) 13 else 8
        val list = recs.distinctBy { it.exId }.take(rows)
        c.panel(60f, y - 20, W - 120f, max(1, list.size) * 92f + 30)
        y += 50
        list.forEach { s -> val ex = Catalog.of(s.exId); val r = s.pr.split(',').mapNotNull { Rec.of(it) }.first()
            c.t("★", 96f, y, 40f, bold, accent)
            c.t(ex.name, 150f, y - 8, 36f, bold, ink, maxW = 520f); c.t(r.label + " · " + fmtDayYear(st.dayOfSet(s)), 150f, y + 26, 26f, reg, sub, maxW = 520f)
            c.t(if (look.hideWeights && (r == Rec.WEIGHT || r == Rec.E1RM)) "↑" else Records.show(ex, r, s).substringBefore(" ("), W - 96f, y, 34f, num, ink, Paint.Align.RIGHT); y += 92 }
        if (list.isEmpty()) c.t(tr("Скоро будут!", "Coming soon!"), W / 2f, y, 40f, bold, sub, Paint.Align.CENTER)
        c.roomPet(d.leader, Pose.CHEER, y, 420f)
        c.footer()
        return b to tr("Мои рекорды: ${sp.label()}", "My records: ${sp.label()}")
    }

    private fun cardio(sp: Span): Pair<Bitmap, String> {
        val (b, c) = base(spanName(sp)); val st = stats(sp)
        c.t(km(st.meters), 60f, top + 90, 110f, num)
        c.t(tr("кардио · ${hours(st.cardioSecs.toLong())}", "cardio · ${hours(st.cardioSecs.toLong())}"), 60f, top + 150, 36f, reg, sub)
        var y = top + 200
        st.journey()?.let { (to, f) -> c.panel(60f, y, W - 120f, 150f)
            c.t(if (f >= 1) tr("Пройдено: $to", "Done: $to") else tr("${(f * 100).toInt()}% пути: $to", "${(f * 100).toInt()}% of $to"), 96f, y + 60, 32f, bold, ink, maxW = W - 192f)
            c.drawRoundRect(RectF(96f, y + 90, W - 96f, y + 112), 11f, 11f, p(panel or 0x22000000)); c.drawRoundRect(RectF(96f, y + 90, 96f + (W - 192f) * f.coerceIn(.02f, 1f), y + 112), 11f, 11f, p(accent))
            y += 180 }
        val lines = st.cardio.take(4).map { it.ex.name to listOfNotNull(if (it.meters > 0) km(it.meters) else null, dur(it.secs)).joinToString(" · ") } +
            st.bestEfforts().filter { it.second != null }.take(3).map { (l, s) -> tr("Лучшее: $l", "Best: $l") to clock(s!!.secs) }
        lines.forEach { (a, v) -> c.t(a, 60f, y + 40, 36f, bold, ink, maxW = 600f); c.t(v, W - 60f, y + 40, 34f, num, sub, Paint.Align.RIGHT); y += 66 }
        c.roomPet(d.pets.firstOrNull { it.species == "otter" && it.hatched > 0 } ?: d.leader, Pose.WORK, y, 480f)
        c.footer()
        return b to tr("Кардио: ${sp.label()}", "Cardio: ${sp.label()}")
    }

    private fun lifts(sp: Span): Pair<Bitmap, String> {
        val (b, c) = base(spanName(sp)); val st = stats(sp)
        c.t(tr("Мои упражнения", "My lifts"), 60f, top + 70, 72f)
        c.t(tr("${st.exercises.size} упражнений · ${fmt(st.setsN)} подходов", "${st.exercises.size} exercises · ${fmt(st.setsN)} sets"), 60f, top + 125, 34f, reg, sub)
        var y = top + 200; val rows = if (look.story) 12 else 8
        val list = st.exercises.take(rows)
        c.panel(60f, y - 30, W - 120f, list.size * 84f + 40); y += 36
        list.forEachIndexed { i, e ->
            c.t("${i + 1}", 100f, y, 36f, num, sub, Paint.Align.CENTER)
            c.t(e.ex.name, 150f, y, 36f, bold, ink, maxW = 560f)
            val g = (e.growth * 100).toInt()
            c.t(if (g != 0) (if (g > 0) "+$g%" else "−${abs(g)}%") else tr("${e.sets} подх.", "${e.sets} sets"), W - 96f, y, 34f, num, if (g > 0) accent else sub, Paint.Align.RIGHT); y += 84 }
        c.roomPet(d.leader, Pose.WORK, y, 420f)
        c.footer()
        return b to tr("Мои упражнения: ${sp.label()}", "My lifts: ${sp.label()}")
    }

}

object Share {
    fun send(ctx: Context, bmp: Bitmap, title: String) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }; val f = File(dir, "pumpling.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, tr("Поделиться открыткой", "Share the card")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves the card to Pictures/Pumpling. */
    fun save(ctx: Context, bmp: Bitmap): Boolean = runCatching {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "pumpling-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pumpling") }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; true }.getOrDefault(false)
}

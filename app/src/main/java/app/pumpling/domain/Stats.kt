package app.pumpling.domain

import app.pumpling.data.Body
import app.pumpling.data.Settings
import app.pumpling.data.WSet
import app.pumpling.data.Workout
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.pow

/** A stretch of days to count over, [from]..[to] inclusive. */
data class Span(val from: Long, val to: Long, val kind: Int) {
    val days get() = (to - from + 1).toInt()
    /** The same length right before, to compare with. */
    /** The span before, cut to as many days as have passed in this one when it is still going. */
    fun prev(): Span { val p = prevFull(); val t = today(); return if (t in from..to) p.copy(to = minOf(p.to, p.from + (t - from))) else p }
    private fun prevFull() = when (kind) {
        MONTH -> LocalDate.ofEpochDay(from).minusMonths(1).let { Span(it.toEpochDay(), it.plusMonths(1).toEpochDay() - 1, kind) }
        YEAR -> LocalDate.ofEpochDay(from).minusYears(1).let { Span(it.toEpochDay(), it.plusYears(1).toEpochDay() - 1, kind) }
        else -> Span(from - days, from - 1, kind)
    }
    fun label(): String = when (kind) {
        WEEK -> "${fmtDayShort(from)} – ${fmtDayShort(to)}"
        MONTH -> LocalDate.ofEpochDay(from).let { "${monthName(it.monthValue).cap()} ${it.year}" }
        QUARTER -> LocalDate.ofEpochDay(from).let { "Q${(it.monthValue - 1) / 3 + 1} ${it.year}" }
        YEAR -> LocalDate.ofEpochDay(from).year.toString()
        else -> tr("Всё время", "All time")
    }
    companion object {
        const val WEEK = 0; const val MONTH = 1; const val QUARTER = 2; const val YEAR = 3; const val ALL = 4
        fun of(kind: Int, back: Int = 0, first: Long = 0L): Span {
            val t = LocalDate.now()
            return when (kind) {
                WEEK -> { val m = weekStart(today()) - 7L * back; Span(m, m + 6, kind) }
                MONTH -> t.withDayOfMonth(1).minusMonths(back.toLong()).let { Span(it.toEpochDay(), it.plusMonths(1).toEpochDay() - 1, kind) }
                QUARTER -> t.withDayOfMonth(1).withMonth((t.monthValue - 1) / 3 * 3 + 1).minusMonths(3L * back).let { Span(it.toEpochDay(), it.plusMonths(3).toEpochDay() - 1, kind) }
                YEAR -> t.withDayOfYear(1).minusYears(back.toLong()).let { Span(it.toEpochDay(), it.plusYears(1).toEpochDay() - 1, kind) }
                else -> Span(minOf(first, today()), today(), kind)
            }
        }
    }
}

/** Everything the statistics show for one span. */
class Stats(allWorkouts: List<Workout>, allSets: List<WSet>, val bodies: List<Body>, val s: Settings, val span: Span) {
    val finished = allWorkouts.filter { it.end > 0 }
    private val finishedIds = finished.map { it.id }.toSet()
    val allDone = allSets.filter { it.done && it.workoutId in finishedIds }
    val workouts = finished.filter { dayOf(it.start) in span.from..span.to }.sortedBy { it.start }
    private val ids = workouts.map { it.id }.toSet()
    val sets = allDone.filter { it.workoutId in ids }
    val work = sets.filter { !it.warmup }
    val bw: Double get() = bodies.firstOrNull { it.weight > 0 }?.weight ?: s.bodyweight.takeIf { it > 0 } ?: 70.0
    private val wById = finished.associateBy { it.id }
    fun dayOfSet(x: WSet) = wById[x.workoutId]?.let { dayOf(it.start) } ?: 0L

    // ---------- totals ----------
    val count get() = workouts.size
    val secs: Long get() = workouts.sumOf { max(0L, (it.end - it.start) / 1000) }
    val setsN get() = work.size
    val reps get() = work.sumOf { it.reps }
    fun tonnageOf(x: WSet): Double { val ex = Catalog.of(x.exId)
        return when (ex.kind) { Kind.WEIGHT -> x.weight * x.reps; Kind.BODY -> (bw * .7 + x.weight) * x.reps; Kind.ASSIST -> max(0.0, bw * .7 - x.weight) * x.reps; else -> 0.0 } }
    val tonnage: Double by lazy { work.sumOf { tonnageOf(it) } }
    val meters: Double get() = work.filter { Catalog.of(it.exId).kind == Kind.CARDIO }.sumOf { it.meters }
    val cardioSecs: Int get() = work.filter { Catalog.of(it.exId).cat == Cat.CARDIO }.sumOf { it.secs }
    /** A rough estimate from MET values. */
    val kcal: Double by lazy {
        workouts.sumOf { w ->
            val l = work.filter { it.workoutId == w.id }
            val timed = l.filter { Catalog.of(it.exId).kind.timed }
            val timedSecs = timed.sumOf { it.secs }
            val timedKcal = timed.sumOf { x -> met(Catalog.of(x.exId)) * bw * x.secs / 3600.0 }
            val rest = max(0L, (w.end - w.start) / 1000 - timedSecs)
            timedKcal + (if (l.any { !Catalog.of(it.exId).kind.timed }) 5.0 * bw * rest / 3600.0 else 0.0)
        }
    }
    private fun met(e: Ex) = when (e.cat) { Cat.CARDIO -> if (e.id == "walk" || e.id == "nordic") 4.0 else if (e.id == "hike" || e.id == "incline_walk") 6.0 else 8.5; Cat.HIIT -> 9.0; Cat.JUMP -> 9.0; Cat.FLEX -> 2.8; Cat.BAL -> 3.0; Cat.CORE -> 4.0; else -> 5.0 }
    val activeDays get() = workouts.map { dayOf(it.start) }.toSet().size
    val avgSecs get() = if (count == 0) 0L else secs / count
    /** Kilograms a minute of workout. */
    val density get() = if (secs == 0L) 0.0 else tonnage / (secs / 60.0)
    val avgRest: Int by lazy {
        val gaps = work.groupBy { it.workoutId }.values.flatMap { l -> l.filter { it.doneAt > 0 }.sortedBy { it.doneAt }.zipWithNext { a, b -> ((b.doneAt - a.doneAt) / 1000).toInt() } }.filter { it in 15..900 }
        if (gaps.isEmpty()) 0 else gaps.average().toInt()
    }
    val prs get() = work.count { it.pr.isNotEmpty() }
    val food get() = workouts.sumOf { it.food }
    val plates get() = workouts.sumOf { it.plates }

    // ---------- time ----------
    /** day → working sets that day (for the heat map). */
    val heat: Map<Long, Int> by lazy { val m = HashMap<Long, Int>(); for (x in allDone) if (!x.warmup) { val d = dayOfSet(x); m[d] = (m[d] ?: 0) + 1 }
        for (w in finished) { val d = dayOf(w.start); if (d !in m) m[d] = 1 }; m }
    val weekdays: IntArray get() = IntArray(7).also { a -> workouts.forEach { a[LocalDate.ofEpochDay(dayOf(it.start)).dayOfWeek.value - 1]++ } }
    /** Workouts by start hour, 24 buckets. */
    val hours: IntArray get() = IntArray(24).also { a -> workouts.forEach { a[hourOf(it.start)]++ } }
    /** Records by start hour of their workout: when you're strongest. */
    val prHours: IntArray get() = IntArray(24).also { a -> work.filter { it.pr.isNotEmpty() }.forEach { x -> wById[x.workoutId]?.let { a[hourOf(it.start)]++ } } }
    val partOfDay: List<Pair<String, Int>> get() { val h = hours
        return listOf(tr("Утро", "Morning") to (5..11).sumOf { h[it] }, tr("День", "Afternoon") to (12..16).sumOf { h[it] },
            tr("Вечер", "Evening") to (17..21).sumOf { h[it] }, tr("Ночь", "Night") to ((22..23) + (0..4)).sumOf { h[it] }) }
    val weeks = Weeks(finished.map { dayOf(it.start) }.toSet(), s.weekGoal, s.sickDays)

    /** Values per bucket across the span: weeks for a month or quarter, months for a year, days for a week. */
    fun buckets(f: (List<Workout>, List<WSet>) -> Double): List<Pair<String, Double>> {
        val from = span.from; val to = minOf(span.to, max(span.from, today()))
        return when (span.kind) {
            Span.WEEK -> (0..6).map { i -> val d = from + i; dayShort(i) to f(workouts.filter { dayOf(it.start) == d }, work.filter { dayOfSet(it) == d }) }
            Span.MONTH, Span.QUARTER -> { val out = ArrayList<Pair<String, Double>>(); var m = weekStart(from)
                while (m <= to) { val a = m; val b = m + 6; out += fmtDayShort(maxOf(a, from)) to f(workouts.filter { dayOf(it.start) in a..b }, work.filter { dayOfSet(it) in a..b }); m += 7 }; out }
            Span.YEAR -> (1..12).map { mo -> val a = LocalDate.ofEpochDay(from).withMonth(mo).toEpochDay(); val b = LocalDate.ofEpochDay(a).plusMonths(1).toEpochDay() - 1
                monthShort(mo) to f(workouts.filter { dayOf(it.start) in a..b }, work.filter { dayOfSet(it) in a..b }) }
            else -> { val first = finished.minOfOrNull { dayOf(it.start) } ?: today(); val y0 = LocalDate.ofEpochDay(first).year; val y1 = LocalDate.now().year
                (y0..y1).flatMap { y -> (1..12).mapNotNull { mo -> val a = LocalDate.of(y, mo, 1).toEpochDay(); if (a < weekStart(first) - 31 || a > today()) null else {
                    val b = LocalDate.ofEpochDay(a).plusMonths(1).toEpochDay() - 1
                    (if (mo == 1 || a <= first) "${monthShort(mo)} ${y % 100}" else monthShort(mo)) to f(workouts.filter { dayOf(it.start) in a..b }, work.filter { dayOfSet(it) in a..b }) } } } }
        }
    }

    // ---------- exercises ----------
    class ExLine(val ex: Ex, val sets: Int, val workouts: Int, val tonnage: Double, val best: Double, val growth: Double, val secs: Int, val meters: Double)
    val exercises: List<ExLine> by lazy {
        work.groupBy { it.exId }.map { (id, l) -> val ex = Catalog.of(id)
            val byW = l.groupBy { it.workoutId }.toSortedMap(compareBy { wById[it]?.start ?: 0 })
            val e = byW.values.map { ws -> ws.maxOf { e1rm(it.weight, it.reps) } }.filter { it > 0 }
            ExLine(ex, l.size, byW.size, l.sumOf { tonnageOf(it) }, e.maxOrNull() ?: 0.0, if (e.size >= 2 && e.first() > 0) e.last() / e.first() - 1 else 0.0,
                l.sumOf { it.secs }, l.sumOf { it.meters })
        }.sortedByDescending { it.sets }
    }
    val newExercises: List<Ex> by lazy { val before = allDone.filter { dayOfSet(it) < span.from }.map { it.exId }.toSet()
        work.map { it.exId }.distinct().filter { it !in before }.map { Catalog.of(it) } }
    /** Exercise most done in each month of the span. */
    fun favouriteByMonth(): List<Pair<String, Ex>> = work.groupBy { LocalDate.ofEpochDay(dayOfSet(it)).let { d -> d.year * 100 + d.monthValue } }.toSortedMap()
        .map { (k, l) -> monthName(k % 100).cap() to Catalog.of(l.groupingBy { it.exId }.eachCount().maxBy { it.value }.key) }

    // ---------- muscles ----------
    /** Working sets per muscle: primary counts one, secondary a half. Only sets with weight or reps or holds. */
    val muscleSets: Map<Muscle, Double> by lazy {
        val m = HashMap<Muscle, Double>()
        for (x in work) { val ex = Catalog.of(x.exId); if (ex.kind == Kind.CARDIO || ex.kind == Kind.MINUTES) continue
            ex.primary.forEach { m[it] = (m[it] ?: 0.0) + 1 }; ex.secondary.forEach { m[it] = (m[it] ?: 0.0) + .5 } }
        m
    }
    val weeksInSpan: Double get() = max(1.0, (minOf(span.to, today()) - max(span.from, finished.minOfOrNull { dayOf(it.start) } ?: span.from) + 1) / 7.0)
    fun perWeek(m: Muscle) = (muscleSets[m] ?: 0.0) / weeksInSpan
    val pushPull: Pair<Double, Double> get() = muscleSets.entries.fold(0.0 to 0.0) { (a, b), (m, v) -> when (m.push) { true -> a + v to b; false -> a to b + v; null -> a to b } }
    val upperLower: Pair<Double, Double> get() = muscleSets.entries.fold(0.0 to 0.0) { (a, b), (m, v) -> if (m.upper) a + v to b else a to b + v }
    val frontBack: Pair<Double, Double> get() = muscleSets.entries.fold(0.0 to 0.0) { (a, b), (m, v) -> if (m.front) a + v to b else a to b + v }
    val equipment: List<Pair<String, Int>> get() = work.groupingBy { Catalog.of(it.exId).equipLabel }.eachCount().toList().sortedByDescending { it.second }
    val families: List<Pair<Cat, Int>> get() = work.groupingBy { Catalog.of(it.exId).cat }.eachCount().toList().sortedByDescending { it.second }
    /** Muscles sorted by how rested they are: hours since last worked. */
    fun recovery(now: Long = System.currentTimeMillis()): Map<Muscle, Double> {
        val last = HashMap<Muscle, Long>(); val load = HashMap<Muscle, Double>()
        for (x in allDone) { if (x.warmup) continue; val ex = Catalog.of(x.exId); val t = wById[x.workoutId]?.end ?: continue
            if (now - t > 4 * 86400_000L) continue
            val f = ((4 * 86400_000.0 - (now - t)) / (4 * 86400_000.0)).coerceIn(0.0, 1.0)
            ex.primary.forEach { load[it] = (load[it] ?: 0.0) + f }; ex.secondary.forEach { load[it] = (load[it] ?: 0.0) + f * .5 }
            ex.muscles.forEach { last[it] = max(last[it] ?: 0, t) } }
        // fatigue 0..1: about 8 recent sets on a muscle tire it fully
        return Muscle.entries.associateWith { ((load[it] ?: 0.0) / 8).coerceIn(0.0, 1.0) }
    }

    // ---------- cardio ----------
    class CardioLine(val ex: Ex, val n: Int, val meters: Double, val secs: Int, val bestPace: Double, val longest: Double)
    val cardio: List<CardioLine> by lazy {
        work.filter { Catalog.of(it.exId).kind == Kind.CARDIO || (Catalog.of(it.exId).cat == Cat.CARDIO) }.groupBy { it.exId }.map { (id, l) ->
            val paced = l.filter { it.meters >= 1000 && it.secs > 0 }
            CardioLine(Catalog.of(id), l.size, l.sumOf { it.meters }, l.sumOf { it.secs }, paced.minOfOrNull { it.secs / (it.meters / 1000) } ?: Double.NaN, l.maxOf { it.meters })
        }.sortedByDescending { it.secs }
    }
    /** Fastest efforts at classic distances (an effort counts for a distance it covered). */
    fun bestEfforts(ids: Set<String> = setOf("run", "treadmill", "trail_run")): List<Pair<String, WSet?>> {
        val runs = allDone.filter { it.exId in ids && it.meters > 0 && it.secs > 0 }
        return listOf(1000.0 to tr("1 км", "1 km"), 5000.0 to tr("5 км", "5 km"), 10000.0 to tr("10 км", "10 km"), 21097.0 to tr("Полумарафон", "Half marathon"), 42195.0 to tr("Марафон", "Marathon"))
            .map { (d, l) -> l to runs.filter { it.meters >= d * .98 && it.meters <= d * 1.15 }.minByOrNull { it.secs / it.meters } }
    }
    /** Your kilometres laid over real distances. */
    fun journey(): Pair<String, Float>? {
        val km = meters / 1000; if (km <= 0) return null
        val trips = listOf(10.0 to tr("через Москву с севера на юг", "across Paris and back"), 42.2 to tr("марафон", "a marathon"), 120.0 to tr("от Москвы до Сергиева Посада и обратно", "London to Oxford and back"),
            330.0 to tr("от Москвы до Твери и обратно", "London to Paris"), 700.0 to tr("от Москвы до Петербурга", "Berlin to Paris by road"), 1500.0 to tr("от Москвы до Берлина", "London to Rome"),
            3500.0 to tr("через всю Европу", "across Europe"), 9300.0 to tr("по Транссибу до Владивостока", "the Trans-Siberian to Vladivostok"), 40075.0 to tr("вокруг Земли", "around the Earth"))
        val next = trips.firstOrNull { it.first > km } ?: trips.last()
        return next.second to (km / next.first).toFloat()
    }

    // ---------- records and strength ----------
    val recordSets: List<WSet> get() = work.filter { it.pr.isNotEmpty() }.sortedByDescending { dayOfSet(it) }
    val bests: Map<String, Best> by lazy { Records.bests(allDone) }
    /** Heaviest weight for each number of reps, 1..12, for one exercise, with the set it was done in. */
    fun repTable(exId: String): List<Pair<Int, WSet?>> { val l = allDone.filter { it.exId == exId && !it.warmup && it.weight > 0 }
        return (1..12).map { r -> r to l.filter { it.reps >= r }.maxByOrNull { it.weight } } }
    /** e1RM, best weight, volume per workout for one exercise. */
    fun history(exId: String): List<Triple<Long, Double, WSet>> = allDone.filter { it.exId == exId && !it.warmup }.groupBy { it.workoutId }
        .mapNotNull { (w, l) -> val d = wById[w]?.let { dayOf(it.start) } ?: return@mapNotNull null
            val best = l.maxBy { e1rm(it.weight, it.reps).takeIf { v -> v > 0 } ?: (it.weight * 1000 + it.reps + it.secs + it.meters) }
            Triple(d, l.sumOf { tonnageOf(it) + it.secs + it.meters }, best) }.sortedBy { it.first }

    class Standard(val ex: Ex, val e1rm: Double, val ratio: Double, val level: Int, val next: Double?)
    val levelNames get() = listOf(tr("Новичок", "Beginner"), tr("Начинающий", "Novice"), tr("Любитель", "Intermediate"), tr("Продвинутый", "Advanced"), tr("Элита", "Elite"))
    /** Lifts against body-weight standards (rough, for adults; women's tables are about two thirds). */
    fun standards(): List<Standard> {
        val k = if (s.sex == "f") .66 else 1.0
        val table = listOf("squat" to doubleArrayOf(.75, 1.25, 1.5, 2.25, 2.75), "bench" to doubleArrayOf(.5, .75, 1.0, 1.5, 2.0),
            "deadlift" to doubleArrayOf(1.0, 1.5, 2.0, 2.5, 3.0), "ohp" to doubleArrayOf(.35, .55, .75, 1.0, 1.25), "row" to doubleArrayOf(.5, .75, 1.0, 1.5, 1.75))
        return table.mapNotNull { (id, t) -> val b = bests[id] ?: return@mapNotNull null; if (b.e1rm <= 0) return@mapNotNull null
            val r = b.e1rm / bw; val lvl = t.indexOfLast { r >= it * k }
            Standard(Catalog.of(id), b.e1rm, r, lvl, t.getOrNull(lvl + 1)?.let { it * k * bw }) }
    }
    /** Squat + bench + deadlift by estimated maxes, and the DOTS points for it. */
    fun total(): Pair<Double, Double>? {
        val sq = bests["squat"]?.e1rm ?: 0.0; val bp = bests["bench"]?.e1rm ?: 0.0; val dl = bests["deadlift"]?.e1rm ?: 0.0
        if (sq <= 0 || bp <= 0 || dl <= 0) return null
        val t = sq + bp + dl; val x = bw
        val c = if (s.sex == "f") doubleArrayOf(-57.96288, 13.6175032, -0.1126655495, 0.0005158568, -0.0000010706)
                else doubleArrayOf(-307.75076, 24.0900756, -0.1918759221, 0.0007391293, -0.000001093)
        val den = c[0] + c[1] * x + c[2] * x.pow(2) + c[3] * x.pow(3) + c[4] * x.pow(4)
        return t to t * 500 / den
    }

    // ---------- fun ----------
    fun funFacts(): List<String> {
        val out = ArrayList<String>(); val t = tonnage
        if (t >= 1000) {
            val things = listOf(5_000.0 to listOf("слона", "слона", "слонов", "elephant", "elephants"), 150_000.0 to listOf("синего кита", "синих кита", "синих китов", "blue whale", "blue whales"),
                1_500.0 to listOf("легковушки", "легковушки", "легковушек", "car", "cars"), 400_000.0 to listOf("взлетающего «Боинга-747»", "«Боинга-747»", "«Боингов-747»", "jumbo jet", "jumbo jets"))
            val (kg, n) = things.filter { t >= it.first }.maxBy { it.first }
            val k = t / kg
            out += tr("Поднято столько, сколько весят ${fmt1(k)} ${if (k < 2 || fmt1(k).contains(',')) n[0] else plural(k.toLong(), n[0], n[1], n[2])}", "You lifted ${fmt1(k)} ${if (k < 2) n[3] else n[4]}")
        }
        val pushups = work.filter { it.exId.contains("pushup") }.sumOf { it.reps }
        if (pushups >= 10) out += tr("${fmt(pushups)} отжиманий – это подъём груза твоего веса на ${fmt((pushups * .35).toLong())} м", "${fmt(pushups)} push-ups lift your weight ${fmt((pushups * .35).toLong())} m")
        val squats = work.filter { it.exId.contains("squat") }.sumOf { it.reps }
        if (squats >= 20) out += tr("${fmt(squats)} приседаний – как ${fmt(squats / 20)} этажей по лестнице", "${fmt(squats)} squats, like climbing ${fmt(squats / 20)} floors")
        journey()?.let { (to, f) -> out += if (f >= 1) tr("Пройдено и пробежано ${km(meters)}: это $to", "You've covered ${km(meters)}: that's $to") else tr("${km(meters)} – ${(f * 100).toInt()}% пути $to", "${km(meters)} – ${(f * 100).toInt()}% of $to") }
        if (secs > 0) out += tr("${hours(secs)} в зале – это ${fmt1(secs / 3600.0 / 1.5)} фильмов", "${hours(secs)} training – ${fmt1(secs / 3600.0 / 1.5)} films' worth")
        if (kcal > 500) out += tr("≈ ${fmt(kcal.toLong())} ккал – это ${fmt((kcal / 280).toLong())} пельменных порций", "≈ ${fmt(kcal.toLong())} kcal – ${fmt((kcal / 280).toLong())} plates of dumplings")
        return out
    }
    val heaviestWorkout get() = workouts.maxByOrNull { w -> work.filter { it.workoutId == w.id }.sumOf { tonnageOf(it) } }
    val longestWorkout get() = workouts.maxByOrNull { it.end - it.start }
    val earliestWorkout get() = workouts.minByOrNull { java.time.Instant.ofEpochMilli(it.start).atZone(zone).toLocalTime().let { t -> ((t.hour + 20) % 24) * 60 + t.minute } }

    // ---------- body ----------
    /** Weight with a smoothed trend (exponential moving average, about a week). */
    fun weightTrend(): List<Triple<Long, Double, Double>> {
        val l = bodies.filter { it.weight > 0 }.sortedBy { it.day }; var ema = l.firstOrNull()?.weight ?: 0.0
        return l.map { b -> ema += (b.weight - ema) * .25; Triple(b.day, b.weight, ema) }
    }
}

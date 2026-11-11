package app.pumpling.domain

import app.pumpling.data.Pet
import app.pumpling.data.Settings
import app.pumpling.data.WSet
import app.pumpling.data.Workout
import kotlin.math.max
import kotlin.math.min

/** Sums over finished workouts that decide which eggs are found and how the week goes. */
class Tally(val workouts: List<Workout>, val sets: List<WSet>, val s: Settings) {
    val done = sets.filter { it.done }
    val work = done.filter { !it.warmup }
    private val byW = workouts.associateBy { it.id }
    val catSets = work.groupingBy { Catalog.of(it.exId).cat }.eachCount()
    val catSecs = work.groupBy { Catalog.of(it.exId).cat }.mapValues { e -> e.value.sumOf { it.secs } }
    val pullupReps = work.filter { it.exId in PULLUPS }.sumOf { it.reps }
    val holdSecs = work.filter { Catalog.of(it.exId).kind == Kind.HOLD }.sumOf { it.secs }
    val cardioSecs = work.filter { Catalog.of(it.exId).kind == Kind.CARDIO }.sumOf { it.secs }
    val longestCardio = work.filter { Catalog.of(it.exId).cat == Cat.CARDIO }.maxOfOrNull { it.secs } ?: 0
    private val perW = work.groupBy { it.workoutId }
    fun workoutsWith(c: Cat) = perW.count { (_, l) -> l.any { Catalog.of(it.exId).cat == c } }
    val maxDistinct = perW.values.maxOfOrNull { l -> l.map { it.exId }.distinct().size } ?: 0
    val prs = workouts.sumOf { it.prs }
    val bw get() = if (s.bodyweight > 0) s.bodyweight else 70.0
    val liftedBw = work.any { Catalog.of(it.exId).kind == Kind.WEIGHT && it.weight >= bw && it.reps >= 1 }
    val early = workouts.count { val t = java.time.Instant.ofEpochMilli(it.start).atZone(zone).toLocalTime(); t.hour < 7 || (t.hour == 7 && t.minute < 30) }
    val late = workouts.count { hourOf(it.start) >= 21 }
    val tonnage = work.sumOf { tonnageOf(it) }
    val cats = work.map { Catalog.of(it.exId).cat }.toSet()
    val days = workouts.map { dayOf(it.start) }.toSortedSet()
    /** The longest pause between two workouts, in days. */
    val maxGap = days.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0
    fun slowRestWorkouts(): Int = perW.count { (_, l) ->
        val t = l.filter { it.doneAt > 0 }.sortedBy { it.doneAt }.zipWithNext { a, b -> (b.doneAt - a.doneAt) / 1000 }.filter { it in 20..900 }
        t.size >= 4 && t.average() >= 150 }
    val weeks = Weeks(days, s.weekGoal, s.sickDays)

    fun tonnageOf(x: WSet): Double {
        val ex = Catalog.of(x.exId)
        return when (ex.kind) { Kind.WEIGHT -> x.weight * x.reps; Kind.BODY -> (bw * .7 + x.weight) * x.reps; Kind.ASSIST -> max(0.0, bw * .7 - x.weight) * x.reps; else -> 0.0 }
    }
    fun workoutOf(x: WSet) = byW[x.workoutId]

    companion object { val PULLUPS = setOf("pullup", "chinup", "negative_pullup", "assisted_pullup") }
}

/** Weeks and the weekly goal: which weeks met it, the streak now and the best one. */
class Weeks(days: Set<Long>, val goal: Int, sick: Set<Long>) {
    val count: Map<Long, Int> = days.groupingBy { weekStart(it) }.eachCount()
    private val sickWeeks = sick.map { weekStart(it) }.toSet()
    fun met(week: Long) = (count[week] ?: 0) >= goal
    /** Weeks in a row the goal was met, counting this week only once it's met. Sick weeks don't break it. */
    val streak: Int = run {
        var w = weekStart(today()); var n = 0
        if (met(w)) n++
        w -= 7
        while (met(w) || (w in sickWeeks && count.isNotEmpty() && w > count.keys.min())) { if (met(w)) n++; w -= 7 }
        n
    }
    val best: Int = run {
        if (count.isEmpty()) return@run 0
        var w = count.keys.min(); val end = weekStart(today()); var run = 0; var best = 0
        while (w <= end) { if (met(w)) { run++; best = max(best, run) } else if (w !in sickWeeks && w != end) run = 0; w += 7 }
        best
    }
}

/** What a finished workout gave. */
data class Outcome(
    val food: Double, val plates: Int, val records: List<Pair<WSet, Rec>>, val eggs: List<String>, val hatched: List<Pet>,
    val quests: List<Quest>, val newEx: List<String>, val shares: Map<Long, Double>, val bonus: List<String>,
)

object Engine {
    /** Pet ids leading the team, then the two companions. */
    fun team(pets: List<Pet>) = pets.filter { it.slot in 0..2 }.sortedBy { it.slot }

    /**
     * Feeds the team for workout [w] with [sets] (its done sets, records already marked).
     * [before] are the workouts and sets finished before it. Returns the outcome and the pets as they are now.
     */
    fun feed(w: Workout, sets: List<WSet>, before: List<Workout>, beforeSets: List<WSet>, pets: List<Pet>, s: Settings): Pair<Outcome, List<Pet>> {
        val team = team(pets)
        val species = team.map { it.species }.toSet()
        val work = sets.filter { it.done }
        val bw = if (s.bodyweight > 0) s.bodyweight else 70.0
        val hour = hourOf(w.start)
        val lastDay = before.maxOfOrNull { dayOf(it.start) }
        val gap = if (lastDay == null) 0L else dayOf(w.start) - lastDay
        val distinct = work.map { it.exId }.distinct().size
        val weeks = Weeks(before.map { dayOf(it.start) }.toSet(), s.weekGoal, s.sickDays)
        val bonus = ArrayList<String>()
        var teamExtra = 0.0
        if ("squirrel" in species && distinct >= 5) { teamExtra += .3; bonus += tr("Белка: ${distinct} упражнений, ×1,3", "Squirrel: $distinct exercises, ×1.3") }
        if ("mole" in species && hour < 8) { teamExtra += .5; bonus += tr("Крот: утренняя тренировка, ×1,5", "Mole: a morning workout, ×1.5") }
        if ("owl" in species && hour >= 20) { teamExtra += .5; bonus += tr("Сова: вечерняя тренировка, ×1,5", "Owl: an evening workout, ×1.5") }
        if ("hamster" in species && weeks.streak > 0) { val b = min(.5, .1 * weeks.streak); teamExtra += b; bonus += tr("Хомяк: ${weeks.streak} нед. подряд, +${(b * 100).toInt()}%", "Hamster: ${weeks.streak} weeks in a row, +${(b * 100).toInt()}%") }
        if ("phoenix" in species && gap >= 10) { teamExtra += 2.0; bonus += tr("Феникс: возвращение после $gap дней, ×3", "Phoenix: back after $gap days, ×3") }
        if ("dragon" in species) teamExtra += .3

        val ordered = work.sortedBy { it.doneAt }
        val restBefore = HashMap<Long, Long>()
        ordered.zipWithNext { a, b -> if (a.doneAt > 0 && b.doneAt > 0) restBefore[b.id] = (b.doneAt - a.doneAt) / 1000 }

        // food per set and family, before each pet's own share and nature
        data class Bite(val set: WSet, val cat: Cat, val food: Double, val hard: Boolean)
        val bites = work.map { x ->
            val ex = Catalog.of(x.exId)
            var f = when (ex.kind) {
                Kind.WEIGHT, Kind.BODY, Kind.ASSIST -> if (x.warmup) .3 else 1.0
                Kind.HOLD -> min(3.0, max(.5, x.secs / 40.0)) * (if (x.weight > 0) 1.2 else 1.0)
                Kind.CARDIO -> x.secs / 60.0 / 4
                Kind.MINUTES -> x.secs / 60.0 / 5
            }
            if (x.rpe >= 8 && !x.warmup) f *= 1.15
            val pr = x.pr.isNotEmpty()
            if (pr) f *= 1.5
            var m = 1.0 + teamExtra
            for (p in team) { val sp = p.sp(); if (sp.likes == ex.cat) m += .5 }
            if ("turtle" in species && ex.cat == Cat.CARDIO && x.secs >= 2700) m += 1.0
            if ("sloth" in species && (restBefore[x.id] ?: 0) >= 150) m += .5
            if ("gorilla" in species && pr) m += 1.5
            if ("ant" in species && ex.kind == Kind.WEIGHT && x.weight >= bw && !x.warmup) m += 1.0
            Bite(x, ex.cat, f * m, x.rpe >= 9)
        }
        var total = bites.sumOf { it.food }
        val tonnage = work.filter { !it.warmup }.sumOf { x -> val ex = Catalog.of(x.exId); if (ex.kind == Kind.WEIGHT) x.weight * x.reps else 0.0 }
        if ("mammoth" in species && tonnage >= 500) { total += tonnage / 500; bonus += tr("Мамонт: ${tonnage(tonnage)}, +${(tonnage / 500).toInt()}", "Mammoth: ${tonnage(tonnage)}, +${(tonnage / 500).toInt()}") }
        val byCat = bites.groupBy { it.cat }.mapValues { e -> e.value.sumOf { it.food } }

        val shares = HashMap<Long, Double>()
        val hatched = ArrayList<Pet>()
        val prevWeekMet = weeks.met(weekStart(dayOf(w.start)) - 7)
        val now = pets.map { p ->
            if (p.slot !in 0..2) {
                // eggs at home are kept warm too, on a quarter of the food
                if (p.hatched != 0L) return@map p
                val g = p.growth + total * .25; val h = if (g >= Pets.HATCH) dayOf(w.start) else 0L
                return@map p.copy(growth = g, hatched = h).also { if (h > 0) hatched += it }
            }
            val nat = p.nat()
            var share = if (p.slot == 0) 1.0 else if (nat == Nature.SOCIAL) .75 else .5
            if (nat == Nature.GREEDY) share *= 1.2
            if (nat == Nature.STEADY && prevWeekMet) share *= 1.15
            var eat = total * share
            if (nat == Nature.STUBBORN) eat += bites.filter { it.hard }.sumOf { it.food } * .5 * share
            shares[p.id] = eat
            val cats = p.catMap().toMutableMap()
            for ((c, f) in byCat) cats[c.id] = (cats[c.id] ?: 0.0) + f * share
            if (p.hatched == 0L) {
                val g = p.growth + eat
                val h = if (g >= Pets.HATCH) dayOf(w.start) else 0L
                val np = p.copy(growth = g, hatched = h, cats = catString(cats), workouts = p.workouts + 1)
                if (h > 0) hatched += np
                np
            } else p.copy(belly = p.belly + eat, cats = catString(cats), shape = min(100.0, p.shape + 8), workouts = p.workouts + 1)
        }

        // plates
        val seen = beforeSets.map { it.exId }.toSet()
        val newEx = work.map { it.exId }.distinct().filter { it !in seen }
        val records = work.flatMap { x -> x.pr.split(',').mapNotNull { Rec.of(it) }.map { x to it } }
        var plates = 5 + min(20, work.count { !it.warmup }) + 10 * records.map { it.first.exId to it.second }.distinct().size
        if (before.isNotEmpty() && team.any { it.nat() == Nature.CURIOUS }) plates += 3 * newEx.size
        return Outcome(total, plates, records, emptyList(), hatched, emptyList(), newEx, shares, bonus) to now
    }

    /** Eggs whose conditions [t] now meets and that aren't owned yet. */
    fun finds(t: Tally, owned: Set<String>): List<String> {
        val out = ArrayList<String>()
        fun f(id: String, ok: Boolean) { if (ok && id !in owned) out += id }
        f("bull", (t.catSets[Cat.LEGS] ?: 0) >= 80)
        f("otter", t.cardioSecs >= 6 * 3600)
        f("lynx", (t.catSecs[Cat.FLEX] ?: 0) >= 3 * 3600)
        f("octopus", t.pullupReps >= 200 || (t.catSets[Cat.PULL] ?: 0) >= 120)
        f("crab", (t.catSets[Cat.PUSH] ?: 0) >= 120)
        f("hedgehog", t.holdSecs >= 3600)
        f("kangaroo", t.workoutsWith(Cat.JUMP) >= 15)
        f("hummingbird", t.workoutsWith(Cat.HIIT) >= 12)
        f("turtle", t.longestCardio > 3600)
        f("sloth", t.slowRestWorkouts() >= 10)
        f("squirrel", t.maxDistinct >= 6)
        f("gorilla", t.prs >= 25)
        f("ant", t.liftedBw)
        f("crane", t.workoutsWith(Cat.BAL) >= 12)
        f("mole", t.early >= 10)
        f("owl", t.late >= 10)
        f("hamster", t.weeks.best >= 6)
        f("phoenix", t.maxGap >= 21)
        f("mammoth", t.tonnage >= 250_000)
        f("dragon", t.workouts.size >= 120 && t.cats.size >= Cat.entries.size)
        return out
    }

    /** How near an egg is, 0..1, and the words for it. */
    fun near(id: String, t: Tally): Pair<Float, String> {
        fun r(v: Double, of: Double, txt: String) = (v / of).toFloat().coerceIn(0f, 1f) to txt
        return when (id) {
            "bull" -> r((t.catSets[Cat.LEGS] ?: 0).toDouble(), 80.0, "${t.catSets[Cat.LEGS] ?: 0} / 80")
            "otter" -> r(t.cardioSecs.toDouble(), 6 * 3600.0, "${hours(t.cardioSecs.toLong())} / ${tr("6 ч", "6 h")}")
            "lynx" -> r((t.catSecs[Cat.FLEX] ?: 0).toDouble(), 10800.0, "${hours((t.catSecs[Cat.FLEX] ?: 0).toLong())} / ${tr("3 ч", "3 h")}")
            "octopus" -> r(max(t.pullupReps / 200.0, (t.catSets[Cat.PULL] ?: 0) / 120.0), 1.0, "${t.pullupReps} / 200 · ${t.catSets[Cat.PULL] ?: 0} / 120")
            "crab" -> r((t.catSets[Cat.PUSH] ?: 0).toDouble(), 120.0, "${t.catSets[Cat.PUSH] ?: 0} / 120")
            "hedgehog" -> r(t.holdSecs.toDouble(), 3600.0, "${dur(t.holdSecs)} / ${tr("1 ч", "1 h")}")
            "kangaroo" -> r(t.workoutsWith(Cat.JUMP).toDouble(), 15.0, "${t.workoutsWith(Cat.JUMP)} / 15")
            "hummingbird" -> r(t.workoutsWith(Cat.HIIT).toDouble(), 12.0, "${t.workoutsWith(Cat.HIIT)} / 12")
            "turtle" -> r(t.longestCardio.toDouble(), 3600.0, "${dur(t.longestCardio)} / ${tr("1 ч", "1 h")}")
            "sloth" -> r(t.slowRestWorkouts().toDouble(), 10.0, "${t.slowRestWorkouts()} / 10")
            "squirrel" -> r(t.maxDistinct.toDouble(), 6.0, "${t.maxDistinct} / 6")
            "gorilla" -> r(t.prs.toDouble(), 25.0, "${t.prs} / 25")
            "ant" -> r((t.work.filter { Catalog.of(it.exId).kind == Kind.WEIGHT }.maxOfOrNull { it.weight } ?: 0.0), t.bw, "${wu(t.work.filter { Catalog.of(it.exId).kind == Kind.WEIGHT }.maxOfOrNull { it.weight } ?: 0.0)} / ${wu(t.bw)}")
            "crane" -> r(t.workoutsWith(Cat.BAL).toDouble(), 12.0, "${t.workoutsWith(Cat.BAL)} / 12")
            "mole" -> r(t.early.toDouble(), 10.0, "${t.early} / 10")
            "owl" -> r(t.late.toDouble(), 10.0, "${t.late} / 10")
            "hamster" -> r(t.weeks.best.toDouble(), 6.0, "${t.weeks.best} / 6")
            "phoenix" -> r(t.maxGap.toDouble(), 21.0, tr("перерыв ${t.maxGap} / 21 дн.", "break ${t.maxGap} / 21 days"))
            "mammoth" -> r(t.tonnage, 250_000.0, "${tonnage(t.tonnage)} / ${tonnage(250_000.0)}")
            "dragon" -> r(min(t.workouts.size / 120.0, t.cats.size / 9.0), 1.0, "${t.workouts.size} / 120 · ${t.cats.size} / 9")
            else -> 0f to ""
        }
    }

    /**
     * Turns food in the belly into growth for each day from the day after [from] to yesterday.
     * Rest days digest everything, training days a third. Long idle stretches cost shape.
     * Returns the pets after and news (stage changes) for the inbox.
     */
    fun digest(pets: List<Pet>, from: Long, until: Long, trainedDays: Set<Long>, s: Settings): Pair<List<Pet>, List<String>> {
        if (from >= until - 1) return pets to emptyList()
        val news = ArrayList<String>()
        val sloth = pets.any { it.species == "sloth" && it.slot in 0..2 && it.hatched > 0 }
        val lastTrained = trainedDays.maxOrNull()
        val out = pets.map { p0 ->
            if (p0.hatched == 0L) return@map p0
            var p = p0; val before = p.stage()
            var d = from + 1
            while (d < until) {
                val trained = d in trainedDays
                val sick = d in s.sickDays
                var frac = if (trained) .35 else 1.0
                if (p.nat() == Nature.SLEEPY && trained) frac = .5
                val eaten = p.belly * frac
                var grow = eaten
                if (!trained && sloth) grow *= 1.1
                if (p.nat() == Nature.SLEEPY && !trained) grow *= 1.1
                val idle = if (lastTrained == null) 0 else d - (trainedDays.filter { it <= d }.maxOrNull() ?: d)
                var shape = p.shape
                if (!trained && !sick && idle > 4) shape -= if (p.nat() == Nature.GREEDY) 5 else 3
                p = p.copy(growth = p.growth + grow, belly = p.belly - eaten, shape = shape.coerceIn(0.0, 100.0))
                d++
            }
            val after = p.stage()
            if (after != before) {
                news += "stage:${p.id}:${after.ordinal}"
                if (after == Stage.ADULT && p.form.isEmpty()) p = p.copy(form = Form.from(p.catMap()).id)
            }
            p
        }
        return out to news
    }
}

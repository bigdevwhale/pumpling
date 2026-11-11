package app.pumpling.domain

import app.pumpling.data.WSet
import kotlin.math.max

/** Epley's estimated one-rep max; only trusted up to 12 reps. */
fun e1rm(weight: Double, reps: Int): Double = when {
    reps <= 0 || weight <= 0 -> 0.0
    reps == 1 -> weight
    reps > 12 -> 0.0
    else -> weight * (1 + reps / 30.0)
}

/** Which records a set can break. */
enum class Rec(val id: String, val ru: String, val en: String) {
    E1RM("e1rm", "Расчётный максимум", "Estimated 1RM"), WEIGHT("weight", "Самый большой вес", "Heaviest weight"),
    REPS("reps", "Больше всего повторов", "Most reps"), HOLD("hold", "Дольше всего", "Longest hold"),
    DIST("dist", "Самая длинная дистанция", "Longest distance"), TIME("time", "Дольше всего", "Longest session"),
    PACE("pace", "Лучший темп", "Best pace"), ASSIST("assist", "Меньше всего помощи", "Least help");
    val label get() = tr(ru, en)
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
}

/** The bests of one exercise so far. */
class Best {
    var e1rm = 0.0; var weight = 0.0; var reps = 0; var hold = 0; var dist = 0.0; var time = 0; var pace = Double.MAX_VALUE; var assist = Double.MAX_VALUE
    /** reps → heaviest weight done for at least that many reps */
    val repMax = HashMap<Int, Double>()
    var sets = 0

    fun add(ex: Ex, s: WSet) {
        sets++
        when (ex.kind) {
            Kind.WEIGHT, Kind.BODY -> {
                e1rm = max(e1rm, e1rm(s.weight, s.reps)); weight = max(weight, s.weight)
                if (s.weight == 0.0 || ex.kind == Kind.BODY) reps = max(reps, s.reps)
                for (r in 1..minOf(s.reps, 20)) repMax[r] = max(repMax[r] ?: 0.0, s.weight)
            }
            Kind.ASSIST -> { reps = max(reps, s.reps); if (s.reps >= 5) assist = minOf(assist, s.weight) }
            Kind.HOLD -> { hold = max(hold, s.secs); weight = max(weight, s.weight) }
            Kind.CARDIO -> { dist = max(dist, s.meters); time = max(time, s.secs); if (s.meters >= 1000 && s.secs > 0) pace = minOf(pace, s.secs / (s.meters / 1000)) }
            Kind.MINUTES -> time = max(time, s.secs)
        }
    }

    /** Records [s] would break against these bests. Nothing is a record the first time. */
    fun beats(ex: Ex, s: WSet): List<Rec> {
        if (sets == 0) return emptyList()
        val out = ArrayList<Rec>()
        when (ex.kind) {
            Kind.WEIGHT -> { if (e1rm(s.weight, s.reps) > e1rm + 1e-6 && e1rm > 0) out += Rec.E1RM; if (s.weight > weight + 1e-6) out += Rec.WEIGHT
                else if (s.weight > 0 && s.reps > 0 && s.weight >= (repMax[s.reps] ?: 0.0) + 1e-6 && (repMax[s.reps] ?: 0.0) > 0) out += Rec.REPS }
            Kind.BODY -> { if (s.weight == 0.0 && s.reps > reps) out += Rec.REPS; if (s.weight > weight + 1e-6 && weight > 0) out += Rec.WEIGHT }
            Kind.ASSIST -> { if (s.reps > reps) out += Rec.REPS; if (s.reps >= 5 && s.weight < assist - 1e-6 && assist < Double.MAX_VALUE) out += Rec.ASSIST }
            Kind.HOLD -> if (s.secs > hold) out += Rec.HOLD
            Kind.CARDIO -> { if (s.meters > dist + 1e-6 && dist > 0) out += Rec.DIST; if (s.secs > time && dist == 0.0 && s.meters == 0.0) out += Rec.TIME
                if (s.meters >= 1000 && s.secs > 0 && s.secs / (s.meters / 1000) < pace - .5 && pace < Double.MAX_VALUE) out += Rec.PACE }
            Kind.MINUTES -> if (s.secs > time) out += Rec.TIME
        }
        return out
    }
}

object Records {
    /** Bests of every exercise over [sets] (done, not warm-up). */
    fun bests(sets: List<WSet>): Map<String, Best> {
        val m = HashMap<String, Best>()
        for (s in sets) if (s.done && !s.warmup) m.getOrPut(s.exId) { Best() }.add(Catalog.of(s.exId), s)
        return m
    }

    /**
     * Marks the records in one workout: for each exercise and kind of record only the best set
     * gets the flag. Returns set id → records.
     */
    fun mark(prev: Map<String, Best>, sets: List<WSet>): Map<Long, List<Rec>> {
        val out = HashMap<Long, MutableList<Rec>>()
        for ((exId, list) in sets.filter { it.done && !it.warmup }.groupBy { it.exId }) {
            val ex = Catalog.of(exId); val b = prev[exId] ?: continue
            val cand = HashMap<Rec, WSet>()
            for (s in list) for (r in b.beats(ex, s)) {
                val cur = cand[r]
                if (cur == null || better(ex, r, s, cur)) cand[r] = s
            }
            // an e1RM record already says it all for a heavier set; keep both only when they're different sets
            for ((r, s) in cand) out.getOrPut(s.id) { ArrayList() } += r
        }
        return out
    }

    private fun better(ex: Ex, r: Rec, a: WSet, b: WSet): Boolean = when (r) {
        Rec.E1RM -> e1rm(a.weight, a.reps) > e1rm(b.weight, b.reps)
        Rec.WEIGHT -> a.weight > b.weight
        Rec.REPS -> if (ex.kind == Kind.WEIGHT) a.weight > b.weight else a.reps > b.reps
        Rec.HOLD, Rec.TIME -> a.secs > b.secs
        Rec.DIST -> a.meters > b.meters
        Rec.PACE -> a.secs / a.meters < b.secs / b.meters
        Rec.ASSIST -> a.weight < b.weight
    }

    /** Words for a record: «100 кг × 5», «2:30», «5,2 км». */
    fun show(ex: Ex, r: Rec, s: WSet): String = when (r) {
        Rec.E1RM -> "${wu(s.weight)} × ${s.reps} (≈ ${wuA(e1rm(s.weight, s.reps))})"
        Rec.WEIGHT -> if (ex.kind == Kind.HOLD) wu(s.weight) else "${wu(s.weight)} × ${s.reps}"
        Rec.REPS -> if (s.weight > 0) "${wu(s.weight)} × ${s.reps}" else count(s.reps, "повтор", "повтора", "повторов", "rep", "reps")
        Rec.ASSIST -> "−${wu(s.weight)} × ${s.reps}"
        Rec.HOLD, Rec.TIME -> dur(s.secs)
        Rec.DIST -> km(s.meters)
        Rec.PACE -> pace(s.secs / (s.meters / 1000))
    }
}

/** Short words for a set: «60 × 8», «−20 × 6», «1:30», «5 км · 27:10». */
fun setLine(ex: Ex, s: WSet): String = when (ex.kind) {
    Kind.WEIGHT -> "${w(s.weight)} × ${s.reps}"
    Kind.BODY -> if (s.weight > 0) "+${w(s.weight)} × ${s.reps}" else "× ${s.reps}"
    Kind.ASSIST -> "−${w(s.weight)} × ${s.reps}"
    Kind.HOLD -> if (s.weight > 0) "${w(s.weight)} · ${clock(s.secs)}" else clock(s.secs)
    Kind.CARDIO -> listOfNotNull(s.meters.takeIf { it > 0 }?.let { km(it) }, clock(s.secs)).joinToString(" · ")
    Kind.MINUTES -> dur(s.secs)
}

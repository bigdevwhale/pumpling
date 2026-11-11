package app.pumpling.data

import app.pumpling.AppContainer
import app.pumpling.domain.Catalog
import app.pumpling.domain.Kind
import app.pumpling.domain.Nature
import app.pumpling.domain.Quests
import app.pumpling.domain.today
import app.pumpling.domain.tr
import app.pumpling.domain.weekStart
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** Half a year of training put through the real engine, for screenshots (demo builds only). */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.settings.now().onboarded) return
        c.dao.clearWorkouts(); c.dao.clearSets(); c.dao.clearPets(); c.dao.clearBodies(); c.dao.clearRoutines()
        Catalog.builtin(c.app)
        val r = Random(7)
        val days = 182
        val first = today() - days
        c.settings.update { it.copy(weekGoal = 3, bodyweight = 81.0, sex = "m", digested = first - 1, yearGoal = 150) }
        c.dao.insert(c.newPet("bull", 0, nature = Nature.STUBBORN).copy(name = tr("Бублик", "Bagel"), found = first, growth = 20.0, shiny = false))

        // weights grow week by week
        fun prog(start: Double, end: Double, day: Long, step: Double = 2.5): Double { val f = (day - first).toDouble() / days
            return (Math.round((start + (end - start) * f) / step) * step) }
        val A = listOf("squat" to (60.0 to 102.5), "bench" to (45.0 to 72.5), "row" to (45.0 to 70.0), "lateral" to (6.0 to 10.0), "plank" to (40.0 to 90.0))
        val B = listOf("deadlift" to (80.0 to 135.0), "ohp" to (30.0 to 47.5), "pullup" to (4.0 to 10.0), "lunge" to (12.0 to 22.0), "hanging" to (0.0 to 0.0))
        val C = listOf("leg_press" to (100.0 to 160.0), "incline_db" to (16.0 to 26.0), "lat_pulldown" to (45.0 to 65.0), "db_curl" to (10.0 to 14.0), "pushdown" to (20.0 to 30.0), "hollow" to (20.0 to 45.0))
        var plan = 0
        for (d in first..today() - 1) {
            val dow = LocalDate.ofEpochDay(d).dayOfWeek.value
            val pause = d in (first + 60)..(first + 71)          // a holiday
            if (pause) continue
            val lift = dow == 1 || dow == 3 || dow == 5
            val run = dow == 6 || (dow == 2 && r.nextInt(3) == 0)
            val yoga = dow == 7 && r.nextInt(2) == 0
            if (!lift && !run && !yoga) continue
            if (r.nextInt(9) == 0) continue
            val hour = when { dow == 6 -> 9; r.nextInt(6) == 0 -> 7; r.nextInt(6) == 0 -> 21; else -> 18 }
            val start = LocalDate.ofEpochDay(d).atTime(hour, r.nextInt(50)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val id = c.dao.insert(Workout(start = start))
            var t = start + 4 * 60_000L
            val sets = ArrayList<WSet>(); var block = 0
            fun add(ex: String, n: Int, f: (Int) -> WSet) { for (i in 0 until n) { t += (90 + r.nextInt(100)) * 1000L; sets += f(i).copy(workoutId = id, exId = ex, block = block, idx = i, done = true, doneAt = t) }; block++ }
            var title: String
            if (lift) {
                val list = when (plan % 3) { 0 -> A; 1 -> B; else -> C }; title = when (plan % 3) { 0 -> tr("Фулбоди А", "Full body A"); 1 -> tr("Фулбоди Б", "Full body B"); else -> tr("Объём", "Volume") }; plan++
                for ((ex, rng) in list) {
                    val e = Catalog.of(ex)
                    when {
                        ex == "hanging" -> add("leg_raise", 3) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, reps = 8 + r.nextInt(5)) }
                        e.kind == Kind.HOLD -> add(ex, 3) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, secs = prog(rng.first, rng.second, d, 5.0).toInt() + r.nextInt(10)) }
                        e.kind == Kind.BODY -> add(ex, 3) { i -> WSet(workoutId = 0, exId = "", block = 0, idx = 0, reps = (prog(rng.first, rng.second, d, 1.0) - i + r.nextInt(2)).toInt().coerceAtLeast(1)) }
                        else -> { val wt = prog(rng.first, rng.second, d, if (rng.second < 30) 1.0 else 2.5)
                            val warm = ex == "squat" || ex == "deadlift" || ex == "bench"
                            add(ex, if (warm) 4 else 3) { j -> val i = if (warm) j - 1 else j
                                if (i < 0) WSet(workoutId = 0, exId = "", block = 0, idx = 0, weight = Math.round(wt * .5 / 2.5) * 2.5, reps = 8, warmup = true)
                                else WSet(workoutId = 0, exId = "", block = 0, idx = 0, weight = wt, reps = (if (rng.second > 60) 5 else 10) - (if (i == 2 && r.nextInt(3) == 0) 1 else 0) + if (i == 0 && r.nextInt(5) == 0) 1 else 0,
                                    rpe = if (i == 2 && r.nextInt(2) == 0) 8 + r.nextInt(3) else 0) } }
                    }
                }
                if (r.nextInt(4) == 0) add("stretch", 1) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, secs = 600) }
            } else if (run) {
                title = tr("Пробежка", "Run")
                val km = if (r.nextInt(5) == 0) 10.0 else 5.0 + r.nextInt(3)
                val pace = 360 - 30 * (d - first) / days.toDouble() + r.nextInt(25)
                add(if (r.nextInt(4) == 0) "treadmill" else "run", 1) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, meters = km * 1000, secs = (km * pace).toInt()) }
                if (r.nextInt(3) == 0) add("hiit", 1) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, secs = 900) }
            } else {
                title = tr("Йога и баланс", "Yoga and balance")
                add("yoga", 1) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, secs = 1800 + r.nextInt(900)) }
                add("tree_pose", 2) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, secs = 40 + r.nextInt(30)) }
                if (r.nextInt(2) == 0) add("jump_rope", 1) { WSet(workoutId = 0, exId = "", block = 0, idx = 0, secs = 600) }
            }
            c.dao.insertSets(sets)
            c.settings.update { s -> s.copy(questWeek = weekStart(d), quests = Quests.deal(weekStart(d)), questsDone = if (s.questWeek == weekStart(d)) s.questsDone else emptySet()) }
            c.finish(id, title, "", if (r.nextInt(3) == 0) 4 + r.nextInt(2) else 3, t + 5 * 60_000L)
            // digest the days up to this one, as if the app was opened each day
            digestTo(c, d)
            if (d % 9 == 0L) c.addBody(Body(day = d, weight = 82.5 - 4.0 * (d - first) / days + r.nextDouble() * .8, waist = 88.0 - 4.0 * (d - first) / days))
        }
        digestTo(c, today())
        c.addBody(Body(day = today(), weight = 78.4, waist = 84.0))
        c.dao.insert(Routine(name = tr("Фулбоди А", "Full body A"), items = "squat|3|5|0|0;bench|3|5|0|0;row|3|10|0|0;lateral|3|10|0|0;plank|3|0|0|60", created = 1))
        c.dao.insert(Routine(name = tr("Фулбоди Б", "Full body B"), items = "deadlift|3|5|0|0;ohp|3|10|0|0;pullup|3|8|0|0;lunge|3|10|0|0", created = 2))
        c.dao.insert(Routine(name = tr("Объём", "Volume"), items = "leg_press|3|10|0|0;incline_db|3|10|0|0;lat_pulldown|3|10|0|0;db_curl|3|10|0|0;pushdown|3|10|0|0", created = 3))
        // things for the gym and a clean inbox
        c.settings.update { it.copy(onboarded = true, items = setOf("mat", "plant", "poster", "boombox", "lights", "bench", "kettle", "trophies"), plates = 286, inbox = emptyList(), digested = today() - 1,
            questWeek = weekStart(today()), quests = Quests.deal(weekStart(today())), questsDone = emptySet(),
            // screenshots: no first-visit hints
            seen = if (app.pumpling.BuildConfig.SHOTS) setOf("home", "live", "journal", "routines", "pets", "stats", "shop", "routine-edit") else emptySet()) }
        // the team: the bull leads, the two biggest found pets beside it
        val pets = c.dao.petList(); val others = pets.filter { it.species != "bull" }.sortedByDescending { it.growth }
        c.dao.updatePets(pets.map { p -> p.copy(slot = when { p.species == "bull" -> 0; p.id == others.getOrNull(0)?.id -> 1; p.id == others.getOrNull(1)?.id -> 2; else -> -1 }) })
    }

    private suspend fun digestTo(c: AppContainer, day: Long) {
        val s = c.settings.now()
        val trained = c.dao.workoutList().filter { it.end > 0 }.map { app.pumpling.domain.dayOf(it.start) }.toSet()
        if (s.digested < day - 1) {
            val (pets, _) = app.pumpling.domain.Engine.digest(c.dao.petList(), s.digested, day, trained, s)
            c.dao.updatePets(pets); c.settings.update { it.copy(digested = day - 1) }
        }
    }
}

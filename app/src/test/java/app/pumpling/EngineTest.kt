package app.pumpling

import app.pumpling.data.Pet
import app.pumpling.data.Settings
import app.pumpling.data.WSet
import app.pumpling.data.Workout
import app.pumpling.domain.Catalog
import app.pumpling.domain.Engine
import app.pumpling.domain.Place as P
import app.pumpling.domain.Rec
import app.pumpling.domain.Records
import app.pumpling.domain.Stage
import app.pumpling.domain.Supersets
import app.pumpling.domain.Weeks
import app.pumpling.domain.e1rm
import app.pumpling.domain.stage
import app.pumpling.domain.today
import app.pumpling.domain.weekStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class EngineTest {
    @Before fun load() { Catalog.load(File("src/main/assets/exercises.tsv").readLines()) }

    private fun set(id: Long, w: Long, ex: String, kg: Double, reps: Int, warm: Boolean = false) =
        WSet(id = id, workoutId = w, exId = ex, block = 0, idx = id.toInt(), weight = kg, reps = reps, done = true, warmup = warm)

    /** The «5 tonnes» quest counts what the journal calls «lifted», own-weight exercises too (#9). */
    @Test fun volumeQuestCountsLikeTheJournal() {
        val sets = listOf(set(1, 1, "squat", 100.0, 10), set(2, 1, "pushup", 0.0, 20), set(3, 1, "squat", 60.0, 10, warm = true))
        val wk = app.pumpling.domain.Week(listOf(Workout(id = 1, start = 0, end = 1)), sets, emptyList(), emptyList(), 0, savedWeight = 80.0)
        val q = app.pumpling.domain.Quests.byId.getValue("volume")
        assertEquals(1000.0 + 80.0 * .7 * 20, q.progress(wk), 1e-6)
        assertEquals(q.progress(wk), sets.filter { !it.warmup }.sumOf { app.pumpling.domain.Tonnage.of(it, 80.0) }, 1e-6)
    }

    @Test fun timeAndDaysByPlace() {
        // #24, #26: an hour in the gym and 20 minutes at home the same day, 30 minutes outside the next, one not said
        val d0 = today() - 3; val ms = d0 * 86_400_000L + 10 * 3_600_000L
        val ws = listOf(Workout(1, ms, ms + 3_600_000, place = "gym"), Workout(2, ms + 7_200_000, ms + 8_400_000, place = "home"),
            Workout(3, ms + 86_400_000, ms + 88_200_000, place = "street"), Workout(4, ms + 2 * 86_400_000L, ms + 2 * 86_400_000L + 600_000))
        val st = app.pumpling.domain.Stats(ws, emptyList(), emptyList(), Settings(), app.pumpling.domain.Span(d0 - 10, d0 + 10, app.pumpling.domain.Span.MONTH))
        assertEquals(listOf(P.GYM to 3600L, P.STREET to 1800L, P.HOME to 1200L), st.secsByPlace)
        val day = app.pumpling.domain.dayOf(ms)
        assertEquals(P.GYM, st.placeOfDay[day]); assertEquals(P.STREET, st.placeOfDay[day + 1]); assertEquals(null, st.placeOfDay[day + 2])
    }

    @Test fun epley() {
        assertEquals(100.0, e1rm(100.0, 1), 1e-9)
        assertEquals(116.67, e1rm(100.0, 5), .01)
        assertEquals(0.0, e1rm(100.0, 15), 1e-9)
    }

    @Test fun firstTimeIsNoRecord() {
        val marks = Records.mark(Records.bests(emptyList()), listOf(set(1, 1, "squat", 100.0, 5)))
        assertTrue(marks.isEmpty())
    }

    @Test fun recordsMarkOnlyTheBestSet() {
        val before = listOf(set(1, 1, "squat", 100.0, 5), set(2, 1, "squat", 100.0, 5))
        val now = listOf(set(3, 2, "squat", 102.5, 5), set(4, 2, "squat", 105.0, 3), set(5, 2, "squat", 60.0, 10, warm = true))
        val m = Records.mark(Records.bests(before), now)
        assertTrue(Rec.WEIGHT in m[4L].orEmpty())
        assertTrue(Rec.E1RM in m[3L].orEmpty())
        assertTrue(m[5L] == null)
    }

    @Test fun feedingFavoursTheLikedFamily() {
        val w = Workout(id = 1, start = System.currentTimeMillis(), end = System.currentTimeMillis() + 3600_000)
        val bull = Pet(id = 1, species = "bull", name = "B", nature = "steady", found = 0, hatched = 1, slot = 0)
        val legs = (1L..5L).map { set(it, 1, "squat", 80.0, 5) }
        val push = (1L..5L).map { set(it, 1, "bench", 60.0, 5) }
        val a = Engine.feed(w, legs, emptyList(), emptyList(), listOf(bull), Settings()).first.food
        val b = Engine.feed(w, push, emptyList(), emptyList(), listOf(bull), Settings()).first.food
        assertEquals(a, b * 1.5, 1e-6)
    }

    @Test fun eggHatchesAndRestDaysDigest() {
        val now = System.currentTimeMillis()
        val w = Workout(id = 1, start = now, end = now + 3600_000)
        val egg = Pet(id = 1, species = "otter", name = "O", nature = "steady", found = 0, growth = 20.0, slot = 0)
        val (out, pets) = Engine.feed(w, (1L..12L).map { set(it, 1, "bench", 50.0, 10) }, emptyList(), emptyList(), listOf(egg), Settings())
        assertEquals(1, out.hatched.size)
        assertEquals(Stage.BABY, pets[0].stage())
        val fed = pets[0].copy(belly = 50.0)
        val (after, _) = Engine.digest(listOf(fed), today() - 3, today(), setOf(today() - 2), Settings())
        assertTrue(after[0].growth > fed.growth + 49)   // a training day digests a third, the rest day the remainder
        assertTrue(after[0].belly < 1e-6)
    }

    @Test fun weekStreakCountsMetWeeks() {
        val mon = weekStart(today())
        val days = setOf(mon - 7, mon - 5, mon - 3, mon - 14, mon - 12, mon - 10, mon)
        assertEquals(2, Weeks(days, 3, emptySet()).streak)
        assertEquals(2, Weeks(days, 3, emptySet()).best)
    }

    @Test fun searchFindsSynonyms() {
        val all = Catalog.all()
        assertTrue(all.filter { it.matches("разведение ног") }.any { it.id == "abductor" })
        assertTrue(all.filter { it.matches("гравитрон") }.any { it.id == "assisted_pullup" })
        assertTrue(all.filter { it.matches("ЖИМ лёжа") }.any { it.id == "bench" })
        assertTrue(all.filter { it.matches("жим леж") }.any { it.id == "bench" })
        assertTrue(all.size >= 270)
    }

    @Test fun everyExerciseHasHowTo() {
        Catalog.loadHow(File("src/main/assets/howto.tsv").readLines())
        val missing = Catalog.all().filter { it.how.isBlank() }.map { it.id }
        assertTrue("no how-to for $missing", missing.isEmpty())
    }

    @Test fun logHelpNamesTheExercise() {
        val ex = Catalog.of("mountain")
        val help = ex.kind.help(ex)
        assertTrue(help, help.contains(ex.name))
        assertTrue(help, !help.contains("одтягиван") && !help.contains("ull-up"))
    }

    @Test fun supersetsLinkAndCut() {
        assertEquals(listOf(1, 1, 0), Supersets.link(listOf(0, 0, 0), 0, true))
        assertEquals(listOf(1, 1, 1), Supersets.link(listOf(1, 1, 0), 1, true))
        assertEquals(listOf(0, 1, 1), Supersets.link(listOf(1, 1, 1), 0, false))
        assertEquals(listOf(1, 1, 2, 2), Supersets.link(listOf(1, 1, 1, 1), 1, false))
        // the same number on blocks that don't stand together is no superset
        assertTrue(!Supersets.inSet(listOf(1, 0, 1), 0))
        assertEquals(listOf(1, 0, 1), Supersets.link(listOf(1, 0, 1), 5, true))   // no such block: nothing changes
    }

    @Test fun plansKeepSupersetsAndOldPlansStillRead() {
        val old = "squat|3|5|0.0|0;bench|3|5|0.0|0"
        assertEquals(old, Plan.join(Plan.parse(old)))
        val l = Plan.parse(old).map { it.copy(sup = 1) }
        assertEquals(listOf(1, 1), Plan.parse(Plan.join(l)).map { it.sup })
        // a weight set beforehand (#18) reads back, with or without a superset
        val kg = listOf(Plan("squat", 3, 5, 0.0, 0, kgs = listOf(60.0, 62.5, 0.0)), Plan("bench", 3, 5, 0.0, 0, sup = 2, kgs = listOf(40.0, 40.0, 40.0)), Plan("row", 3, 8, 0.0, 0))
        assertEquals(kg, Plan.parse(Plan.join(kg)))
        assertEquals(listOf(0, 2, 0), Plan.parse(Plan.join(kg)).map { it.sup })
        assertEquals("row|3|8|0.0|0", Plan.join(kg.drop(2)))   // nothing set beforehand: written as before
        // more sets take the weight of the last one, fewer drop the rest
        assertEquals(listOf(40.0, 45.0, 45.0, 45.0), Plan("bench", 2, 5, 0.0, 0, kgs = listOf(40.0, 45.0)).withSets(4).kgs)
        assertEquals(listOf(40.0), Plan("bench", 2, 5, 0.0, 0, kgs = listOf(40.0, 45.0)).withSets(1).kgs)
    }

    @Test fun reminderFallsOnTheNextChosenDay() {
        val mon = java.time.ZonedDateTime.of(2026, 11, 9, 12, 0, 0, 0, java.time.ZoneId.of("UTC"))   // a Monday noon
        assertEquals(mon.withHour(18), app.pumpling.work.Remind.next(setOf(1, 3), 18 * 60, mon))
        assertEquals(mon.plusDays(2).withHour(9), app.pumpling.work.Remind.next(setOf(1, 3), 9 * 60, mon))
        assertEquals(mon.plusDays(7).withHour(9), app.pumpling.work.Remind.next(setOf(1), 9 * 60, mon))
        assertEquals(null, app.pumpling.work.Remind.next(emptySet(), 9 * 60, mon))
    }

    @Test fun achievementsCountTiersAndPayEachOnce() {
        val ws = (1L..10L).map { Workout(id = it, start = it * 86400_000, end = it * 86400_000 + 3600_000) }
        val sets = ws.map { set(it.id, it.id, "squat", 100.0, 5) }
        val d = app.pumpling.domain.AchData(app.pumpling.domain.Tally(ws, sets, Settings(bodyweight = 80.0)), emptyList(), Settings(), 0)
        val a = app.pumpling.domain.Achievements
        assertEquals(2, a.byId.getValue("workouts").level(a.byId.getValue("workouts").value(d)))
        assertEquals(2, a.byId.getValue("own").level(a.byId.getValue("own").value(d)))   // 100 kg at 80 kg: ×0.5 and ×1
        val fresh = a.fresh(d, emptySet())
        assertTrue(fresh.any { it.first.id == "workouts" && it.second == 2 })
        assertTrue(a.fresh(d, fresh.map { (x, l) -> a.key(x, l) }.toSet()).isEmpty())
        assertTrue(a.all.all { x -> x.tiers == x.tiers.sorted() && x.tiers.distinct().size == x.tiers.size })
        assertEquals(a.all.size, a.all.map { it.id }.distinct().size)
    }

    @Test fun questsHaveTheirOwnIds() {
        assertEquals(app.pumpling.domain.Quests.all.size, app.pumpling.domain.Quests.byId.size)
        assertTrue(Catalog.all().filter { it.matches("тяга к груди узким хватом") }.any { it.id == "close_pulldown" })   // #11
    }

    @Test fun oneSidedSetsAreMarked() {
        val ex = Catalog.of("squat")
        assertTrue(app.pumpling.domain.setLine(ex, set(1, 1, "squat", 20.0, 8).copy(side = 1)).startsWith(app.pumpling.domain.sideMark(1)))
        assertEquals(app.pumpling.domain.setLine(ex, set(1, 1, "squat", 20.0, 8)), app.pumpling.domain.setLine(ex, set(1, 1, "squat", 20.0, 8).copy(side = 0)))
    }

    @Test fun placeCodes() {   // #13
        assertEquals(app.pumpling.domain.Place.STREET, app.pumpling.domain.Place.of("street"))
        assertEquals(null, app.pumpling.domain.Place.of(""))
    }

    /** Two dumbbells lift twice the weight written; one dumbbell, or a left or right set, once (#34). */
    @Test fun dumbbellPairsCountTwice() {
        val t = app.pumpling.domain.Tonnage
        assertEquals(400.0, t.of(set(1, 1, "db_bench", 20.0, 10), 80.0), 1e-6)
        assertEquals(200.0, t.of(set(2, 1, "db_row", 20.0, 10), 80.0), 1e-6)
        assertEquals(200.0, t.of(set(3, 1, "db_curl", 20.0, 10).copy(side = 1), 80.0), 1e-6)
        assertEquals(1000.0, t.of(set(4, 1, "squat", 100.0, 10), 80.0), 1e-6)
        assertTrue(t.ONE_DB.all { Catalog.of(it).equip == app.pumpling.domain.Equip.DB })
    }

    /** Stretching in motion is written in reps but lifts nothing (#35); a class logged at a group counts as a class (#29). */
    @Test fun stretchesAndClasses() {
        assertEquals(0.0, app.pumpling.domain.Tonnage.of(set(1, 1, "worlds_greatest", 0.0, 60), 80.0), 1e-6)
        assertTrue(Catalog.all().filter { it.matches("растяжка") }.any { it.id == "stretching_class" })
        val ws = listOf(Workout(1, 0, 600_000, place = P.GROUP.code), Workout(2, 0, 600_000, classOf = "pilates"), Workout(3, 0, 600_000, place = P.HOME.code))
        val d = app.pumpling.domain.AchData(app.pumpling.domain.Tally(ws, emptyList(), Settings()), emptyList(), Settings(), 0)
        assertEquals(2.0, app.pumpling.domain.Achievements.byId.getValue("classes").value(d), 1e-9)
    }

    /** How many workouts in a row the heaviest set stayed the same (#33). */
    @Test fun plateau() {
        val p = app.pumpling.domain.Plateau
        val h = listOf(listOf(set(1, 3, "lat_pulldown", 30.0, 8), set(2, 3, "lat_pulldown", 30.0, 7)), listOf(set(3, 2, "lat_pulldown", 30.0, 8)),
            listOf(set(4, 1, "lat_pulldown", 27.5, 8)))
        assertEquals(30.0 to 2, p.of(h)!!.let { it.first.weight to it.second })
        assertEquals(1, p.of(listOf(listOf(set(5, 4, "lat_pulldown", 32.5, 8))) + h)!!.second)
        assertEquals(null, p.of(emptyList()))
    }

    /** Plans for a day: by day, days gone by and broken ones left out (#31). */
    @Test fun plannedDays() {
        val s = Settings(planned = listOf("${today() - 1}:4", "${today() + 2}:7", "${today()}:5", "junk"))
        assertEquals(listOf(today() to 5L, today() + 2 to 7L), s.plans(today()))
    }
}

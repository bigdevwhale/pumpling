package app.pumpling

import app.pumpling.data.Pet
import app.pumpling.data.Settings
import app.pumpling.data.WSet
import app.pumpling.data.Workout
import app.pumpling.domain.Catalog
import app.pumpling.domain.Engine
import app.pumpling.domain.Rec
import app.pumpling.domain.Records
import app.pumpling.domain.Stage
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
}

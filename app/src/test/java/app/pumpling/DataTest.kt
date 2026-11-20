package app.pumpling

import app.pumpling.data.Backup
import app.pumpling.data.Pet
import app.pumpling.domain.Catalog
import app.pumpling.domain.Lang
import app.pumpling.domain.Supersets
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** The real database and settings: a workout from start to finish, then out to a backup and back. One test, as the settings store lives once per process. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DataTest {
    private lateinit var c: AppContainer

    @Before fun up() { Lang.ru = true; Catalog.load(File("src/main/assets/exercises.tsv").readLines()); c = AppContainer(RuntimeEnvironment.getApplication()) }
    @After fun down() { c.db.close() }

    @Test fun aWeightSetBeforehandWinsOverLastTime() = runBlocking {
        // last time: squats at 60 (written straight in: finish() would pay plates into settings other tests share)
        val first = c.dao.insert(app.pumpling.data.Workout(start = 1_000, end = 2_000_000))
        c.dao.insertSets((0..1).map { app.pumpling.data.WSet(workoutId = first, exId = "squat", block = 0, idx = it, weight = 60.0, reps = 5, done = true, doneAt = 1_000_000) })
        // the plan: squats 65, 70 and one left empty set beforehand, bench left empty, and a saved day's weight that last time beats
        val r = app.pumpling.data.Routine(id = 1, name = "R", items = Plan.join(listOf(Plan("squat", 3, 5, 50.0, 0, kgs = listOf(65.0, 70.0, 0.0)), Plan("bench", 2, 8, 40.0, 0))))
        val id = c.start(r)
        val sets = c.dao.setsOfNow(id)
        assertEquals(listOf(65.0, 70.0, 60.0), sets.filter { it.exId == "squat" }.map { it.weight })   // the empty one: last time's
        assertEquals(listOf(40.0, 40.0), sets.filter { it.exId == "bench" }.map { it.weight })   // never done: the plan's fallback
        c.dao.deleteSetsOf(id); c.dao.deleteWorkout(id)
        // without a weight set beforehand, last time wins over the saved one, as before
        val id2 = c.start(r.copy(items = Plan.join(listOf(Plan("squat", 2, 5, 50.0, 0)))))
        assertEquals(listOf(60.0, 60.0), c.dao.setsOfNow(id2).map { it.weight })
    }

    @Test fun stretchingIsLoggedInOneGo() = runBlocking {
        val s0 = c.settings.now()
        val end = 50_000_000L
        val (id, out) = c.logDone("stretch", 10, "home", end)!!
        c.settings.update { it.copy(plates = s0.plates, achieved = s0.achieved) }   // settings are shared between tests
        val w = c.dao.workout(id)!!
        assertEquals(end, w.end); assertEquals(end - 600_000, w.start)
        assertEquals("Растяжка", w.title); assertEquals("home", w.place); assertTrue(w.manual)
        val sets = c.dao.setsOfNow(id)
        assertEquals(1, sets.size); assertEquals(600, sets.single().secs); assertTrue(sets.single().done)
        assertTrue(out.food > 0)   // the pets ate
        // a workout going on: nothing is logged over it
        val live = c.start()
        assertNull(c.logDone("yoga", 15, "", end + 1_000_000))
        c.dao.deleteWorkout(live)
    }

    @Test fun weightsSetBeforehandAreUsedUpByTheWorkout() = runBlocking {
        val rid = c.dao.insert(app.pumpling.data.Routine(name = "R", items = Plan.join(listOf(Plan("squat", 2, 5, 0.0, 0, kgs = listOf(70.0, 75.0)), Plan("bench", 2, 5, 0.0, 0, kgs = listOf(50.0, 50.0))))))
        val id = c.start(c.dao.routineList().single { it.id == rid })
        // squats done, bench skipped today
        c.dao.updateSets(c.dao.setsOfNow(id).filter { it.exId == "squat" }.map { it.copy(done = true, doneAt = System.currentTimeMillis()) })
        val plates = c.settings.now().plates
        c.finish(id, "", "", 0)
        c.settings.update { it.copy(plates = plates, achieved = emptySet()) }   // settings are shared between tests
        val after = Plan.parse(c.dao.routineList().single { it.id == rid }.items)
        assertEquals(emptyList<Double>(), after.single { it.exId == "squat" }.kgs)            // last time's from now on: 70, 75
        assertEquals(listOf(50.0, 50.0), after.single { it.exId == "bench" }.kgs)             // not done: still waiting
        val next = c.start(c.dao.routineList().single { it.id == rid })
        assertEquals(listOf(70.0, 75.0), c.dao.setsOfNow(next).filter { it.exId == "squat" }.map { it.weight })
    }

    @Test fun workoutFinishesOnceAndABackupBringsItBack() = runBlocking {
        c.dao.insert(Pet(species = "bull", name = "B", nature = "steady", found = 0, hatched = 1, slot = 0))
        val id = c.start()
        c.addExercise(id, "squat"); c.addExercise(id, "bench")
        c.link(id, 0, true)
        val live = c.dao.setsOfNow(id)
        assertTrue("both exercises share a superset", live.all { it.superset > 0 } && live.map { it.superset }.distinct().size == 1)
        assertTrue(Supersets.linked(live.groupBy { it.block }.toSortedMap().values.map { it.first().superset }, 0))
        c.dao.updateSets(live.map { it.copy(weight = 60.0, reps = 8, done = true, doneAt = System.currentTimeMillis()) })

        val out = c.finish(id, "Legs", "", 4)
        assertNotNull(out); out!!
        val fed = c.dao.petList().single()
        assertTrue(fed.belly > 0)
        assertTrue(c.dao.workout(id)!!.end > 0)
        assertEquals(out.plates, c.settings.now().plates)
        assertTrue("the first workout is an achievement", out.achievements.any { it.first.id == "workouts" && it.second == 1 })
        assertTrue("workouts:1" in c.settings.now().achieved)

        // a second «Finish» for the same workout feeds nobody and pays nothing
        assertNull(c.finish(id, "Legs", "", 4))
        assertEquals(fed.belly, c.dao.petList().single().belly, 1e-9)
        assertEquals(out.plates, c.settings.now().plates)
        // and the daily housekeeping doesn't pay an achievement twice
        c.tick()
        assertEquals(out.plates, c.settings.now().plates)

        // out to a backup…
        val file = ByteArrayOutputStream().also { Backup.write(c, it, "test") }.toByteArray()
        val sets = c.dao.setList(); val pets = c.dao.petList(); val settings = c.settings.now()

        // a file that isn't a backup changes nothing
        val junk = runCatching { Backup.read(c, ByteArrayInputStream("not a backup".toByteArray())) }
        assertTrue(junk.exceptionOrNull() is Backup.Unreadable)
        assertEquals(sets, c.dao.setList())

        // …everything is lost…
        c.deleteWorkout(id)
        c.settings.update { it.copy(plates = 0, achieved = emptySet(), rest = 0) }
        assertTrue(c.dao.workoutList().isEmpty() && c.dao.setList().isEmpty())

        // …and it comes back as it was
        Backup.read(c, ByteArrayInputStream(file))
        assertEquals(sets, c.dao.setList())
        assertEquals(pets, c.dao.petList())
        assertEquals("Legs", c.dao.workout(id)!!.title)
        assertEquals(settings.plates, c.settings.now().plates)
        assertEquals(settings.achieved, c.settings.now().achieved)
        assertEquals(settings.rest, c.settings.now().rest)
    }
}

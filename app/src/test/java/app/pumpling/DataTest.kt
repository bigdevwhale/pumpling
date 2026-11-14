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

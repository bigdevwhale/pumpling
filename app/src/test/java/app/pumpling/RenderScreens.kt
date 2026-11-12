package app.pumpling

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.pumpling.data.DemoSeed
import app.pumpling.domain.Lang
import app.pumpling.ui.Root
import app.pumpling.ui.Start
import app.pumpling.ui.theme.PumpTheme
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.Look
import app.pumpling.ui.share.Painter
import app.pumpling.ui.share.ShareReq
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every screen of the demo drawn to PNGs in app/build/art/screens (run with `-Prender`).
 * The screen is very tall, so whole scrolling pages fit in one picture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h2400dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val dark = System.getProperty("render.dark") == "1"

    private fun shot(name: String) {
        repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
        val v = rule.activity.window.decorView
        // crop the empty tail of tall pages
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "${if (dark) "d-" else ""}$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<PumplingApp>().container
        runBlocking { DemoSeed.seedIfEmpty(c); c.loadOwn() }
        val pets = runBlocking { c.dao.petList() }
        val last = runBlocking { c.dao.workoutList().maxBy { it.start } }
        val shots = listOf(
            "1-home" to (0 to null), "2-log" to (1 to null), "3-pets" to (2 to null), "4-stats" to (3 to null),
            "5-pet" to (2 to "pet:${pets.first().id}"), "6-workout-view" to (1 to "workout:${last.id}"), "7-exercise" to (3 to "ex:squat"),
            "8-shop" to (0 to "shop"), "9-settings" to (0 to "settings"), "10-body" to (0 to "body"), "11-picker" to (0 to "picker"),
            "12-routine" to (1 to "routine"), "12b-routine-a" to (1 to null), "13-wrapped" to (3 to "wrapped"),
            "15-share-sheet" to (0 to "share:GYM"),
            "16-exercise-how" to (3 to "ex:mountain"), "17-workout-edit" to (1 to "edit:${last.id}"),
        )
        var i by mutableIntStateOf(-1)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val s by c.settings.settings.collectAsState(null)
            PumpTheme(dark) { s?.let { st -> key(i) { if (i >= 0) Root(st) } } }
        }
        for ((n, sh) in shots.withIndex()) {
            Start.tab = sh.second.first; Start.open = sh.second.second
            rule.runOnUiThread { i = n }
            shot(sh.first)
        }
        // the stats sections
        listOf("4b-stats-lifts", "4c-stats-muscles", "4d-stats-cardio", "4e-stats-records", "4f-stats-numbers").forEachIndexed { k, name ->
            Start.tab = 3; Start.open = "part:$k"; rule.runOnUiThread { i = 100 + k }; shot(name) }
        Start.tab = 2; Start.open = null; rule.runOnUiThread { i = 150 }
        repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }; rule.onAllNodesWithText("Бестиарий")[0].performClick(); shot("3b-bestiary")
        // rain in the window
        runBlocking { c.settings.update { it.copy(outside = app.pumpling.data.Outside(app.pumpling.data.Weather.RAIN, 90, 6.0, 0, 0, System.currentTimeMillis()).json()) } }
        Start.tab = 0; rule.runOnUiThread { i = 160 }; shot("1b-home-rain")
        // the live workout
        runBlocking { val w = c.start(null); listOf("squat", "bench", "plank", "run").forEach { c.addExercise(w, it) }
            c.dao.setsOfNow(w).take(2).forEach { s -> c.dao.update(s.copy(done = true, doneAt = System.currentTimeMillis())) }
            c.addExercise(w, "bulgarian"); c.split(w, c.dao.setsOfNow(w).maxOf { it.block }, true) }
        Start.tab = 0; Start.open = "live"; rule.runOnUiThread { i = 200 }
        shot("14-live")
        // every card at full size, feed and story
        val data = runBlocking { app.pumpling.ui.Data(c.settings.now(), c.dao.workoutList(), c.dao.setList(), c.dao.petList(), c.dao.bodyList(), c.dao.routineList(), c.dao.ownExList()) }
        val rec = data.doneSets.last { it.pr.isNotEmpty() }
        val reqs = listOf(ShareReq(CardKind.WORKOUT, last.id), ShareReq(CardKind.RECORD, rec.id), ShareReq(CardKind.WEEK), ShareReq(CardKind.MONTH), ShareReq(CardKind.YEAR),
            ShareReq(CardKind.PET, pets.first().id), ShareReq(CardKind.GYM), ShareReq(CardKind.BESTIARY), ShareReq(CardKind.EXERCISE, ex = "squat"),
            ShareReq(CardKind.MUSCLES), ShareReq(CardKind.RECORDS, span = app.pumpling.domain.Span.of(app.pumpling.domain.Span.QUARTER)), ShareReq(CardKind.PERIOD, span = app.pumpling.domain.Span.of(app.pumpling.domain.Span.QUARTER)),
            ShareReq(CardKind.CARDIO, span = app.pumpling.domain.Span.of(app.pumpling.domain.Span.QUARTER)), ShareReq(CardKind.LIFTS, span = app.pumpling.domain.Span.of(app.pumpling.domain.Span.QUARTER)))
        val cards = File(out.parentFile, "cards").apply { mkdirs() }
        reqs.forEachIndexed { k, r -> listOf(false, true).forEach { story ->
            val bmp = Painter(rule.activity, data, Look(story, k % 6, false)).render(r).first
            File(cards, "${k}-${r.kind.name.lowercase()}${if (story) "-story" else ""}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) } } }
    }
}

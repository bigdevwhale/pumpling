package app.pumpling

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import app.pumpling.domain.Lang
import app.pumpling.ui.Onboarding
import app.pumpling.ui.theme.PumpTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The onboarding steps drawn to PNGs in app/build/art/onboarding (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h1600dp-xxhdpi")
class RenderOnboarding {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "onboarding").apply { mkdirs() }

    private fun shot(name: String) {
        repeat(4) { rule.mainClock.advanceTimeBy(400); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun tap(t: String) { repeat(3) { rule.mainClock.advanceTimeBy(300); org.robolectric.shadows.ShadowLooper.idleMainLooper() }; rule.onAllNodesWithText(t)[0].performClick() }

    @Test fun render() {
        Lang.ru = true
        rule.mainClock.autoAdvance = false
        rule.setContent { PumpTheme(false) { Onboarding() } }
        tap("Выбрать яйцо"); tap("Дальше"); tap("Дальше")
        tap("Зал"); tap("Дом"); tap("Гантели"); tap("Турник"); tap("Мышцы и рельеф"); tap("Похудеть"); tap("Средний")
        shot("3-survey")
        tap("Подобрать программы")
        shot("4-suggest")
    }
}

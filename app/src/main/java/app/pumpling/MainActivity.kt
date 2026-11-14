package app.pumpling

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pumpling.data.DemoSeed
import app.pumpling.data.Settings
import app.pumpling.domain.Lang
import app.pumpling.ui.Onboarding
import app.pumpling.ui.Root
import app.pumpling.ui.Start
import app.pumpling.ui.theme.PumpTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty() && AppCompatDelegate.getApplicationLocales().toLanguageTags() != BuildConfig.DEMO_LANG) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(BuildConfig.DEMO_LANG)); return }
        Lang.ru = resources.configuration.locales[0].language == "ru"
        if (BuildConfig.DEMO) {
            container.scope.launch { DemoSeed.seedIfEmpty(container) }
            Start.tab = intent.getIntExtra("tab", 0); Start.open = intent.getStringExtra("open")
        }
        if (savedInstanceState == null) showHealthNote(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val sys = isSystemInDarkTheme()
            val dark = when (settings?.theme) { "dark" -> true; "light" -> false; else -> sys }
            LaunchedEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t), navigationBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t))
            }
            PumpTheme(dark) {
                settings?.let { s -> if (s.onboarded) Root(s) else Onboarding() }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); showHealthNote(intent) }
    override fun onStart() { super.onStart(); app.pumpling.work.Rest.shown = true; app.pumpling.work.Rest.seen(this); app.pumpling.work.Remind.clear(this) }
    override fun onStop() { app.pumpling.work.Rest.shown = false; super.onStop() }

    /** Health Connect's «why does this app want access» opens the privacy note, where it is written down. */
    private fun showHealthNote(i: android.content.Intent?) {
        val a = i?.action ?: return
        if (a != "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" && a != "android.intent.action.VIEW_PERMISSION_USAGE") return
        runCatching { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_URL))) }
    }
}

const val PRIVACY_URL = "https://github.com/bigdevwhale/pumpling/blob/main/PRIVACY.md"

/** Switches the app language: "" follows the phone, "ru" or "en" pins it. The activity restarts in the new language. */
fun setLanguage(tag: String) = AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
fun currentLanguage(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

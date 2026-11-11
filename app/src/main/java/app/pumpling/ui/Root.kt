package app.pumpling.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pumpling.container
import app.pumpling.data.Body
import app.pumpling.data.OwnEx
import app.pumpling.data.Pet
import app.pumpling.data.Routine
import app.pumpling.data.Settings
import app.pumpling.data.WSet
import app.pumpling.data.Workout
import app.pumpling.domain.Catalog
import app.pumpling.domain.Outcome
import app.pumpling.domain.Units
import app.pumpling.domain.tr
import app.pumpling.toEx
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.home.HomeScreen
import app.pumpling.ui.home.Inbox
import app.pumpling.ui.home.SettingsPage
import app.pumpling.ui.home.ShopPage
import app.pumpling.ui.journal.BodyPage
import app.pumpling.ui.journal.JournalScreen
import app.pumpling.ui.journal.RoutinePage
import app.pumpling.ui.journal.WorkoutPage
import app.pumpling.ui.pets.PetPage
import app.pumpling.ui.pets.PetsScreen
import app.pumpling.ui.share.ShareSheet
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.ShareReq
import app.pumpling.ui.stats.ExercisePage
import app.pumpling.ui.stats.StatsScreen
import app.pumpling.ui.stats.WrappedPage
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T
import app.pumpling.ui.workout.OwnExPage
import app.pumpling.ui.workout.PickerPage
import app.pumpling.ui.workout.ResultPage
import app.pumpling.ui.workout.WorkoutScreen
import kotlinx.coroutines.delay

/** Where a demo/screenshot launch opens (set from the intent in demo builds only). */
object Start { var tab = 0; var open: String? = null }

/** A page over the tabs. */
sealed class Screen {
    data object Live : Screen()
    data class Result(val workoutId: Long, val outcome: Outcome) : Screen()
    data class WorkoutView(val id: Long) : Screen()
    data class Exercise(val exId: String) : Screen()
    data class PetInfo(val id: Long) : Screen()
    data object Shop : Screen()
    data object Settings : Screen()
    data class RoutineEdit(val id: Long) : Screen()
    /** Picks exercises: for the live workout or for a routine being edited. */
    data class Picker(val forRoutine: Boolean = false) : Screen()
    data class OwnEdit(val id: Long) : Screen()
    data object BodyLog : Screen()
    data class Wrapped(val year: Int) : Screen()
    /** A stats section: 0 lifts, 1 muscles, 2 cardio, 3 records, 4 all the numbers. */
    data class StatsPart(val part: Int) : Screen()
}

class Nav {
    var tab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()
    var share by mutableStateOf<ShareReq?>(null)
    /** Exercises picked for the routine editor. */
    var picked by mutableStateOf<List<String>>(emptyList())
    /** A name typed in the search that found nothing, for a new own exercise. */
    var draftName by mutableStateOf("")
    /** A start waiting for «Start»: a routine id, 0 for a workout without one; null when nothing is asked. */
    var askStart by mutableStateOf<Long?>(null)
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(s: Screen) { pop(); push(s) }
}

/** Everything the screens read, collected once. */
class Data(val s: Settings, val workouts: List<Workout>, val sets: List<WSet>, val pets: List<Pet>, val bodies: List<Body>, val routines: List<Routine>, val own: List<OwnEx>) {
    val finished = workouts.filter { it.end > 0 }
    val live = workouts.firstOrNull { it.end == 0L }
    val team = pets.filter { it.slot in 0..2 }.sortedBy { it.slot }
    val leader = team.firstOrNull { it.slot == 0 }
    private val ids = finished.map { it.id }.toSet()
    val doneSets = sets.filter { it.done && it.workoutId in ids }
    val setsBy: Map<Long, List<WSet>> = sets.groupBy { it.workoutId }
}

@Composable
fun Root(s: Settings) {
    val c = LocalContext.current.container
    val workouts by c.dao.workouts().collectAsState(initial = emptyList())
    val sets by c.dao.sets().collectAsState(initial = emptyList())
    val pets by c.dao.pets().collectAsState(initial = emptyList())
    val bodies by c.dao.bodies().collectAsState(initial = emptyList())
    val routines by c.dao.routines().collectAsState(initial = emptyList())
    val own by c.dao.ownEx().collectAsState(initial = emptyList())
    LaunchedEffect(own) { Catalog.own = own.filter { !it.archived }.map { it.toEx() } }
    Units.lb = s.lb
    val data = remember(s, workouts, sets, pets, bodies, routines, own) { Data(s, workouts, sets, pets, bodies, routines, own) }
    val nav = remember { Nav().apply { tab = Start.tab } }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.news.collect { note = it; delay(2600); note = null } }
    LaunchedEffect(Unit) { c.tick() }
    LaunchedEffect(Unit) { if (!app.pumpling.BuildConfig.SHOTS) while (true) { c.refreshOutside(); delay(30 * 60_000L) } }
    LaunchedEffect(Unit) {
        when (Start.open) { "live" -> nav.push(Screen.Live); "shop" -> nav.push(Screen.Shop); "wrapped" -> nav.push(Screen.Wrapped(java.time.LocalDate.now().year))
            "body" -> nav.push(Screen.BodyLog); "settings" -> nav.push(Screen.Settings)
            "picker" -> nav.push(Screen.Picker()); "routine" -> nav.push(Screen.RoutineEdit(0))
            else -> Start.open?.let { o -> if (o.startsWith("part:")) nav.push(Screen.StatsPart(o.drop(5).toInt())) else if (o.startsWith("pet:")) nav.push(Screen.PetInfo(o.drop(4).toLong())) else if (o.startsWith("workout:")) nav.push(Screen.WorkoutView(o.drop(8).toLong())) else if (o.startsWith("ex:")) nav.push(Screen.Exercise(o.drop(3))) else if (o.startsWith("share:")) nav.share = ShareReq(CardKind.valueOf(o.drop(6).substringBefore(':')), o.drop(6).substringAfter(':', "0").toLong()) } }
        Start.open = null
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                when (nav.tab) {
                    0 -> HomeScreen(data, nav)
                    1 -> JournalScreen(data, nav)
                    2 -> PetsScreen(data, nav)
                    else -> StatsScreen(data, nav)
                }
            }
            Dock(nav.tab, data.live != null, { nav.tab = it }) {
                if (data.live == null) nav.askStart = 0L else nav.push(Screen.Live)
            }
        }
        for (scr in nav.stack) Box(Modifier.fillMaxSize()) {
            when (scr) {
                Screen.Live -> WorkoutScreen(data, nav)
                is Screen.Result -> ResultPage(scr, data, nav)
                is Screen.WorkoutView -> WorkoutPage(scr.id, data, nav)
                is Screen.Exercise -> ExercisePage(scr.exId, data, nav)
                is Screen.PetInfo -> PetPage(scr.id, data, nav)
                Screen.Shop -> ShopPage(data, nav)
                Screen.Settings -> SettingsPage(data, nav)
                is Screen.RoutineEdit -> RoutinePage(scr.id, data, nav)
                is Screen.Picker -> PickerPage(scr.forRoutine, data, nav)
                is Screen.OwnEdit -> OwnExPage(scr.id, data, nav)
                Screen.BodyLog -> BodyPage(data, nav)
                is Screen.Wrapped -> WrappedPage(scr.year, data, nav)
                is Screen.StatsPart -> app.pumpling.ui.stats.StatsPartPage(scr.part, data, nav)
            }
        }
        if (nav.stack.isEmpty()) Inbox(data, nav)
        nav.askStart?.let { app.pumpling.ui.workout.StartSheet(it, data, nav) }
        nav.share?.let { ShareSheet(it, data) { nav.share = null } }
        AnimatedVisibility(note != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 18.dp, end = 18.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(note ?: "", style = T.bodyB.copy(color = C.accInk), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.ink).padding(horizontal = 16.dp, vertical = 11.dp))
        }
    }
}

@Composable
private fun Dock(sel: Int, live: Boolean, onSel: (Int) -> Unit, onGo: () -> Unit) {
    val tabs = listOf(tr("Зал", "Gym") to Icons.GYM, tr("Журнал", "Log") to Icons.LOG, tr("Питомцы", "Pets") to Icons.PAW, tr("Летопись", "Stats") to Icons.STATS)
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().shadow(12.dp, ambientColor = C.shadow, spotColor = C.shadow).background(C.nav).navigationBarsPadding().padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, (label, ic) ->
                if (i == 2) Box(Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(C.nav).padding(3.dp).clip(CircleShape).background(if (live) C.ok else C.acc).clickable(onClick = onGo),
                        contentAlignment = Alignment.Center) { Icon(if (live) Icons.TIMER else Icons.PLUS, C.accInk, 28.dp) }
                }
                val col = if (i == sel) C.acc else C.mute
                Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(ic, col, 24.dp)
                    Text(label, style = T.tiny.copy(color = col), modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

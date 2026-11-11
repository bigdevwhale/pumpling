package app.pumpling

import android.app.Application
import android.content.Context
import app.pumpling.data.Body
import app.pumpling.data.Outside
import app.pumpling.data.OutsideSource
import app.pumpling.data.Db
import app.pumpling.data.OwnEx
import app.pumpling.data.Pet
import app.pumpling.data.Routine
import app.pumpling.data.SettingsStore
import app.pumpling.data.WSet
import app.pumpling.data.Workout
import app.pumpling.domain.Catalog
import app.pumpling.domain.Cat
import app.pumpling.domain.Engine
import app.pumpling.domain.Equip
import app.pumpling.domain.Ex
import app.pumpling.domain.Kind
import app.pumpling.domain.Lang
import app.pumpling.domain.Muscle
import app.pumpling.domain.Nature
import app.pumpling.domain.Outcome
import app.pumpling.domain.Pets
import app.pumpling.domain.Quests
import app.pumpling.domain.Records
import app.pumpling.domain.Rec
import app.pumpling.domain.Shop
import app.pumpling.domain.Tally
import app.pumpling.domain.Units
import app.pumpling.domain.Week
import app.pumpling.domain.dayOf
import app.pumpling.domain.today
import app.pumpling.domain.weekStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.random.Random

/** One planned exercise of a routine. */
data class Plan(val exId: String, val sets: Int, val reps: Int, val weight: Double, val secs: Int) {
    fun code() = "$exId|$sets|$reps|$weight|$secs"
    companion object {
        fun parse(items: String): List<Plan> = items.split(';').filter { it.isNotBlank() }.map { val c = it.split('|')
            Plan(c[0], c.getOrNull(1)?.toIntOrNull() ?: 3, c.getOrNull(2)?.toIntOrNull() ?: 0, c.getOrNull(3)?.toDoubleOrNull() ?: 0.0, c.getOrNull(4)?.toIntOrNull() ?: 0) }
        fun join(l: List<Plan>) = l.joinToString(";") { it.code() }
    }
}

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = Db.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val news = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private val lock = Mutex()
    val photos: File get() = File(app.filesDir, "photos").apply { mkdirs() }

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }
    fun say(s: String) { news.tryEmit(s) }

    suspend fun loadOwn() { Catalog.own = dao.ownExList().filter { !it.archived }.map { it.toEx() } }

    // ---------- the workout ----------
    suspend fun start(routine: Routine? = null, at: Long = System.currentTimeMillis()): Long {
        dao.liveNow()?.let { return it.id }
        val id = dao.insert(Workout(start = at, title = routine?.name.orEmpty(), routineId = routine?.id ?: 0))
        if (routine != null) {
            val last = lastSets()
            val sets = Plan.parse(routine.items).flatMapIndexed { b, p ->
                val prev = last[p.exId].orEmpty().filter { !it.warmup }
                (0 until p.sets).map { i -> val pv = prev.getOrNull(i) ?: prev.lastOrNull()
                    WSet(workoutId = id, exId = p.exId, block = b, idx = i, weight = pv?.weight ?: p.weight, reps = pv?.reps ?: p.reps, secs = pv?.secs ?: p.secs, meters = pv?.meters ?: 0.0) }
            }
            dao.insertSets(sets)
        }
        return id
    }

    /** Sets of the last finished workout that had each exercise, to show as the ghost and fill new sets. */
    suspend fun lastSets(): Map<String, List<WSet>> {
        val ws = dao.workoutList().filter { it.end > 0 }.sortedByDescending { it.start }
        val sets = dao.setList().filter { it.done }.groupBy { it.workoutId }
        val out = HashMap<String, List<WSet>>()
        for (w in ws) for ((ex, l) in sets[w.id].orEmpty().groupBy { it.exId }) if (ex !in out) out[ex] = l.sortedBy { it.idx }
        return out
    }

    suspend fun addExercise(workoutId: Long, exId: String) {
        val cur = dao.setsOfNow(workoutId); val block = (cur.maxOfOrNull { it.block } ?: -1) + 1
        val prev = lastSets()[exId].orEmpty().filter { !it.warmup }
        val ex = Catalog.of(exId)
        val n = if (prev.isNotEmpty()) prev.size.coerceAtMost(6) else if (ex.kind.timed && ex.kind != Kind.HOLD) 1 else 3
        dao.insertSets((0 until n).map { i -> val p = prev.getOrNull(i)
            WSet(workoutId = workoutId, exId = exId, block = block, idx = i, weight = p?.weight ?: 0.0, reps = p?.reps ?: 0, secs = p?.secs ?: 0, meters = p?.meters ?: 0.0) })
    }

    suspend fun addSet(workoutId: Long, block: Int, warmup: Boolean = false) {
        val l = dao.setsOfNow(workoutId).filter { it.block == block }; val last = l.lastOrNull() ?: return
        dao.insert(last.copy(id = 0, idx = (l.maxOf { it.idx }) + 1, done = false, doneAt = 0, pr = "", warmup = warmup, rpe = 0))
    }

    suspend fun finish(workoutId: Long, title: String, note: String, feel: Int, end: Long = System.currentTimeMillis()): Outcome? = lock.withLock {
        val w = dao.workout(workoutId) ?: return null
        val mine = dao.setsOfNow(workoutId)
        // sets never ticked are dropped; an empty workout is thrown away
        mine.filter { !it.done }.forEach { dao.deleteSet(it.id) }
        val done = mine.filter { it.done }
        if (done.isEmpty()) { dao.deleteWorkout(workoutId); return null }
        val all = dao.workoutList().filter { it.end > 0 && it.id != workoutId }
        val before = all.filter { it.start < w.start }
        val beforeIds = before.map { it.id }.toSet()
        val allSets = dao.setList()
        val beforeSets = allSets.filter { it.workoutId in beforeIds && it.done }
        val marks = Records.mark(Records.bests(beforeSets), done)
        val marked = done.map { it.copy(pr = marks[it.id]?.joinToString(",") { r -> r.id }.orEmpty()) }
        dao.updateSets(marked)
        val s = settings.now()
        val pets = dao.petList()
        val (out0, fed) = Engine.feed(w, marked, before, beforeSets, pets, s)
        dao.updatePets(fed)
        val leader = Engine.team(fed).firstOrNull()?.species.orEmpty()
        val finished = w.copy(end = maxOf(end, w.start + 60_000), title = title.trim(), note = note.trim(), feel = feel,
            food = out0.food, prs = out0.records.map { it.first.exId to it.second }.distinct().size, leader = leader)
        dao.update(finished)
        // eggs found
        val tally = Tally(all + finished, allSets.filter { it.workoutId != workoutId && it.workoutId in (all.map { it.id }.toSet()) } + marked, s)
        val eggs = Engine.finds(tally, fed.map { it.species }.toSet())
        val freeSlots = (0..2).filter { sl -> fed.none { it.slot == sl } }.toMutableList()
        for (e in eggs) dao.insert(newPet(e, freeSlots.removeFirstOrNull() ?: -1))
        // quests
        val week = weekStart(today())
        val wk = weekData(week, all + finished, allSets.filter { it.workoutId != workoutId } + marked)
        val questIds = if (s.questWeek == week) s.quests else Quests.deal(week)
        val doneBefore = if (s.questWeek == week) s.questsDone else emptySet()
        val newQuests = questIds.mapNotNull { Quests.byId[it] }.filter { it.id !in doneBefore && it.progress(wk) >= it.need }
        val plates = out0.plates + newQuests.sumOf { it.plates }
        dao.update(finished.copy(plates = plates))
        settings.update { it.copy(plates = it.plates + plates, questWeek = week, quests = questIds, questsDone = doneBefore + newQuests.map { q -> q.id },
            inbox = it.inbox + eggs.map { e -> "egg:$e" } + out0.hatched.map { p -> "hatch:${p.id}" }) }
        return out0.copy(plates = plates, eggs = eggs, quests = newQuests)
    }

    suspend fun weekData(monday: Long, ws: List<Workout>? = null, ss: List<WSet>? = null): Week {
        val workouts = (ws ?: dao.workoutList()).filter { it.end > 0 }
        val sets = ss ?: dao.setList()
        val inWeek = workouts.filter { dayOf(it.start) >= monday }.map { it.id }.toSet()
        val beforeIds = workouts.filter { dayOf(it.start) < monday }.map { it.id }.toSet()
        return Week(workouts.filter { it.id in inWeek }, sets.filter { it.workoutId in inWeek }, sets.filter { it.workoutId in beforeIds && it.done }, dao.bodyList(), monday)
    }

    /** Daily housekeeping: digest past days, deal this week's quests. */
    suspend fun tick() = lock.withLock {
        val s = settings.now(); val t = today()
        val days = dao.workoutList().filter { it.end > 0 }.map { dayOf(it.start) }.toSet()
        val from = if (s.digested == 0L) t - 1 else s.digested
        if (from < t - 1) {
            val (pets, news) = Engine.digest(dao.petList(), from, t, days, s)
            dao.updatePets(pets)
            settings.update { it.copy(digested = t - 1, inbox = it.inbox + news) }
        } else if (s.digested == 0L) settings.update { it.copy(digested = t - 1) }
        val week = weekStart(t)
        if (settings.now().questWeek != week) settings.update { it.copy(questWeek = week, quests = Quests.deal(week), questsDone = emptySet()) }
    }

    fun newPet(species: String, slot: Int, hatched: Long = 0, nature: Nature? = null): Pet {
        val sp = Pets.of(species); val names = if (Lang.ru) sp.names else sp.namesEn
        return Pet(species = species, name = names.random(), nature = (nature ?: Nature.entries.random()).id, found = today(), hatched = hatched,
            slot = slot, shiny = Random.nextInt(16) == 0)
    }

    suspend fun setSlot(p: Pet, slot: Int) {
        val pets = dao.petList()
        val other = pets.firstOrNull { it.slot == slot && it.id != p.id }
        if (other != null) dao.update(other.copy(slot = p.slot))
        dao.update(p.copy(slot = slot))
    }

    suspend fun buy(id: String): Boolean {
        val it = Shop.byId[id] ?: return false; val s = settings.now()
        if (s.plates < it.price || id in s.items) return false
        settings.update { st -> st.copy(plates = st.plates - it.price, items = st.items + id) }
        return true
    }

    suspend fun saveOwn(e: OwnEx): String {
        val id = if (e.id == 0L) dao.insert(e) else { dao.update(e); e.id }
        loadOwn(); return "u$id"
    }

    /** Fetches the weather for the window when it is older than half an hour; guesses the town from the time zone once. */
    suspend fun refreshOutside(force: Boolean = false) = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching {
        var s = settings.now()
        if (s.lat.isNaN() && !s.guessed) {
            val t = OutsideSource.guess(Lang.ru)
            settings.update { it.copy(guessed = true, town = t?.name ?: it.town, lat = t?.lat ?: it.lat, lon = t?.lon ?: it.lon) }
            s = settings.now()
        }
        if (s.lat.isNaN()) return@runCatching
        val old = Outside.of(s.outside)
        if (!force && old != null && System.currentTimeMillis() - old.at < 30 * 60_000L) return@runCatching
        val o = OutsideSource.fetch(s.lat, s.lon)
        settings.update { it.copy(outside = o.json()) }
    } }

    suspend fun addBody(b: Body) { val old = dao.bodyList().firstOrNull { it.day == b.day }
        if (old != null) dao.update(b.copy(id = old.id, photo = b.photo.ifEmpty { old.photo })) else dao.insert(b)
        if (b.weight > 0) settings.update { it.copy(bodyweight = b.weight) } }
}

fun OwnEx.toEx() = Ex("u$id", name, name, Kind.of(kind.firstOrNull() ?: 'W'), primary.split(',').mapNotNull { Muscle.of(it) },
    secondary.split(',').mapNotNull { Muscle.of(it) }, if (equip.startsWith(OWN_GEAR)) Equip.OTHER else Equip.of(equip), Cat.of(cat), own = true,
    gear = if (equip.startsWith(OWN_GEAR)) equip.removePrefix(OWN_GEAR) else "")

/** OwnEx.equip of the user's own equipment: «own:Тренажёр Смита». */
const val OWN_GEAR = "own:"

class PumplingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = AppContainer(this)
        container.scope.launch(Dispatchers.Default) { Catalog.builtin(this@PumplingApp); container.loadOwn()
            Units.lb = container.settings.now().lb }
    }
}

val Context.container: AppContainer get() = (applicationContext as PumplingApp).container


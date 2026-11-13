package app.pumpling.ui.workout

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ColumnScope
import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pumpling.container
import app.pumpling.data.WSet
import app.pumpling.domain.Catalog
import app.pumpling.domain.Ex
import app.pumpling.domain.Kind
import app.pumpling.domain.clock
import app.pumpling.domain.fmt2
import app.pumpling.domain.setLine
import app.pumpling.domain.toKg
import app.pumpling.domain.tr
import app.pumpling.domain.unit
import app.pumpling.domain.w
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.art.PetView
import app.pumpling.ui.art.Pose
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Dot
import app.pumpling.ui.components.Field
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Popup
import app.pumpling.ui.components.Sheet
import app.pumpling.ui.components.card
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T
import app.pumpling.work.Rest
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The live workout, or with [editId] a finished one opened for changes: the same blocks and sets,
 * without the rest timer and the team, and «Save» instead of «Finish».
 */
@Composable
fun WorkoutScreen(d: Data, nav: Nav, editId: Long = 0) {
    val ctx = LocalContext.current; val c = ctx.container
    val edit = editId != 0L
    val w = if (edit) d.workouts.firstOrNull { it.id == editId } else d.live
    // leaving an edit keeps what was changed and drops the sets left unticked
    fun leave() { if (edit && w != null) c.write { saveEdit(w.id, w.title, w.note, w.feel, w.start, w.end) }; nav.pop() }
    BackHandler { leave() }
    if (w == null) { LaunchedEffect(Unit) { delay(600); if (edit || c.dao.liveNow() == null) nav.pop() }; Box(Modifier.fillMaxSize().background(C.bg)); return }
    val sets = d.setsBy[w.id].orEmpty()
    val blocks = sets.groupBy { it.block }.toSortedMap()
    val last by produceState<Map<String, List<WSet>>>(emptyMap(), d.finished.size) { value = c.lastSets() }
    val bests by produceState<Map<String, app.pumpling.domain.Best>>(emptyMap(), d.finished.size) { value = app.pumpling.domain.Records.bests(d.doneSets) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); if (Rest.until in 1..now) Rest.done(ctx); delay(250) } }
    var finishing by remember { mutableStateOf(false) }
    var plateCalc by remember { mutableStateOf<Double?>(null) }
    var menu by remember { mutableStateOf<Int?>(null) }
    var setMenu by remember { mutableStateOf<WSet?>(null) }
    var cancel by remember { mutableStateOf(false) }
    // A group class shows a timer; «also log exercises» opens the usual list.
    val isClass = w.classOf.isNotEmpty() && !edit
    var listing by remember(w.id) { mutableStateOf(false) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { if (!edit && Build.VERSION.SDK_INT >= 33) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { leave() }, contentAlignment = Alignment.Center) { Icon(if (edit) Icons.BACK else Icons.DOWN, C.ink, 24.dp) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(if (edit) tr("Правка", "Editing") + " · " + w.title.ifEmpty { tr("тренировка", "workout") } else w.title.ifEmpty { tr("Тренировка", "Workout") }, style = T.h2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text((if (w.manual || edit) app.pumpling.domain.fmtDayYear(app.pumpling.domain.dayOf(w.start)) else clock(((now - w.start) / 1000).toInt())) + if (isClass && !listing) "" else " · " + tr("${sets.count { it.done }} подх.", "${sets.count { it.done }} sets"), style = T.small)
            }
            Btn(if (edit) tr("Сохранить", "Save") else tr("Готово", "Finish"), small = true, color = C.ok) { finishing = true }
        }
        if (!edit) TeamStrip(d, sets)
        if (isClass && !listing) ClassBody(w, d, now, onList = { listing = true }, onCancel = { cancel = true }) else
        LazyColumn(Modifier.weight(1f).fillMaxWidth().imePadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 120.dp)) {
            if (edit) item {
                Text(tr("Изменения сохраняются сразу. Неотмеченные подходы уберутся. Рекорды пересчитаются, а съеденное питомцами останется как было.",
                    "Changes are saved as you go. Unticked sets will be dropped. Records are worked out again; what the pets ate stays as it was."),
                    style = T.small, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.card2).padding(12.dp))
                Spacer(Modifier.height(10.dp))
            } else item { app.pumpling.ui.components.Hint("live", d.s.seen, Icons.CHECK, tr("Как записывать", "How to log"), tr("Галочка – подход сделан, начнётся отдых. Нажмите на номер подхода: разминка, RPE или удаление. Строка «в прошлый раз» подскажет, к чему стремиться, а звёздочка – что будет рекорд.",
                "The tick marks a set done and starts the rest. Tap the set number for warm-up, RPE or delete. «Last time» shows what to beat; a star means a record."), Modifier.padding(bottom = 10.dp)) }
            if (blocks.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    d.leader?.let { PetView(it, 150.dp, Pose.IDLE) }
                    Text(tr("Питомец ждёт. Добавьте первое упражнение.", "Your pet is waiting. Add the first exercise."), style = T.body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                }
            }
            items(blocks.entries.toList(), key = { "b${it.key}" }) { (b, list) ->
                val ex = Catalog.of(list.first().exId)
                Block(w.id, b, ex, list, last[ex.id].orEmpty(), bests[ex.id], d, live = !edit, doneAt = if (edit) w.end else 0L, onMenu = { menu = b }, onSet = { setMenu = it }, onInfo = { nav.push(Screen.Exercise(ex.id)) })
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(tr("+ Упражнение", "+ Exercise"), Modifier.weight(1f)) { nav.push(Screen.Picker(into = if (edit) w.id else 0)) }
                }
                if (!edit) Text(tr("Отмените тренировку, если она началась случайно", "Cancel the workout if it was started by mistake"), style = T.small.copy(color = C.bad),
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).clickable { cancel = true }, textAlign = TextAlign.Center)
            }
        }
    }
        if (!edit) Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp)) { RestBar(d, now) }
    }
    menu?.let { b -> val l = blocks[b].orEmpty(); if (l.isEmpty()) { menu = null; return@let }
        Sheet({ menu = null }) {
            Text(Catalog.of(l.first().exId).name, style = T.h2)
            Spacer(Modifier.height(12.dp))
            Ghost(tr("Добавить разминочный подход", "Add a warm-up set"), Modifier.fillMaxWidth()) { c.write { addSet(w.id, b, warmup = true) }; menu = null }
            Spacer(Modifier.height(8.dp))
            Ghost(tr("Как делать, мышцы и история", "How to, muscles and history"), Modifier.fillMaxWidth()) { menu = null; nav.push(Screen.Exercise(l.first().exId)) }
            Spacer(Modifier.height(8.dp))
            val split = l.any { it.side != 0 }
            Ghost(if (split) tr("Обе стороны вместе", "Both sides together") else tr("Левая и правая отдельно", "Left and right apart"), Modifier.fillMaxWidth()) { c.write { split(w.id, b, !split) }; menu = null }
            if (!split) Text(tr("Для упражнений на одну руку или ногу: каждый подход станет парой Л и П.", "For one-arm or one-leg work: each set becomes an L and R pair."),
                style = T.tiny, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            val top = l.maxOf { it.weight }
            if (Catalog.of(l.first().exId).equip == app.pumpling.domain.Equip.BAR && top > d.s.bar) {
                Spacer(Modifier.height(8.dp)); Ghost(tr("Блины на сторону для ${w(top)} ${unit()}", "Plates per side for ${w(top)} ${unit()}"), Modifier.fillMaxWidth()) { menu = null; plateCalc = top } }
            Spacer(Modifier.height(8.dp))
            Ghost(tr("Поднять выше", "Move up"), Modifier.fillMaxWidth()) { c.write { swap(w.id, b, -1) }; menu = null }
            Spacer(Modifier.height(8.dp))
            Ghost(tr("Убрать упражнение", "Remove exercise"), Modifier.fillMaxWidth(), color = C.bad) { c.write { dao.deleteBlock(w.id, b) }; menu = null }
        }
    }
    plateCalc?.let { kg -> PlatePopup(kg, d) { plateCalc = null } }
    setMenu?.let { s0 -> val s = sets.firstOrNull { it.id == s0.id } ?: run { setMenu = null; return@let }
        Sheet({ setMenu = null }) {
            Text(tr("Подход", "Set") + " · " + Catalog.of(s.exId).name, style = T.h2)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(tr("Разминочный", "Warm-up"), style = T.bodyB); Text(tr("Не идёт в рекорды и тоннаж, питомцу – треть порции", "Not counted for records or tonnage; a third of a portion"), style = T.small) }
                app.pumpling.ui.components.Chip(if (s.warmup) tr("да", "yes") else tr("нет", "no"), s.warmup) { c.write { dao.update(s.copy(warmup = !s.warmup)) } }
            }
            Text(tr("Сторона", "Side"), style = T.bodyB, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to tr("обе", "both"), 1 to tr("левая", "left"), 2 to tr("правая", "right")).forEach { (v, t) ->
                    app.pumpling.ui.components.Chip(t, s.side == v) { c.write { dao.update(s.copy(side = v)) } } } }
            Text(tr("Насколько тяжело (RPE)", "How hard (RPE)"), style = T.bodyB, modifier = Modifier.padding(top = 14.dp))
            Text(tr("10 – больше ни разу, 8 – оставалось два повтора", "10 – not one more, 8 – two reps left"), style = T.small)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 6, 7, 8, 9, 10).forEach { r -> app.pumpling.ui.components.Chip(if (r == 0) "–" else "$r", s.rpe == r) { c.write { dao.update(s.copy(rpe = r)) } } } }
            Spacer(Modifier.height(16.dp))
            Ghost(tr("Удалить подход", "Delete the set"), Modifier.fillMaxWidth(), color = C.bad) { c.write { dao.deleteSet(s.id) }; setMenu = null }
        }
    }
    if (cancel) app.pumpling.ui.components.Popup({ cancel = false }) {
        Text(tr("Отменить тренировку?", "Cancel the workout?"), style = T.h2)
        Text(tr("Все подходы из неё удалятся, питомцы ничего не получат.", "All its sets are deleted and the pets get nothing."), style = T.body, modifier = Modifier.padding(vertical = 10.dp))
        Btn(tr("Отменить тренировку", "Cancel it"), Modifier.fillMaxWidth(), color = C.bad) { c.write { dao.deleteSetsOf(w.id); dao.deleteWorkout(w.id); Rest.stop(app) }; cancel = false; nav.pop() }
        Spacer(Modifier.height(8.dp)); Ghost(tr("Продолжить тренировку", "Keep training"), Modifier.fillMaxWidth()) { cancel = false }
    }
    if (finishing) { if (edit) EditSheet(w, nav) { finishing = false } else FinishSheet(d, nav) { finishing = false } }
}

/** Moves an exercise block up or down in the live workout. */
private suspend fun app.pumpling.AppContainer.swap(wid: Long, b: Int, dir: Int) {
    val all = dao.setsOfNow(wid); val order = all.map { it.block }.distinct().sorted()
    val i = order.indexOf(b); val j = i + dir; if (j !in order.indices) return
    val other = order[j]
    dao.updateSets(all.filter { it.block == b || it.block == other }.map { it.copy(block = if (it.block == b) other else b) })
}

@Composable
private fun RestBar(d: Data, now: Long) {
    val ctx = LocalContext.current
    if (Rest.until <= now) return
    val left = ((Rest.until - now) / 1000).toInt() + 1
    Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp).fillMaxWidth().card(22.dp, C.ink).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        d.leader?.let { PetView(it, 54.dp, Pose.SLEEP) }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(tr("Отдых", "Rest"), style = T.small.copy(color = C.bg))
            Text(clock(left), style = T.num.copy(color = C.bg))
            app.pumpling.ui.components.Meter(1f - (Rest.until - now).toFloat() / Rest.length.coerceAtLeast(1), Modifier.padding(top = 4.dp), color = C.acc, h = 5.dp, track = C.mute.copy(alpha = .4f))
        }
        Spacer(Modifier.width(8.dp))
        Text("−15", style = T.smallB.copy(color = C.bg), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { Rest.add(ctx, -15) }.padding(8.dp))
        Text("+15", style = T.smallB.copy(color = C.bg), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { Rest.add(ctx, 15) }.padding(8.dp))
        Box(Modifier.size(36.dp).clip(CircleShape).clickable { Rest.stop(ctx) }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, C.bg, 20.dp) }
    }
}

@Composable
private fun Block(wid: Long, b: Int, ex: Ex, list: List<WSet>, last: List<WSet>, best: app.pumpling.domain.Best?, d: Data, live: Boolean, doneAt: Long, onMenu: () -> Unit, onSet: (WSet) -> Unit, onInfo: () -> Unit) {
    val c = LocalContext.current.container
    Column(Modifier.padding(bottom = 12.dp).fillMaxWidth().card(20.dp).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app.pumpling.ui.art.ExThumb(ex, 54.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(ex.name, style = T.h3, maxLines = 2)
                Text(ex.primary.joinToString(", ") { it.label.lowercase() }, style = T.tiny.copy(color = app.pumpling.ui.components.PRIMARY), maxLines = 1)
            }
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onMenu), contentAlignment = Alignment.Center) { Icon(Icons.DOTS, C.mute, 20.dp) }
        }
        val ghost = last.filter { !it.warmup }
        if (!live) Unit else if (ghost.isNotEmpty()) Text(tr("В прошлый раз: ", "Last time: ") + ghost.joinToString(", ") { setLine(ex, it) } + hint(ex, ghost), style = T.small, modifier = Modifier.padding(top = 2.dp))
        else Text(tr("Первый раз – рекордом будет следующий", "First time – the next one can be a record"), style = T.small, modifier = Modifier.padding(top = 2.dp))
        if (ex.kind == Kind.ASSIST || ex.kind == Kind.BODY || ex.kind == Kind.HOLD) {
            var more by remember { mutableStateOf(false) }
            Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.card2).clickable { more = !more }.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top) {
                Text("?", style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(end = 8.dp))
                Text(if (more) ex.kind.help(ex) else when (ex.kind) {
                    Kind.ASSIST -> tr("Пишите вес помощи гравитрона: меньше помощь – сильнее вы", "Write the machine's help: less help – stronger you")
                    Kind.BODY -> tr("Вес – только если добавляете груз на пояс", "Weight only if you add a load on a belt")
                    else -> tr("Время удержания в секундах", "Hold time in seconds") } + if (more) "" else tr("  ·  подробнее", "  ·  more"), style = T.tiny.copy(color = C.ink))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Подх.", "Set"), style = T.tiny, modifier = Modifier.width(40.dp))
            heads(ex).forEach { Text(it, style = T.tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
            Spacer(Modifier.width(48.dp))
        }
        // a left and a right set share a number: «1Л», «1П»
        var n = 0; var prevSide = 0
        list.sortedBy { it.idx }.forEach { s ->
            if (!s.warmup && !(s.side == 2 && prevSide == 1)) n++
            if (!s.warmup) prevSide = s.side
            SetRow(s, ex, if (s.warmup) tr("Р", "W") else "$n" + app.pumpling.domain.sideMark(s.side), best, d, live, doneAt, onSet) }
        Text(tr("+ подход", "+ set"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).clickable { c.write { addSet(wid, b) } }.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

/** A nudge for progressive overload after the ghost: «→ 62,5 × 8». */
private fun hint(ex: Ex, ghost: List<WSet>): String {
    if (ex.kind != Kind.WEIGHT) return ""
    val top = ghost.maxBy { it.weight }; if (top.weight <= 0 || top.reps <= 0) return ""
    val allHit = ghost.all { it.reps >= top.reps }
    return if (allHit && top.reps >= 10) tr("  → попробуйте ${w(top.weight + 2.5)}\u00a0×\u00a0${top.reps - 2}", "  → try ${w(top.weight + 2.5)}\u00a0×\u00a0${top.reps - 2}")
    else tr("  → попробуйте ${w(top.weight)}\u00a0×\u00a0${top.reps + 1}", "  → try ${w(top.weight)}\u00a0×\u00a0${top.reps + 1}")
}

private fun heads(ex: Ex) = when (ex.kind) {
    Kind.WEIGHT -> listOf(unit(), tr("повт.", "reps"))
    Kind.BODY -> listOf(tr("+вес", "+weight"), tr("повт.", "reps"))
    Kind.ASSIST -> listOf(tr("помощь, ${unit()}", "help, ${unit()}"), tr("повт.", "reps"))
    Kind.HOLD -> listOf(unit(), tr("сек", "sec"))
    Kind.CARDIO -> listOf(tr("км", "km"), tr("мин", "min"))
    Kind.MINUTES -> listOf(tr("мин", "min"))
}

@Composable
private fun SetRow(s: WSet, ex: Ex, label: String, best: app.pumpling.domain.Best?, d: Data, live: Boolean, doneAt: Long, onSet: (WSet) -> Unit) {
    val ctx = LocalContext.current; val c = ctx.container
    fun num(v: Double) = if (v == 0.0) "" else fmt2(v).replace(',', '.').let { if (app.pumpling.domain.Lang.ru) it.replace('.', ',') else it }
    var a by remember(s.id) { mutableStateOf(when (ex.kind) { Kind.CARDIO -> num(s.meters / 1000); Kind.MINUTES -> if (s.secs > 0) "${s.secs / 60}" else ""; else -> if (s.weight > 0) w(s.weight) else "" }) }
    var b by remember(s.id) { mutableStateOf(when (ex.kind) { Kind.HOLD -> if (s.secs > 0) "${s.secs}" else ""; Kind.CARDIO -> if (s.secs > 0) num(s.secs / 60.0) else ""; else -> if (s.reps > 0) "${s.reps}" else "" }) }
    fun p(x: String) = x.replace(',', '.').toDoubleOrNull() ?: 0.0
    fun build(): WSet = when (ex.kind) {
        Kind.WEIGHT, Kind.BODY, Kind.ASSIST -> s.copy(weight = toKg(p(a)), reps = p(b).toInt())
        Kind.HOLD -> s.copy(weight = toKg(p(a)), secs = p(b).toInt())
        Kind.CARDIO -> s.copy(meters = p(a) * 1000, secs = (p(b) * 60).roundToInt())
        Kind.MINUTES -> s.copy(secs = (p(a) * 60).roundToInt())
    }
    fun save() = c.write { dao.update(build()) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val cur = build()
    val willRecord = live && !s.warmup && best != null && best.sets > 0 && best.beats(ex, cur).isNotEmpty()
    val bg = if (s.done) C.okSoft else Color.Transparent
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(40.dp).height(40.dp).clip(RoundedCornerShape(10.dp)).clickable { onSet(s) }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = T.smallB.copy(color = if (s.warmup) C.gold else C.ink))
                if (s.rpe > 0) Text("@${s.rpe}", style = T.tiny.copy(color = C.acc)) }
            if (willRecord && !s.done) Box(Modifier.align(Alignment.TopEnd).padding(end = 4.dp)) { Icon(Icons.STAR, C.gold, 10.dp) }
        }
        Field(a, { a = it; save() }, if (ex.kind == Kind.BODY) "0" else "–", Modifier.weight(1f).padding(horizontal = 3.dp), number = true, style = T.numS, center = true)
        if (ex.kind != Kind.MINUTES) Field(b, { b = it; save() }, "–", Modifier.weight(1f).padding(horizontal = 3.dp), number = true, style = T.numS, center = true)
        Box(Modifier.padding(start = 6.dp).size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (s.done) C.ok else C.card2).clickable {
            val now = if (live) System.currentTimeMillis() else doneAt; focus.clearFocus()
            val upd = build().let { if (it.done) it.copy(done = false, doneAt = 0) else it.copy(done = true, doneAt = now) }
            // an empty set takes the numbers it showed as a hint
            c.write { dao.update(upd) }
            if (live && !s.done && (ex.kind == Kind.WEIGHT || ex.kind == Kind.BODY || ex.kind == Kind.ASSIST || ex.kind == Kind.HOLD)) Rest.start(ctx, d.s.rest)
        }, contentAlignment = Alignment.Center) { Icon(Icons.CHECK, if (s.done) Color.White else C.mute, 22.dp) }
    }
}

@Composable
private fun PlatePopup(kg: Double, d: Data, onClose: () -> Unit) {
    Popup(onClose) {
        Text(tr("Блины на одну сторону", "Plates per side"), style = T.h2)
        Text("${w(kg)} ${unit()} · ${tr("гриф", "bar")} ${w(d.s.bar)}", style = T.small, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
        var side = (kg - d.s.bar) / 2
        val plates = d.s.plateSet.split(',').mapNotNull { it.toDoubleOrNull() }.sortedDescending()
        val out = ArrayList<Double>()
        for (p in plates) while (side >= p - 1e-6) { out += p; side -= p }
        if (out.isEmpty()) Text(tr("Только гриф", "Just the bar"), style = T.body)
        Row(Modifier.fillMaxWidth().height(110.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(40.dp).height(10.dp).background(C.mute))
            out.forEach { p -> val h = (40 + p * 2.6).coerceAtMost(105.0)
                Box(Modifier.padding(horizontal = 1.dp).width((10 + p / 3).dp).height(h.dp).clip(RoundedCornerShape(4.dp)).background(plateColor(p))) }
            Box(Modifier.width(20.dp).height(10.dp).background(C.mute))
        }
        Text(out.groupingBy { it }.eachCount().entries.joinToString("  ") { "${w(it.key)}×${it.value}" }, style = T.bodyB)
        if (side > 0.01) Text(tr("Не хватает ${w(side * 2)} ${unit()} мелочью", "Short by ${w(side * 2)} ${unit()}"), style = T.small.copy(color = C.bad))
        Spacer(Modifier.height(14.dp)); Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth(), onClick = onClose)
    }
}

private fun plateColor(p: Double) = when { p >= 25 -> Color(0xFFD2484E); p >= 20 -> Color(0xFF2F6FD0); p >= 15 -> Color(0xFFE5B81E); p >= 10 -> Color(0xFF3E9C6A); p >= 5 -> Color(0xFFEDEDED); else -> Color(0xFF8A8A8A) }

/** A group class going on: its name, the clock, the pet, and nothing to fill in (#10). */
@Composable
private fun ColumnScope.ClassBody(w: app.pumpling.data.Workout, d: Data, now: Long, onList: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        app.pumpling.ui.art.ExThumb(Catalog.of(w.classOf), 96.dp)
        Text(Catalog.of(w.classOf).name, style = T.body, modifier = Modifier.padding(top = 8.dp))
        Text(clock(((now - w.start) / 1000).toInt()), style = T.num.copy(fontSize = 56.sp), modifier = Modifier.padding(top = 6.dp))
        d.leader?.let { PetView(it, 140.dp, Pose.WORK) }
        Text(tr("Занимайтесь – записывать ничего не нужно. Когда закончите, нажмите «Готово».", "Just train – there's nothing to log. Tap «Finish» when it's over."),
            style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Ghost(tr("Записать и упражнения", "Log exercises too"), Modifier.padding(top = 18.dp)) { onList() }
        Text(tr("Отменить, если началось случайно", "Cancel if started by mistake"), style = T.small.copy(color = C.bad),
            modifier = Modifier.padding(top = 18.dp).clickable { onCancel() }, textAlign = TextAlign.Center)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FinishSheet(d: Data, nav: Nav, onClose: () -> Unit) {
    val ctx = LocalContext.current; val c = ctx.container
    val w = d.live ?: return
    var title by remember { mutableStateOf(w.title.ifEmpty { guessTitle(d.setsBy[w.id].orEmpty()) }) }
    var note by remember { mutableStateOf(w.note) }
    var feel by remember { mutableStateOf(0) }
    var mins by remember { mutableStateOf(60) }
    val isClass = w.classOf.isNotEmpty()
    var hard by remember { mutableStateOf(0) }
    var muscles by remember { mutableStateOf(Catalog.of(w.classOf.ifEmpty { "group_class" }).muscles.toSet()) }
    val done = d.setsBy[w.id].orEmpty().count { it.done } + if (isClass) 1 else 0
    Sheet(onClose) {
        Text(tr("Закончить тренировку?", "Finish the workout?"), style = T.h2)
        if (done == 0) Text(tr("Ни один подход не отмечен – тренировка не сохранится.", "No set is ticked, so nothing will be saved."), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 4.dp))
        else if (!isClass) Text(tr("Неотмеченные подходы уберутся.", "Unticked sets will be dropped."), style = T.small, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))
        Field(title, { title = it }, tr("Название", "Name"), Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Field(note, { note = it }, tr("Заметка: как спалось, что болело…", "A note: sleep, aches…"), Modifier.fillMaxWidth(), single = false)
        Text(tr("Как прошло?", "How was it?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("😫", "😕", "🙂", "😄", "🔥").forEachIndexed { i, e ->
                Box(Modifier.size(48.dp).clip(CircleShape).background(if (feel == i + 1) C.accSoft else C.card2).clickable { feel = i + 1 }, contentAlignment = Alignment.Center) { Text(e, style = T.h2) } }
        }
        if (isClass) {
            Text(tr("Насколько было тяжело?", "How hard was it?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp))
            Text(tr("1 – легко, 10 – на пределе", "1 – easy, 10 – all out"), style = T.small)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..10).forEach { n ->
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp)).background(if (hard == n) C.acc else C.card2).clickable { hard = n }, contentAlignment = Alignment.Center) {
                        Text("$n", style = T.smallB.copy(color = if (hard == n) Color.White else C.ink)) } } }
            Text(tr("Что работало?", "What did it work?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                app.pumpling.domain.Muscle.entries.forEach { m -> app.pumpling.ui.components.Chip(m.label, m in muscles) { muscles = if (m in muscles) muscles - m else muscles + m } } }
        }
        if (w.manual) {
            Text(tr("Сколько длилась?", "How long was it?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(30, 45, 60, 75, 90, 120).forEach { m -> app.pumpling.ui.components.Chip("$m", mins == m) { mins = m } } }
        }
        Spacer(Modifier.height(16.dp))
        Btn(tr("Закончить", "Finish"), Modifier.fillMaxWidth(), color = C.ok) {
            onClose(); Rest.stop(ctx)
            val picked = app.pumpling.domain.Muscle.entries.filter { m -> m in muscles }
            c.write { val end = if (w.manual) w.start + mins * 60_000L else System.currentTimeMillis()
                if (isClass) closeClass(w.id, hard, picked, end)
                val out = finish(w.id, title, note, feel, end)
                nav.pop(); if (out != null) nav.push(Screen.Result(w.id, out)) else say(tr("Пустая тренировка не сохранена", "An empty workout isn't saved")) }
        }
    }
}

/** Saving an edited workout: its name, note and feel, and when it was and how long it took. */
@Composable
private fun EditSheet(w: app.pumpling.data.Workout, nav: Nav, onClose: () -> Unit) {
    val c = LocalContext.current.container
    var title by remember { mutableStateOf(w.title) }
    var note by remember { mutableStateOf(w.note) }
    var feel by remember { mutableStateOf(w.feel) }
    val zone = java.time.ZoneId.systemDefault()
    val t0 = java.time.Instant.ofEpochMilli(w.start).atZone(zone)
    var day by remember { mutableLongStateOf(t0.toLocalDate().toEpochDay()) }
    var mins0 by remember { mutableStateOf(t0.hour * 60 + t0.minute) }
    var mins by remember { mutableStateOf(maxOf(1L, (w.end - w.start) / 60_000).toInt()) }
    Sheet(onClose) {
        Text(tr("Сохранить изменения", "Save the changes"), style = T.h2)
        Spacer(Modifier.height(12.dp))
        Field(title, { title = it }, tr("Название", "Name"), Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Field(note, { note = it }, tr("Заметка", "A note"), Modifier.fillMaxWidth(), single = false)
        Text(tr("Как прошло?", "How was it?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("😫", "😕", "🙂", "😄", "🔥").forEachIndexed { i, e ->
                Box(Modifier.size(48.dp).clip(CircleShape).background(if (feel == i + 1) C.accSoft else C.card2).clickable { feel = if (feel == i + 1) 0 else i + 1 }, contentAlignment = Alignment.Center) { Text(e, style = T.h2) } }
        }
        Text(tr("Когда", "When"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Stepper(app.pumpling.domain.fmtDayYear(day), { day = minOf(app.pumpling.domain.today(), day + it) })
        Spacer(Modifier.height(6.dp))
        Stepper(tr("начало ", "start ") + "%d:%02d".format(mins0 / 60, mins0 % 60), { mins0 = (mins0 + it * 15 + 1440) % 1440 })
        Spacer(Modifier.height(6.dp))
        Stepper(tr("длилась $mins мин", "$mins min long"), { mins = (mins + it * 5).coerceIn(5, 600) })
        Spacer(Modifier.height(16.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), color = C.ok) {
            val start = java.time.LocalDate.ofEpochDay(day).atStartOfDay(zone).toInstant().toEpochMilli() + mins0 * 60_000L
            onClose()
            c.write { if (saveEdit(w.id, title, note, feel, start, start + mins * 60_000L)) say(tr("Тренировка сохранена", "Workout saved")) else say(tr("Подходов не осталось – тренировка удалена", "No sets left – the workout is deleted")) }
            nav.pop()
        }
    }
}

/** «‹ value ›»: a value stepped down and up. */
@Composable
private fun Stepper(text: String, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.card2), verticalAlignment = Alignment.CenterVertically) {
        Text("−", style = T.h2.copy(color = C.acc), modifier = Modifier.clickable { onStep(-1) }.padding(horizontal = 18.dp, vertical = 8.dp))
        Text(text, style = T.bodyB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("+", style = T.h2.copy(color = C.acc), modifier = Modifier.clickable { onStep(1) }.padding(horizontal = 18.dp, vertical = 8.dp))
    }
}

private fun guessTitle(sets: List<WSet>): String {
    val cats = sets.filter { it.done }.groupingBy { Catalog.of(it.exId).cat }.eachCount()
    val top = cats.maxByOrNull { it.value }?.key ?: return tr("Тренировка", "Workout")
    return when (top) {
        app.pumpling.domain.Cat.LEGS -> tr("Ноги", "Legs"); app.pumpling.domain.Cat.PUSH -> tr("Жимы", "Push"); app.pumpling.domain.Cat.PULL -> tr("Спина и тяги", "Pull")
        app.pumpling.domain.Cat.CARDIO -> tr("Кардио", "Cardio"); app.pumpling.domain.Cat.FLEX -> tr("Растяжка", "Stretching"); else -> top.label
    } + if (cats.size >= 3) tr(" и всё понемногу", " and more") else ""
}


/** The team training along: the leader works, the companions cheer, food so far. */
@Composable
private fun TeamStrip(d: Data, sets: List<WSet>) {
    if (d.team.isEmpty()) return
    val done = sets.filter { it.done }
    Row(Modifier.padding(horizontal = 14.dp, vertical = 2.dp).fillMaxWidth().card(18.dp).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        d.leader?.let { PetView(it, 70.dp, if (done.isEmpty()) Pose.IDLE else Pose.WORK) }
        d.team.filter { it.slot != 0 }.forEach { PetView(it, 46.dp, if (done.isEmpty()) Pose.IDLE else Pose.CHEER) }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(if (done.isEmpty()) tr("Команда разминается", "The team is warming up") else tr("Команда тренируется вместе с вами", "The team trains with you"), style = T.smallB)
            Text(tr("Подходов: ${done.count { !it.warmup }} · упражнений: ${done.map { it.exId }.distinct().size}", "Sets: ${done.count { !it.warmup }} · exercises: ${done.map { it.exId }.distinct().size}"), style = T.tiny)
        }
    }
}

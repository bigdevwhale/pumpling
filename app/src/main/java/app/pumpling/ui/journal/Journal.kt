package app.pumpling.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import app.pumpling.ui.art.Art
import app.pumpling.domain.stage
import app.pumpling.domain.fmtDayShort
import app.pumpling.domain.weekStart
import app.pumpling.domain.Cat
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import app.pumpling.Plan
import app.pumpling.container
import app.pumpling.data.Routine
import app.pumpling.data.Workout
import app.pumpling.domain.Catalog
import app.pumpling.domain.Kind
import app.pumpling.domain.Rec
import app.pumpling.domain.Stats
import app.pumpling.domain.Span
import app.pumpling.domain.count
import app.pumpling.domain.dayOf
import app.pumpling.domain.dur
import app.pumpling.domain.fmtDayYear
import app.pumpling.domain.monthName
import app.pumpling.domain.setLine
import app.pumpling.domain.tonnage
import app.pumpling.domain.today
import app.pumpling.domain.tr
import app.pumpling.domain.weekday
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.Chip
import app.pumpling.ui.components.Dot
import app.pumpling.ui.components.Empty
import app.pumpling.ui.components.Field
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.Section
import app.pumpling.ui.components.Segments
import app.pumpling.ui.components.Sheet
import app.pumpling.ui.components.TileGrid
import app.pumpling.ui.components.card
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.ShareReq
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T
import java.time.LocalDate
import java.time.ZoneId

val FEELS = listOf("😫", "😕", "🙂", "😄", "🔥")

@Composable
fun JournalScreen(d: Data, nav: Nav) {
    var tab by remember { mutableStateOf(0) }
    var past by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 14.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Журнал", "Log"), style = T.title, modifier = Modifier.weight(1f))
            if (tab == 0) Text(tr("+ прошедшая", "+ past one"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { past = true }.padding(8.dp))
        }
        Segments(listOf(tr("История", "History"), tr("Программы", "Routines"), tr("Свои", "Own")), tab, Modifier.padding(vertical = 10.dp)) { tab = it }
        when (tab) {
            0 -> History(d, nav)
            1 -> Routines(d, nav)
            else -> OwnList(d, nav)
        }
    }
    if (past) PastSheet(d, nav) { past = false }
}

@Composable
private fun History(d: Data, nav: Nav) {
    val st = remember(d.workouts, d.sets) { Stats(d.workouts, d.sets, d.bodies, d.s, Span.of(Span.ALL)) }
    var filter by remember { mutableStateOf(0) }
    if (d.finished.isEmpty()) { Empty(tr("Здесь появятся тренировки. Начните первую кнопкой «+» внизу.", "Workouts show up here. Start one with «+» below.")); return }
    fun kindOf(w: Workout): Int { val l = d.setsBy[w.id].orEmpty().filter { it.done }; val cardio = l.count { Catalog.of(it.exId).cat == Cat.CARDIO }
        return if (cardio * 2 >= l.size) 2 else 1 }
    val list = d.finished.sortedByDescending { it.start }.filter { w -> when (filter) { 1 -> kindOf(w) == 1; 2 -> kindOf(w) == 2; 3 -> w.prs > 0; else -> true } }
    val byWeek = list.groupBy { weekStart(dayOf(it.start)) }
    // this week and the last one open; older weeks fold into their header (any filter opens everything)
    val now = weekStart(today())
    var opened by remember { mutableStateOf(setOf(now, now - 7)) }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)) {
        item(key = "hint") { app.pumpling.ui.components.Hint("journal", d.s.seen, Icons.LOG, tr("Журнал тренировок", "Your workouts"), tr("Все тренировки по неделям. Цветная полоска показывает, что тренировали, кубок – рекорды. «+ прошедшая» – записать тренировку задним числом.",
            "Every workout by week. The coloured stripe shows what was trained, the cup – records. «+ past one» logs a workout afterwards."), Modifier.padding(bottom = 10.dp)) }
        item(key = "glance") { Glance(d) }
        item(key = "filter") {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(tr("Все", "All"), tr("Силовые", "Strength"), tr("Кардио", "Cardio"), tr("С рекордами", "With records")).forEachIndexed { i, l -> Chip(l, filter == i) { filter = i } } }
        }
        if (list.isEmpty()) item { Empty(tr("Таких тренировок пока нет", "No such workouts yet")) }
        byWeek.forEach { (mon, ws) ->
            item(key = "w$mon") {
                val work = ws.flatMap { w -> d.setsBy[w.id].orEmpty().filter { it.done && !it.warmup } }
                val days = ws.map { dayOf(it.start) }.toSet(); val met = days.size >= d.s.weekGoal
                val open = filter != 0 || mon in opened
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = filter == 0) { opened = if (mon in opened) opened - mon else opened + mon }.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mon == weekStart(today())) tr("Эта неделя", "This week") else if (mon == weekStart(today()) - 7) tr("Прошлая неделя", "Last week")
                            else "${fmtDayShort(mon)} – ${fmtDayShort(mon + 6)}", style = T.h3, modifier = Modifier.weight(1f))
                        if (met) Row(Modifier.clip(RoundedCornerShape(50)).background(C.ok.copy(alpha = .14f)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.CHECK, C.ok, 13.dp); Text(" " + tr("цель", "goal"), style = T.tiny.copy(color = C.ok)) }
                        if (filter == 0) Icon(if (open) Icons.UP else Icons.DOWN, C.mute, 20.dp, Modifier.padding(start = 6.dp))
                    }
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        (0..6).forEach { i -> val day = mon + i; val w = ws.firstOrNull { dayOf(it.start) == day }
                            val col = w?.let { x -> d.setsBy[x.id].orEmpty().filter { it.done }.groupingBy { Catalog.of(it.exId).cat }.eachCount().maxByOrNull { it.value }?.key?.let { Color(it.color) } ?: C.acc }
                            Box(Modifier.padding(end = 4.dp).size(width = 18.dp, height = 6.dp).clip(RoundedCornerShape(50)).background(col ?: C.track)) }
                        Text("  " + listOfNotNull(count(ws.size, "тренировка", "тренировки", "тренировок", "workout", "workouts"), work.sumOf { st.tonnageOf(it) }.takeIf { it > 0 }?.let { tonnage(it) }).joinToString(" · "), style = T.small)
                    }
                }
            }
            if (filter != 0 || mon in opened) item(key = "c$mon") {
                // a week is one card, its workouts quiet rows inside it
                Column(Modifier.fillMaxWidth().card(18.dp)) {
                    ws.forEachIndexed { i, w ->
                        if (i > 0) Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(C.line))
                        HistoryRow(w, d, st) { nav.push(Screen.WorkoutView(w.id)) }
                    }
                }
            }
        }
    }
}

/** Three small numbers over the history: this month's workouts and time, and the weeks in a row. */
@Composable
private fun Glance(d: Data) {
    val t = today(); val m0 = java.time.LocalDate.ofEpochDay(t).withDayOfMonth(1).toEpochDay()
    val month = d.finished.filter { dayOf(it.start) >= m0 }
    val weeks = remember(d.finished, d.s) { app.pumpling.domain.Weeks(d.finished.map { dayOf(it.start) }.toSet(), d.s.weekGoal, d.s.sickDays) }
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple(Icons.CAL, C.acc, "${month.size}" to tr("тренировок\nв этом месяце", "workouts\nthis month")),
            Triple(Icons.CLOCK, C.ok, app.pumpling.domain.span(month.sumOf { it.end - it.start }) to tr("в зале\nза месяц", "training\nthis month")),
            Triple(Icons.FLAME, C.gold, "${weeks.streak}" to tr("недель подряд\nс целью", "weeks in a row\non goal"))).forEach { (ic, col, v) ->
            Column(Modifier.weight(1f).fillMaxHeight().card(16.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(ic, col, 16.dp); Text("  " + v.first, style = T.bodyB, maxLines = 1) }
                Text(v.second, style = T.tiny, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

/** This week at a glance: the seven days, workouts against the goal, time and tonnage. */
@Composable
private fun ThisWeek(d: Data, st: Stats) {
    val mon = weekStart(today())
    val ws = d.finished.filter { dayOf(it.start) >= mon }
    val days = ws.map { dayOf(it.start) }.toSet()
    val work = ws.flatMap { w -> d.setsBy[w.id].orEmpty().filter { it.done && !it.warmup } }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.ok, Color(0xFF1F5F7A)))).padding(18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(tr("Эта неделя", "This week"), style = T.h3.copy(color = Color.White), modifier = Modifier.weight(1f))
            Text(tr("${ws.size} из ${d.s.weekGoal}", "${ws.size} of ${d.s.weekGoal}"), style = T.h2.copy(color = Color.White))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { i -> val on = mon + i in days
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) Color.White else Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                        if (on) Icon(Icons.CHECK, C.ok, 16.dp) else if (mon + i == today()) Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White)) }
                    Text(app.pumpling.domain.dayShort(i), style = T.tiny.copy(color = Color(0xCCFFFFFF)), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            listOf(app.pumpling.domain.span(ws.sumOf { it.end - it.start }) to tr("в зале", "training"), tonnage(work.sumOf { st.tonnageOf(it) }) to tr("поднято", "lifted"),
                "${ws.sumOf { it.prs }}" to tr("рекордов", "records")).forEach { (v, l) ->
                Column { Text(v, style = T.numS.copy(color = Color.White)); Text(l, style = T.tiny.copy(color = Color(0xCCFFFFFF))) } }
        }
    }
}

@Composable
private fun PastSheet(d: Data, nav: Nav, onClose: () -> Unit) {
    val c = LocalContext.current.container
    var back by remember { mutableStateOf(1) }
    var hour by remember { mutableStateOf(18) }
    Sheet(onClose) {
        Text(tr("Записать прошедшую тренировку", "Log a past workout"), style = T.h2)
        Text(tr("Когда была?", "When was it?"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (0..4).forEach { b -> Chip(when (b) { 0 -> tr("сегодня", "today"); 1 -> tr("вчера", "yesterday"); else -> fmtDayYear(today() - b) }, back == b) { back = b } } }
        Text(tr("Во сколько началась?", "Started at"), style = T.bodyB, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(7, 9, 12, 15, 18, 20).forEach { h -> Chip("$h:00", hour == h) { hour = h } } }
        Spacer(Modifier.height(16.dp))
        Btn(tr("Записать упражнения", "Log the exercises"), Modifier.fillMaxWidth(), enabled = d.live == null) {
            val at = LocalDate.ofEpochDay(today() - back).atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            c.write { val id = start(null, at); dao.workout(id)?.let { dao.update(it.copy(manual = true)) } }
            onClose(); nav.push(Screen.Live)
        }
        if (d.live != null) Text(tr("Сначала закончите текущую тренировку", "Finish the current workout first"), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 6.dp))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WorkoutPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val w = d.finished.firstOrNull { it.id == id } ?: run { if (d.workouts.isNotEmpty()) nav.pop(); return }
    val st = remember(d.workouts, d.sets) { Stats(d.workouts, d.sets, d.bodies, d.s, Span.of(Span.ALL)) }
    val sets = d.setsBy[w.id].orEmpty().filter { it.done }
    var askDelete by remember { mutableStateOf(false) }
    Page(w.title.ifEmpty { tr("Тренировка", "Workout") }, { nav.pop() }, action = {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.push(Screen.Edit(w.id)) }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 22.dp) }
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.WORKOUT, w.id) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) } }) {
        val work = sets.filter { !it.warmup }
        val cats = work.groupingBy { Catalog.of(it.exId).cat }.eachCount().entries.sortedByDescending { it.value }
        val col = cats.firstOrNull()?.key?.let { Color(it.color) } ?: C.acc
        val tons = work.sumOf { st.tonnageOf(it) }; val meters = work.sumOf { it.meters }
        val time = java.time.Instant.ofEpochMilli(w.start).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        // the head: when, how long, how much, and who led
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(col, lerpC(col, Color.Black, .4f)))).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${weekday(dayOf(w.start)).replaceFirstChar { it.uppercase() }}, ${fmtDayYear(dayOf(w.start))}", style = T.small.copy(color = Color(0xDDFFFFFF)))
                    Text(tr("в ", "at ") + "%d:%02d".format(time.hour, time.minute) + (if (w.feel > 0) "  ${FEELS[w.feel - 1]}" else ""), style = T.small.copy(color = Color(0xDDFFFFFF)))
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        listOfNotNull(app.pumpling.domain.span(w.end - w.start) to tr("длилась", "long"),
                            if (tons > 0) tonnage(tons) to tr("поднято", "lifted") else if (meters > 0) app.pumpling.domain.km(meters) to tr("дистанция", "distance") else null,
                            "${work.size}" to tr("подходов", "sets")).forEach { (v, l) ->
                            Column { Text(v, style = T.numS.copy(color = Color.White), maxLines = 1); Text(l, style = T.tiny.copy(color = Color(0xCCFFFFFF))) } }
                    }
                }
                if (w.leader.isNotEmpty()) { val pet = d.pets.firstOrNull { it.species == w.leader && it.stage() != app.pumpling.domain.Stage.EGG }
                    app.pumpling.ui.art.Creature(w.leader, pet?.stage() ?: app.pumpling.domain.Stage.ADULT, app.pumpling.ui.art.Pose.CHEER, 92.dp, pet?.shape?.toFloat() ?: 70f, pet?.shiny ?: false) }
            }
            if (cats.isNotEmpty()) Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))) {
                cats.forEach { (cc, n) -> Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(Color(cc.color))) } }
        }
        // what it brought
        Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOfNotNull(Triple(Icons.PLATE, C.gold, tr("+${w.plates} блинов", "+${w.plates} plates")).takeIf { w.plates > 0 },
                Triple(Icons.HEART, C.acc, tr("еды: ${app.pumpling.domain.fmt1(w.food)}", "food: ${app.pumpling.domain.fmt1(w.food)}")).takeIf { w.food > 0 },
                Triple(Icons.TROPHY, C.gold, count(w.prs, "рекорд", "рекорда", "рекордов", "record", "records")).takeIf { w.prs > 0 }).forEach { (ic, tint, t) ->
                Row(Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = .13f)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ic, tint, 16.dp); Text(" $t", style = T.smallB.copy(color = C.ink)) } }
        }
        if (w.note.isNotBlank()) Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).padding(14.dp)) {
            Icon(Icons.EDIT, C.mute, 18.dp); Text(w.note, style = T.body, modifier = Modifier.padding(start = 10.dp)) }
        // the records, gold
        val recs = sets.mapNotNull { s -> s.pr.split(',').mapNotNull { Rec.of(it) }.takeIf { it.isNotEmpty() }?.let { s to it } }
        if (recs.isNotEmpty()) {
            Section(icon = Icons.TROPHY, text = tr("Рекорды", "Records"))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.goldSoft).padding(horizontal = 14.dp, vertical = 8.dp)) {
                recs.forEach { (s, r) -> val ex = Catalog.of(s.exId)
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.STAR, C.gold, 16.dp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(ex.name, style = T.bodyB, maxLines = 2); Text(r.joinToString(", ") { it.label.lowercase() }, style = T.tiny.copy(color = C.gold)) }
                        Text(setLine(ex, s), style = T.bodyB.copy(color = C.ink), modifier = Modifier.padding(start = 8.dp)) } }
            }
        }
        // what it worked
        val prim = work.flatMap { st.classMuscles(it) ?: Catalog.of(it.exId).primary }.distinct()
        if (prim.isNotEmpty()) {
            val sec = work.filter { st.classMuscles(it) == null }.flatMap { Catalog.of(it.exId).secondary }.distinct() - prim.toSet()
            Section(icon = Icons.ARM, text = tr("Что тренировали", "What it worked"))
            Column(Modifier.fillMaxWidth().card(20.dp).padding(14.dp)) {
                app.pumpling.ui.components.TargetMap(prim, sec, h = 180.dp)
                Text(tr("Основные: ", "Main: ") + prim.joinToString(", ") { it.label.lowercase() }, style = T.small.copy(color = C.ink), modifier = Modifier.padding(top = 8.dp))
            }
        }
        Section(icon = Icons.GYM, text = tr("Упражнения", "Exercises"))
        sets.groupBy { it.block }.toSortedMap().values.forEach { l -> val ex = Catalog.of(l.first().exId); val ec = Color(ex.cat.color)
            Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.Exercise(ex.id)) }.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    app.pumpling.ui.art.ExThumb(ex, 46.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(ex.name, style = T.bodyB, maxLines = 2)
                        Text(listOfNotNull(ex.cat.label, if (ex.kind == Kind.WEIGHT) tonnage(l.filter { !it.warmup }.sumOf { st.tonnageOf(it) }) else null).joinToString(" · "), style = T.tiny.copy(color = ec))
                    }
                }
                androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    l.sortedBy { it.idx }.forEach { s -> val rec = s.pr.isNotBlank()
                        Row(Modifier.clip(RoundedCornerShape(10.dp)).background(when { rec -> C.goldSoft; s.warmup -> C.bg; else -> ec.copy(alpha = .11f) }).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (rec) Icon(Icons.STAR, C.gold, 13.dp)
                            if (s.warmup) Text(tr("разм. ", "warm "), style = T.tiny)
                            Text((if (rec) " " else "") + setLine(ex, s), style = T.smallB.copy(color = if (s.warmup) C.mute else C.ink))
                            if (s.rpe > 0) Text(" · RPE ${s.rpe}", style = T.tiny)
                        } }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Btn(tr("Повторить тренировку", "Do it again"), Modifier.fillMaxWidth(), enabled = d.live == null) {
            // A group class starts again as a class with the same name (#10).
            if (w.classOf.isNotEmpty()) c.write { startClass(w.classOf, w.title) }
            else c.write { val nid = start(null); dao.workout(nid)?.let { dao.update(it.copy(title = w.title)) }; sets.map { it.exId }.distinct().forEach { addExercise(nid, it) } }
            nav.pop(); nav.push(Screen.Live) }
        Spacer(Modifier.height(8.dp))
        Ghost(tr("Изменить тренировку", "Edit the workout"), Modifier.fillMaxWidth()) { nav.push(Screen.Edit(w.id)) }
        Spacer(Modifier.height(8.dp))
        if (w.classOf.isEmpty()) Ghost(tr("Сохранить как программу", "Save as a routine"), Modifier.fillMaxWidth()) {
            val plans = sets.filter { !it.warmup }.groupBy { it.block }.toSortedMap().values.map { l -> Plan(l.first().exId, l.count { it.side != 2 }, l.first().reps, l.first().weight, l.first().secs) }
            c.write { dao.insert(Routine(name = w.title.ifEmpty { tr("Программа", "Routine") }, items = Plan.join(plans), created = System.currentTimeMillis())); say(tr("Программа сохранена", "Routine saved")) } }
        Spacer(Modifier.height(8.dp))
        Ghost(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { askDelete = true }
    }
    if (askDelete) app.pumpling.ui.components.Popup({ askDelete = false }) {
        Text(tr("Удалить тренировку?", "Delete this workout?"), style = T.h2)
        Text(tr("Подходы и рекорды из неё пропадут. Съеденное питомцами останется.", "Its sets and records go. What the pets ate stays."), style = T.body, modifier = Modifier.padding(vertical = 10.dp))
        Btn(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { dao.deleteSetsOf(w.id); dao.deleteWorkout(w.id) }; askDelete = false; nav.pop() }
    }
}

// ---------------- routines ----------------

/** About how long a plan takes: two and a half minutes a set, or its own time. */
private fun planMinutes(l: List<Plan>) = l.sumOf { p -> val k = Catalog.of(p.exId).kind; if (k == Kind.CARDIO || k == Kind.MINUTES) maxOf(1, p.secs / 60) * p.sets else (p.sets * 2.5).toInt() }

/** The families a plan is made of, by sets, for its stripe. */
private fun planParts(l: List<Plan>) = l.groupBy { Catalog.of(it.exId).cat }.map { (c, ps) -> Color(c.color) to ps.sumOf { it.sets } }.sortedByDescending { it.second }
private fun planMain(l: List<Plan>) = l.groupBy { Catalog.of(it.exId).cat }.maxByOrNull { e -> e.value.sumOf { it.sets } }?.key

@Composable
private fun Routines(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var ready by remember { mutableStateOf(false) }
    val lastUse = remember(d.finished) { d.finished.filter { it.routineId > 0 }.groupBy { it.routineId }.mapValues { e -> e.value.maxOf { it.start } } }
    val next = d.routines.minByOrNull { lastUse[it.id] ?: 0L }
    fun go(r: Routine) { if (d.live == null) nav.askStart = r.id else { c.say(tr("Сначала закончите текущую тренировку", "Finish the current workout first")); nav.push(Screen.Live) } }
    // a box, so the sheet of ready routines lies over the list instead of under it
    Box(Modifier.fillMaxSize()) {
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)) {
        item(key = "hint") { app.pumpling.ui.components.Hint("routines", d.s.seen, Icons.LIST, tr("Программы", "Routines"), tr("Программа – готовый список упражнений с подходами. Начинайте её одной кнопкой: подходы подставятся сами. Следующая по очереди всегда сверху.",
            "A routine is a ready list of exercises and sets. Start it with one tap and the sets fill in. The one due next is always on top."), Modifier.padding(bottom = 10.dp)) }
        if (d.routines.isEmpty()) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.acc, Color(0xFFB8324F)))).padding(20.dp)) {
                Text(tr("Программ пока нет", "No routines yet"), style = T.h2.copy(color = Color.White))
                Text(tr("Программа – список упражнений, который начинается одной кнопкой. Возьмите готовую или соберите свою.", "A routine is a list of exercises started with one tap. Take a ready one or build your own."),
                    style = T.body.copy(color = Color(0xE6FFFFFF)), modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(tr("Взять готовую", "Take a ready one"), Modifier.weight(1f), color = Color.White, ink = C.acc, small = true) { ready = true }
                    Btn(tr("Своя", "My own"), Modifier.weight(1f), color = Color(0x33FFFFFF), ink = Color.White, small = true) { nav.picked = emptyList(); nav.push(Screen.RoutineEdit(0)) }
                }
            }
        }
        // the one due next, big
        next?.let { r -> item(key = "next") {
            val plans = Plan.parse(r.items); val main = planMain(plans)
            val col = main?.let { Color(it.color) } ?: C.acc
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(col, lerpC(col, Color.Black, .35f))))
                .clickable { nav.picked = emptyList(); nav.push(Screen.RoutineEdit(r.id)) }.padding(18.dp)) {
                Text(tr("ДАЛЬШЕ ПО ОЧЕРЕДИ", "UP NEXT"), style = T.sec.copy(color = Color(0xCCFFFFFF)))
                Text(r.name, style = T.title.copy(color = Color.White), modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                Text(count(plans.size, "упражнение", "упражнения", "упражнений", "exercise", "exercises") + " · ≈ " + tr("${planMinutes(plans)} мин", "${planMinutes(plans)} min"),
                    style = T.small.copy(color = Color(0xD9FFFFFF)))
                Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    plans.forEach { p -> Text(Catalog.of(p.exId).name, style = T.smallB.copy(color = Color.White), maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x29FFFFFF)).padding(horizontal = 10.dp, vertical = 5.dp)) } }
                Btn(tr("Начать", "Start"), Modifier.fillMaxWidth().padding(top = 16.dp), color = Color.White, ink = col) { go(r) }
            }
        } }
        val rest = d.routines.filter { it.id != next?.id }
        if (rest.isNotEmpty()) item(key = "more") { Section(icon = Icons.LIST, text = tr("Остальные", "The others")) }
        items(rest, key = { it.id }) { r ->
            val plans = Plan.parse(r.items); val main = planMain(plans); val col = main?.let { Color(it.color) } ?: C.mute
            Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(20.dp).clickable { nav.picked = emptyList(); nav.push(Screen.RoutineEdit(r.id)) }.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Icon(Icons.GYM, col, 24.dp) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(r.name, style = T.h3, maxLines = 1)
                        Text(count(plans.size, "упражнение", "упражнения", "упражнений", "exercise", "exercises") + " · ≈ " + tr("${planMinutes(plans)} мин", "${planMinutes(plans)} min"), style = T.small, maxLines = 1)
                    }
                    Box(Modifier.size(44.dp).clip(CircleShape).background(col).clickable { go(r) }, contentAlignment = Alignment.Center) { Text("▶", style = T.h3.copy(color = Color.White)) }
                }
                app.pumpling.ui.components.Stripe(planParts(plans), Modifier.padding(top = 12.dp))
                Text(plans.take(3).joinToString(" · ") { Catalog.of(it.exId).name } + if (plans.size > 3) tr(" и ещё ${plans.size - 3}", " and ${plans.size - 3} more") else "",
                    style = T.tiny, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
            }
        }
        if (d.routines.isNotEmpty()) item(key = "add") {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddTile(Icons.PLUS, tr("Своя программа", "My own routine"), tr("собрать из упражнений", "pick the exercises"), C.acc, Modifier.weight(1f)) { nav.picked = emptyList(); nav.push(Screen.RoutineEdit(0)) }
                AddTile(Icons.SPARK, tr("Готовые", "Ready ones"), count(TEMPLATES.size, "программа", "программы", "программ", "routine", "routines"), C.gold, Modifier.weight(1f)) { ready = true }
            }
        }
    }
    if (ready) Sheet({ ready = false }) {
        var place by remember { mutableStateOf<Place?>(null) }
        var level by remember { mutableStateOf<Level?>(null) }
        var look by remember { mutableStateOf<Template?>(null) }
        val t0 = look
        if (t0 != null) { TemplateView(t0, d) { take -> if (take) { c.write { dao.insert(Routine(name = tr(t0.ru, t0.en), items = t0.items, created = System.currentTimeMillis(), order = d.routines.size))
            say(tr("«${tr(t0.ru, t0.en)}» – в ваших программах", "«${tr(t0.ru, t0.en)}» added")) }; ready = false } else look = null }; return@Sheet }
        Text(tr("Готовые программы", "Ready routines"), style = T.h2)
        Text(count(TEMPLATES.size, "программа", "программы", "программ", "routine", "routines") + tr(" – нажмите, чтобы посмотреть", " – tap one to look inside"), style = T.small, modifier = Modifier.padding(bottom = 10.dp))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(tr("Где угодно", "Anywhere"), place == null) { place = null }
            Place.entries.forEach { pl -> Chip(pl.label, place == pl) { place = pl } } }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(tr("Любой уровень", "Any level"), level == null, color = C.ok) { level = null }
            Level.entries.forEach { lv -> Chip(lv.label, level == lv, color = C.ok) { level = lv } } }
        val shown = TEMPLATES.filter { (place == null || it.place == place) && (level == null || it.level == level) }
        if (shown.isEmpty()) Empty(tr("Таких программ нет – уберите фильтр", "No such routines – clear a filter"))
        shown.groupBy { it.place }.forEach { (pl, ts) ->
            Row(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(placeIcon(pl), C.acc, 16.dp)
                Text("  " + pl.label.uppercase() + " · ${ts.size}", style = T.sec) }
            ts.forEach { t -> TemplateRow(t, d) { look = t } }
        }
    }
    }
}

@Composable
private fun AddTile(icon: Icons, title: String, sub: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.card(20.dp).clickable(onClick = onClick).padding(14.dp)) {
        app.pumpling.ui.components.Badge(icon, tint, 40.dp)
        Text(title, style = T.bodyB, modifier = Modifier.padding(top = 10.dp), maxLines = 1)
        Text(sub, style = T.tiny, maxLines = 1)
    }
}

private fun lerpC(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)

@Composable
fun RoutinePage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.routines.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(old?.name ?: "") }
    var askDelete by remember { mutableStateOf(false) }
    val plans = remember { mutableStateListOf<Plan>().apply { addAll(old?.let { Plan.parse(it.items) } ?: emptyList()) } }
    if (nav.picked.isNotEmpty()) { nav.picked.forEach { ex -> val k = Catalog.of(ex).kind
        plans.add(Plan(ex, if (k == Kind.CARDIO || k == Kind.MINUTES) 1 else 3, if (k.timed) 0 else 10, 0.0, when (k) { Kind.HOLD -> 45; Kind.CARDIO -> 1800; Kind.MINUTES -> 900; else -> 0 })) }; nav.picked = emptyList() }
    val col = planMain(plans)?.let { Color(it.color) } ?: C.acc
    Page(if (old == null) tr("Новая программа", "New routine") else tr("Программа", "Routine"), { nav.pop() }, action = {
        if (old != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable { askDelete = true }, contentAlignment = Alignment.Center) { Icon(Icons.TRASH, C.mute, 22.dp) } }) {
        // the head: the name and what the plan adds up to
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(col, lerpC(col, Color.Black, .35f)))).padding(18.dp)) {
            Text(tr("НАЗВАНИЕ", "NAME"), style = T.sec.copy(color = Color(0xCCFFFFFF)))
            androidx.compose.foundation.text.BasicTextField(name, { name = it }, textStyle = T.title.copy(color = Color.White), singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White), modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                decorationBox = { inner -> if (name.isEmpty()) Text(tr("Например, «Понедельник»", "E.g. «Monday»"), style = T.title.copy(color = Color(0x80FFFFFF))); inner() })
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                listOf("${plans.size}" to tr("упражнений", "exercises"), "${plans.sumOf { it.sets }}" to tr("подходов", "sets"), "≈ ${planMinutes(plans)}" to tr("минут", "minutes")).forEach { (v, l) ->
                    Column { Text(v, style = T.num.copy(color = Color.White)); Text(l, style = T.tiny.copy(color = Color(0xCCFFFFFF))) } }
            }
            if (plans.isNotEmpty()) app.pumpling.ui.components.Stripe(planParts(plans).map { (cc, n) -> cc to n }, Modifier.padding(top = 12.dp))
            val done = if (old == null) emptyList() else d.finished.filter { it.routineId == old.id }.sortedByDescending { it.start }
            if (done.isNotEmpty()) Row(Modifier.padding(top = 12.dp).clip(RoundedCornerShape(50)).background(Color(0x26FFFFFF)).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.CAL, Color.White, 15.dp)
                Text("  " + tr("Делали ${done.size} раз · последний ${fmtDayShort(dayOf(done[0].start))}", "Done ${done.size} times · last ${fmtDayShort(dayOf(done[0].start))}"), style = T.small.copy(color = Color.White))
            }
        }
        // what the whole plan works, on one body
        if (plans.isNotEmpty()) {
            val prim = plans.flatMap { Catalog.of(it.exId).primary }.distinct()
            val sec = plans.flatMap { Catalog.of(it.exId).secondary }.distinct() - prim.toSet()
            Section(icon = Icons.ARM, text = tr("Что тренирует", "What it works"))
            Column(Modifier.fillMaxWidth().card(20.dp).padding(14.dp)) {
                app.pumpling.ui.components.TargetMap(prim, sec, h = 190.dp)
                Text(tr("Основные: ", "Main: ") + prim.joinToString(", ") { it.label.lowercase() }, style = T.small.copy(color = C.ink), modifier = Modifier.padding(top = 8.dp))
                if (sec.isNotEmpty()) Text(tr("Помогают: ", "Helping: ") + sec.joinToString(", ") { it.label.lowercase() }, style = T.small)
            }
        }
        Section(icon = Icons.GYM, text = tr("Упражнения", "Exercises"))
        app.pumpling.ui.components.Hint("routine-edit", d.s.seen, Icons.LIST, tr("Как собрать программу", "Building a routine"), tr("Добавьте упражнения и задайте подходы и повторы. Стрелкой меняется порядок. Когда начнёте программу, всё подставится само, а веса возьмутся из прошлого раза.",
            "Add exercises and set sets and reps; the arrow changes the order. When you start it, everything fills in, weights from last time."), Modifier.padding(bottom = 10.dp))
        if (plans.isEmpty()) Column(Modifier.fillMaxWidth().card(20.dp).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            d.leader?.let { app.pumpling.ui.art.PetView(it, 110.dp, app.pumpling.ui.art.Pose.IDLE) }
            Text(tr("Пока пусто. Добавьте упражнения – подходы и повторы поправите здесь же.", "Empty for now. Add exercises – sets and reps are set right here."), style = T.body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        plans.forEachIndexed { i, p -> val ex = Catalog.of(p.exId); val ec = Color(ex.cat.color)
            Row(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(18.dp).padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 12.dp)) {
                app.pumpling.ui.art.ExThumb(ex, 48.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("${i + 1}. " + ex.name, style = T.bodyB, maxLines = 2)
                            Text((listOf(ex.cat.label) + ex.primary.take(2).map { it.label.lowercase() }).joinToString(" · "), style = T.tiny.copy(color = ec), maxLines = 1) }
                        if (i > 0) Box(Modifier.size(36.dp).clip(CircleShape).clickable { val t = plans[i - 1]; plans[i - 1] = p; plans[i] = t }, contentAlignment = Alignment.Center) { Icon(Icons.UP, C.mute, 18.dp) }
                        Box(Modifier.size(36.dp).clip(CircleShape).clickable { plans.removeAt(i) }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, C.mute, 18.dp) }
                    }
                    val last = remember(d.doneSets, p.exId) { d.doneSets.filter { it.exId == p.exId && !it.warmup }.groupBy { it.workoutId }.maxByOrNull { e -> e.value.maxOf { it.doneAt } }?.value?.sortedBy { it.idx } }
                    if (last != null) Text(tr("В прошлый раз: ", "Last time: ") + last.take(4).joinToString(", ") { app.pumpling.domain.setLine(ex, it).removePrefix("× ") } + (if (last.size > 4) " …" else ""),
                        style = T.tiny.copy(color = C.ink), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stepper(tr("подходы", "sets"), "${p.sets}") { plans[i] = p.copy(sets = (p.sets + it).coerceIn(1, 12)) }
                        when (ex.kind) {
                            Kind.HOLD -> Stepper(tr("секунды", "seconds"), "${p.secs}") { plans[i] = p.copy(secs = (p.secs + it * 15).coerceIn(15, 600)) }
                            Kind.CARDIO, Kind.MINUTES -> Stepper(tr("минуты", "minutes"), "${p.secs / 60}") { plans[i] = p.copy(secs = (p.secs + it * 300).coerceIn(300, 7200)) }
                            else -> Stepper(tr("повторы", "reps"), "${p.reps}") { plans[i] = p.copy(reps = (p.reps + it).coerceIn(1, 50)) }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.5.dp, C.acc.copy(alpha = .5f), RoundedCornerShape(18.dp)).clickable { nav.push(Screen.Picker(forRoutine = true)) }
            .padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.PLUS, C.acc, 20.dp); Text("  " + tr("Добавить упражнения", "Add exercises"), style = T.btn.copy(color = C.acc)) }
        Spacer(Modifier.height(18.dp))
        if (old != null) { Btn(tr("Начать тренировку", "Start workout"), Modifier.fillMaxWidth(), color = C.ok) {
            if (d.live == null) nav.askStart = old.id else { c.say(tr("Сначала закончите текущую тренировку", "Finish the current workout first")); nav.push(Screen.Live) } }
            Spacer(Modifier.height(10.dp)) }
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = name.isNotBlank() && plans.isNotEmpty()) {
            c.write { if (old == null) dao.insert(Routine(name = name.trim(), items = Plan.join(plans), created = System.currentTimeMillis(), order = d.routines.size))
                else dao.update(old.copy(name = name.trim(), items = Plan.join(plans))) }
            nav.pop() }
        if (name.isBlank() && plans.isNotEmpty()) Text(tr("Дайте программе название – вверху", "Give the routine a name – at the top"), style = T.small, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
    }
    if (askDelete && old != null) app.pumpling.ui.components.Popup({ askDelete = false }) {
        Text(tr("Удалить «${old.name}»?", "Delete «${old.name}»?"), style = T.h2)
        Text(tr("Прошлые тренировки по ней останутся в журнале.", "Past workouts from it stay in the log."), style = T.body, modifier = Modifier.padding(vertical = 10.dp))
        Btn(tr("Удалить", "Delete"), Modifier.fillMaxWidth(), color = C.bad) { c.write { dao.deleteRoutine(old.id) }; askDelete = false; nav.pop() }
    }
}

/** − value + with a caption under it. */
@Composable
private fun Stepper(label: String, value: String, onStep: (Int) -> Unit) {
    Column {
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(C.card2), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clickable { onStep(-1) }, contentAlignment = Alignment.Center) { Text("−", style = T.h2.copy(color = C.acc)) }
            Text(value, style = T.numS, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            Box(Modifier.size(40.dp).clickable { onStep(1) }, contentAlignment = Alignment.Center) { Text("+", style = T.h2.copy(color = C.acc)) }
        }
        Text(label, style = T.tiny, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
    }
}

/** The icon for a way of logging. */
fun kindIcon(k: Kind) = when (k) { Kind.WEIGHT -> Icons.GYM; Kind.BODY, Kind.ASSIST -> Icons.ARM; Kind.HOLD -> Icons.CLOCK; Kind.CARDIO -> Icons.ROUTE; Kind.MINUTES -> Icons.TIMER }

@Composable
private fun OwnList(d: Data, nav: Nav) {
    val own = d.own.filter { !it.archived }
    val used = remember(d.doneSets) { d.doneSets.groupingBy { it.exId }.eachCount() }
    val last = remember(d.doneSets, d.finished) { val byW = d.finished.associateBy { it.id }; d.doneSets.groupBy { it.exId }.mapValues { e -> e.value.mapNotNull { byW[it.workoutId]?.start }.maxOrNull() } }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)) {
        item(key = "head") {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF6B5BD2), Color(0xFF3A2E7C)))).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Свои упражнения", "Your own exercises"), style = T.h2.copy(color = Color.White))
                        Text(tr("Всё, чего нет среди ${Catalog.all().size - own.size} в каталоге. Кормят питомцев, как любые другие.", "Anything missing from the ${Catalog.all().size - own.size} in the catalogue. They feed pets like any other."),
                            style = T.small.copy(color = Color(0xD9FFFFFF)), modifier = Modifier.padding(top = 4.dp))
                    }
                    Text("${own.size}", style = T.big.copy(color = Color.White))
                }
                Btn(tr("+ Новое упражнение", "+ New exercise"), Modifier.fillMaxWidth().padding(top = 14.dp), color = Color.White, ink = Color(0xFF4B3DB0), small = true) { nav.push(Screen.OwnEdit(0)) }
            }
        }
        if (own.isEmpty()) item {
            Text(tr("Например: тяга санок, армрестлинг, лазание по канату, своя растяжка. Выберите, как записывать, и какие мышцы работают.",
                "For example: sled pull, arm wrestling, rope climb, your own stretch. Choose how to log it and which muscles it works."), style = T.small, modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp))
        }
        if (own.isNotEmpty()) item { Section(icon = Icons.LIST, text = tr("Ваши", "Yours")) }
        items(own, key = { it.id }) { e -> val ex = Catalog.of("u${e.id}"); val col = Color(ex.cat.color)
            Row(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.Exercise(ex.id)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Icon(kindIcon(ex.kind), col, 24.dp) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(e.name, style = T.bodyB, maxLines = 1)
                    Text(ex.cat.label + " · " + (ex.primary.firstOrNull()?.label?.lowercase() ?: ex.kind.name1.lowercase()), style = T.small.copy(color = col), maxLines = 1)
                    Text((used[ex.id]?.let { tr("$it подх.", "$it sets") } ?: tr("ещё не делали", "not done yet")) + (last[ex.id]?.let { " · " + fmtDayShort(dayOf(it)) } ?: ""), style = T.tiny)
                }
                Box(Modifier.size(38.dp).clip(CircleShape).clickable { nav.push(Screen.OwnEdit(e.id)) }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.mute, 18.dp) }
            }
        }
    }
}

@Composable
private fun HistoryRow(w: Workout, d: Data, st: Stats, onClick: () -> Unit) {
    val work = d.setsBy[w.id].orEmpty().filter { it.done && !it.warmup }
    val day = java.time.LocalDate.ofEpochDay(dayOf(w.start))
    val cats = work.groupingBy { Catalog.of(it.exId).cat }.eachCount().entries.sortedByDescending { it.value }
    val main = cats.firstOrNull()?.key
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(main?.let { Color(it.color).copy(alpha = .14f) } ?: C.card2),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("${day.dayOfMonth}", style = T.bodyB.copy(color = main?.let { Color(it.color) } ?: C.ink))
            Text(app.pumpling.domain.dayShort(day.dayOfWeek.value - 1), style = T.tiny)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(w.title.ifEmpty { tr("Тренировка", "Workout") }, style = T.bodyB, maxLines = 1)
            val t = work.sumOf { st.tonnageOf(it) }; val m = work.sumOf { it.meters }
            Text(listOfNotNull(app.pumpling.domain.span(w.end - w.start), t.takeIf { it > 0 }?.let { tonnage(it) }, m.takeIf { it > 0 && t == 0.0 }?.let { app.pumpling.domain.km(it) })
                .joinToString(" · "), style = T.small, maxLines = 1)
            // what was trained, as one thin stripe
            if (cats.isNotEmpty()) Row(Modifier.padding(top = 6.dp).width(150.dp).height(4.dp).clip(RoundedCornerShape(50))) {
                cats.forEach { (c, n) -> Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(Color(c.color))) } }
        }
        // who led that day, with its records pinned on
        Box(Modifier.padding(start = 8.dp).size(48.dp)) {
            Art.pet(ctx, w.leader.ifEmpty { "bull" }, 2)?.let { androidx.compose.foundation.Image(it, null, Modifier.size(42.dp).align(Alignment.Center)) }
            if (w.prs > 0) Row(Modifier.align(Alignment.BottomEnd).clip(RoundedCornerShape(50)).background(C.gold).padding(horizontal = 5.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.TROPHY, Color.White, 10.dp); Text("${w.prs}", style = T.tiny.copy(color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) }
        }
    }
}

fun placeIcon(p: Place) = when (p) { Place.GYM -> Icons.GYM; Place.HOME -> Icons.HOME; Place.OUT -> Icons.SUN }

/** One ready routine in a list: place icon, name, what it is, level, time, stripe. */
@Composable
fun TemplateRow(t: Template, d: Data, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val plans = Plan.parse(t.items); val col = planMain(plans)?.let { Color(it.color) } ?: C.acc
    val taken = d.routines.any { it.items == t.items }
    Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(18.dp).clickable(onClick = onClick).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(col.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Icon(placeIcon(t.place), col, 20.dp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(tr(t.ru, t.en), style = T.bodyB)
                Text(tr(t.aboutRu, t.aboutEn), style = T.small, maxLines = 2)
                Text(t.level.label + " · ≈ " + tr("${planMinutes(plans)} мин", "${planMinutes(plans)} min") + if (taken) tr(" · уже у вас", " · already yours") else "", style = T.tiny.copy(color = C.ok))
            }
            if (trailing != null) trailing() else Icon(Icons.NEXT, C.mute, 18.dp)
        }
        app.pumpling.ui.components.Stripe(planParts(plans), Modifier.padding(top = 10.dp), h = 5.dp)
    }
}

/** What's inside a ready routine, before taking it. [onDone] true takes it, false goes back. */
@Composable
private fun TemplateView(t: Template, d: Data, onDone: (Boolean) -> Unit) {
    val plans = Plan.parse(t.items); val col = planMain(plans)?.let { Color(it.color) } ?: C.acc
    val taken = d.routines.any { it.items == t.items }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { onDone(false) }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.ink, 22.dp) }
        Text(tr("Все программы", "All routines"), style = T.small)
    }
    Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(col, lerpC(col, Color.Black, .35f)))).padding(18.dp)) {
        Text((t.place.label + " · " + t.level.label).uppercase(), style = T.sec.copy(color = Color(0xCCFFFFFF)))
        Text(tr(t.ru, t.en), style = T.title.copy(color = Color.White), modifier = Modifier.padding(top = 4.dp))
        Text(tr(t.aboutRu, t.aboutEn), style = T.small.copy(color = Color(0xD9FFFFFF)))
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf("${plans.size}" to tr("упражнений", "exercises"), "${plans.sumOf { it.sets }}" to tr("подходов", "sets"), "≈ ${planMinutes(plans)}" to tr("минут", "minutes")).forEach { (v, l) ->
                Column { Text(v, style = T.num.copy(color = Color.White)); Text(l, style = T.tiny.copy(color = Color(0xCCFFFFFF))) } }
        }
    }
    Spacer(Modifier.height(12.dp))
    plans.forEachIndexed { i, p -> val ex = Catalog.of(p.exId); val ec = Color(ex.cat.color)
        Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().card(16.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            app.pumpling.ui.art.ExThumb(ex, 48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text("${i + 1}. " + ex.name, style = T.bodyB)
                Text(ex.primary.joinToString(", ") { it.label.lowercase() } + " · " + ex.equipLabel.lowercase(), style = T.tiny, maxLines = 1)
            }
            Text(when (ex.kind) { Kind.CARDIO, Kind.MINUTES -> tr("${p.secs / 60} мин", "${p.secs / 60} min"); Kind.HOLD -> "${p.sets}\u00a0×\u00a0${p.secs}" + tr("\u00a0с", "\u00a0s"); else -> "${p.sets}\u00a0×\u00a0${p.reps}" }, style = T.numS)
        }
    }
    Spacer(Modifier.height(8.dp))
    if (taken) Text(tr("Эта программа уже у вас", "You already have this routine"), style = T.small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    else Btn(tr("Взять себе", "Take it"), Modifier.fillMaxWidth()) { onDone(true) }
    Text(tr("После можно поменять упражнения и подходы", "You can change exercises and sets afterwards"), style = T.tiny, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
}

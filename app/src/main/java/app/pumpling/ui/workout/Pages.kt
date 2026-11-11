package app.pumpling.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import app.pumpling.domain.count
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
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
import app.pumpling.container
import app.pumpling.Plan
import app.pumpling.data.Routine
import app.pumpling.data.OwnEx
import app.pumpling.domain.Catalog
import app.pumpling.domain.Cat
import app.pumpling.domain.Equip
import app.pumpling.domain.Kind
import app.pumpling.domain.Muscle
import app.pumpling.domain.Records
import app.pumpling.domain.fmt1
import app.pumpling.domain.tr
import app.pumpling.domain.sp
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.art.PetView
import app.pumpling.ui.art.EggView
import app.pumpling.ui.art.Pose
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.Chip
import app.pumpling.ui.components.ChipRow
import app.pumpling.ui.components.Dot
import app.pumpling.ui.components.Field
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.Section
import app.pumpling.ui.components.card
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.ShareReq
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T

@Composable
fun PickerPage(forRoutine: Boolean, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(-1) }
    var eq by remember { mutableStateOf(-1) }
    val picked = remember { mutableStateListOf<String>() }
    val used = remember(d.doneSets) { d.doneSets.groupingBy { it.exId }.eachCount() }
    val recent = remember(d.doneSets) { d.doneSets.sortedByDescending { it.doneAt }.map { it.exId }.distinct().take(8) }
    val all = Catalog.all()
    val list = all.filter { e -> (q.isBlank() || e.name.contains(q.trim(), true) || e.en.contains(q.trim(), true) || e.ru.contains(q.trim(), true)) &&
        (cat < 0 || e.cat == Cat.entries[cat]) && (eq < 0 || (if (eq < Equip.entries.size) e.equip == Equip.entries[eq] && e.gear.isEmpty() else e.gear == d.s.gear.getOrNull(eq - Equip.entries.size))) }
        .sortedWith(compareByDescending<app.pumpling.domain.Ex> { if (q.isBlank() && cat < 0 && eq < 0) (if (it.id in recent) 1000 - recent.indexOf(it.id) else 0) else 0 }.thenByDescending { used[it.id] ?: 0 })
    Page(tr("Упражнения", "Exercises"), { nav.pop() }, scroll = false, action = {
        Text(tr("Своё", "Own"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(end = 12.dp).clip(RoundedCornerShape(10.dp)).clickable { nav.push(Screen.OwnEdit(0)) }.padding(8.dp)) }) {
        Field(q, { q = it }, tr("Найти: жим, присед, бег…", "Search: press, squat, run…"), Modifier.fillMaxWidth())
        ChipRow(listOf(tr("Все", "All")) + Cat.entries.map { it.label }, cat + 1, Modifier.padding(top = 8.dp)) { cat = it - 1 }
        ChipRow(listOf(tr("Любой инвентарь", "Any equipment")) + Equip.entries.map { it.label } + d.s.gear, eq + 1, Modifier.padding(top = 6.dp)) { eq = it - 1 }
        LazyColumn(Modifier.weight(1f).padding(top = 6.dp)) {
            if (list.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("Такого упражнения в каталоге нет", "No such exercise in the catalogue"), style = T.body)
                    Spacer(Modifier.height(10.dp))
                    Btn(if (q.isBlank()) tr("Создать своё", "Create your own") else tr("Создать «${q.trim()}»", "Create «${q.trim()}»"), small = true) { nav.draftName = q.trim(); nav.push(Screen.OwnEdit(0)) }
                }
            }
            items(list, key = { it.id }) { e ->
                val on = e.id in picked
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) C.accSoft else Color.Transparent).clickable { if (on) picked.remove(e.id) else picked.add(e.id) }
                    .padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Dot(Color(e.cat.color))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(e.name, style = T.bodyB)
                        Text((e.primary.map { it.label } + e.equipLabel).joinToString(" · ") + (used[e.id]?.let { tr(" · $it подх.", " · $it sets") } ?: ""), style = T.small, maxLines = 1)
                    }
                    Box(Modifier.size(26.dp).clip(CircleShape).background(if (on) C.acc else C.card2), contentAlignment = Alignment.Center) { if (on) Icon(Icons.CHECK, Color.White, 16.dp) }
                }
            }
        }
        Btn(if (picked.isEmpty()) tr("Выберите упражнения", "Pick exercises") else tr("Добавить (${picked.size})", "Add (${picked.size})"), Modifier.fillMaxWidth().padding(vertical = 10.dp), enabled = picked.isNotEmpty()) {
            val ids = picked.toList()
            if (forRoutine) { nav.picked = nav.picked + ids; nav.pop() }
            else { val w = d.live; c.write { if (w != null) ids.forEach { addExercise(w.id, it) } }; nav.pop() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnExPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.own.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(old?.name ?: nav.draftName.also { nav.draftName = "" }) }
    var kind by remember { mutableStateOf(Kind.of(old?.kind?.firstOrNull() ?: 'W')) }
    val prim = remember { mutableStateListOf<Muscle>().apply { addAll(old?.primary?.split(',')?.mapNotNull { Muscle.of(it) } ?: emptyList()) } }
    var equip by remember { mutableStateOf(old?.equip ?: "other") }
    var addGear by remember { mutableStateOf(false) }
    var cat by remember { mutableStateOf(old?.cat?.let { Cat.of(it) }) }
    val auto = cat ?: guessCat(kind, prim)
    val col = Color(auto.color)
    Page(if (old == null) tr("Новое упражнение", "New exercise") else tr("Упражнение", "Exercise"), { nav.pop() }) {
        // the head, in the colour of the family it feeds
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(col, Color(col.red * .65f, col.green * .65f, col.blue * .65f)))).padding(18.dp)) {
            Text(tr("НАЗВАНИЕ", "NAME"), style = T.sec.copy(color = Color(0xCCFFFFFF)))
            androidx.compose.foundation.text.BasicTextField(name, { name = it }, textStyle = T.title.copy(color = Color.White), singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White), modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                decorationBox = { inner -> if (name.isEmpty()) Text(tr("Например, «Тяга санок»", "E.g. «Sled pull»"), style = T.title.copy(color = Color(0x80FFFFFF))); inner() })
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) { Icon(app.pumpling.ui.journal.kindIcon(kind), Color.White, 17.dp) }
                Text("  " + kind.name1 + " · " + auto.label.lowercase(), style = T.smallB.copy(color = Color.White))
            }
        }
        Section(icon = Icons.EDIT, text = tr("Как записывать", "How to log it"))
        Kind.entries.chunked(2).forEach { row ->
            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k -> val on = kind == k
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) C.accSoft else C.card).border(if (on) 2.dp else 0.dp, C.acc, RoundedCornerShape(16.dp))
                        .clickable { kind = k }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(app.pumpling.ui.journal.kindIcon(k), if (on) C.acc else C.mute, 20.dp)
                        Text(k.name1, style = T.smallB.copy(color = if (on) C.ink else C.mute), modifier = Modifier.padding(start = 8.dp), maxLines = 2)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.card2).padding(14.dp)) {
            Icon(app.pumpling.ui.journal.kindIcon(kind), C.acc, 20.dp)
            Text(kind.help, style = T.small.copy(color = C.ink), modifier = Modifier.padding(start = 10.dp))
        }
        Section(icon = Icons.ARM, text = tr("Какие мышцы работают", "Muscles worked"))
        Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
            app.pumpling.ui.components.BodyMap(Muscle.entries.associateWith { if (it in prim) 1.0 else 0.0 }, color = col, h = 150.dp)
            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Muscle.entries.forEach { m -> Chip(m.label, m in prim, color = col) { if (m in prim) prim.remove(m) else prim.add(m) } } }
        }
        Section(icon = Icons.GYM, text = tr("Инвентарь", "Equipment"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Equip.entries.forEach { e -> Chip(e.label, equip == e.id) { equip = e.id } }
            d.s.gear.forEach { g -> Chip(g, equip == app.pumpling.OWN_GEAR + g, color = C.ok) { equip = app.pumpling.OWN_GEAR + g } }
            Row(Modifier.clip(RoundedCornerShape(50)).border(1.5.dp, C.acc.copy(alpha = .6f), RoundedCornerShape(50)).clickable { addGear = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) { Icon(Icons.PLUS, C.acc, 14.dp); Text(" " + tr("свой", "own"), style = T.smallB.copy(color = C.acc)) } }
        if (addGear) GearPopup(d, { addGear = false }) { equip = app.pumpling.OWN_GEAR + it }
        Section(icon = Icons.PAW, text = tr("Кого кормит", "Who it feeds"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Cat.entries.forEach { k -> Chip(k.label, auto == k, color = Color(k.color)) { cat = k } } }
        val fans = app.pumpling.domain.Pets.all.filter { it.likes == auto }
        Text((if (cat == null) tr("Выбрано само по мышцам. ", "Picked from the muscles. ") else "") + if (fans.isNotEmpty()) tr("Это любит ${fans.joinToString { it.name.lowercase() }}.", "Loved by the ${fans.joinToString { it.name.lowercase() }}.") else "",
            style = T.small, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            c.write { val exId = saveOwn(OwnEx(id = old?.id ?: 0, name = name.trim(), kind = kind.code.toString(), primary = prim.joinToString(",") { it.id }, equip = equip, cat = auto.id))
                say(tr("Упражнение сохранено", "Exercise saved"))
                val w = d.live; if (old == null && w != null && nav.stack.any { it is Screen.Live }) addExercise(w.id, exId) }
            nav.pop()
        }
        if (name.isBlank()) Text(tr("Начните с названия – вверху", "Start with the name – at the top"), style = T.small, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
        if (old != null) Ghost(tr("Убрать из списка", "Hide from the list"), Modifier.fillMaxWidth().padding(top = 10.dp), color = C.bad) { c.write { dao.update(old.copy(archived = true)); loadOwn() }; nav.pop() }
    }
}

private fun guessCat(k: Kind, m: List<Muscle>): Cat = when {
    k == Kind.CARDIO -> Cat.CARDIO; k == Kind.MINUTES -> Cat.FLEX; k == Kind.HOLD -> Cat.CORE
    m.any { it == Muscle.QUADS || it == Muscle.GLUTES || it == Muscle.HAMS } -> Cat.LEGS
    m.any { it == Muscle.LATS || it == Muscle.UPPER || it == Muscle.BI } -> Cat.PULL
    m.any { it == Muscle.ABS || it == Muscle.LOWER } -> Cat.CORE
    else -> Cat.PUSH
}

@Composable
fun ResultPage(r: Screen.Result, d: Data, nav: Nav) {
    val ctx = LocalContext.current
    val o = r.outcome
    val w = d.finished.firstOrNull { it.id == r.workoutId }
    Page(tr("Тренировка готова!", "Workout done!"), { nav.pop() }) {
        Box(Modifier.fillMaxWidth().card(22.dp).background(C.accSoft).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                d.team.filter { it.slot != 0 }.take(1).forEach { PetView(it, 90.dp, Pose.CHEER) }
                d.leader?.let { PetView(it, 170.dp, Pose.CHEER) }
                d.team.filter { it.slot != 0 }.drop(1).forEach { PetView(it, 90.dp, Pose.CHEER) }
            }
        }
        Spacer(Modifier.height(12.dp))
        app.pumpling.ui.components.TileGrid(listOf(
            Triple(fmt1(o.food), tr("порций съела команда", "portions for the team"), null),
            Triple("+${o.plates}", tr("блинов", "plates"), null),
            Triple("${o.records.map { it.first.exId to it.second }.distinct().size}", tr("рекордов", "records"), null),
            Triple(w?.let { tr("${maxOf(1, (it.end - it.start) / 60000)} мин", "${maxOf(1, (it.end - it.start) / 60000)} min") } ?: "–", tr("длилась", "long"), null)), icons = listOf(Icons.HEART to C.bad, Icons.PLATE to C.gold, Icons.TROPHY to C.gold, Icons.CLOCK to C.ok))
        if (o.shares.isNotEmpty()) {
            Section(icon = Icons.HEART, text = tr("Кто сколько съел", "Who ate what"))
            Card { d.pets.filter { it.id in o.shares }.sortedBy { it.slot }.forEach { p ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = T.bodyB, modifier = Modifier.weight(1f)); Text(p.sp().name, style = T.small); Spacer(Modifier.width(10.dp))
                    Text(fmt1(o.shares[p.id] ?: 0.0), style = T.numS.copy(color = C.acc)) } }
                Text(tr("Съеденное станет ростом в дни отдыха.", "What they ate becomes growth on rest days."), style = T.small, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (o.bonus.isNotEmpty()) { Section(icon = Icons.BOLT, text = tr("Способности", "Abilities")); Card { o.bonus.forEach { Text("• $it", style = T.body) } } }
        if (o.records.isNotEmpty()) {
            Section(icon = Icons.TROPHY, text = tr("Рекорды", "Records"))
            Card { o.records.groupBy { it.first.exId }.forEach { (exId, l) -> val ex = Catalog.of(exId); val (s, rec) = l.first()
                Row(Modifier.padding(vertical = 5.dp).clickable { nav.share = ShareReq(CardKind.RECORD, s.id) }, verticalAlignment = Alignment.CenterVertically) {
                    app.pumpling.ui.components.Badge(Icons.TROPHY, C.gold)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(ex.name, style = T.bodyB)
                        Text(Records.show(ex, rec, s).substringBefore(" (") + " · " + l.map { it.second.label.lowercase() }.distinct().joinToString(", "), style = T.small) }
                    Icon(Icons.SHARE, C.mute, 18.dp)
                } } }
        }
        if (o.eggs.isNotEmpty()) { Section(icon = Icons.EGG, text = tr("Находки", "Finds"))
            Card { o.eggs.forEach { e -> Row(verticalAlignment = Alignment.CenterVertically) { EggView(e, 64.dp, .5f)
                Text(tr("Яйцо! ", "An egg! ") + app.pumpling.domain.Pets.of(e).find, style = T.body, modifier = Modifier.padding(start = 8.dp)) } } } }
        if (o.hatched.isNotEmpty()) { Section(icon = Icons.EGG, text = tr("Вылупились", "Hatched")); Card { o.hatched.forEach { Text("${it.name} – ${it.sp().name}", style = T.bodyB) } } }
        if (o.quests.isNotEmpty()) { Section(icon = Icons.TARGET, text = tr("Задания", "Quests")); Card { o.quests.forEach { Text("✓ ${it.label}  +${it.plates}", style = T.bodyB.copy(color = C.ok)) } } }
        if (o.newEx.isNotEmpty() && d.finished.size > 1) Text(tr("Новые упражнения: ", "New exercises: ") + o.newEx.joinToString(", ") { Catalog.of(it).name }, style = T.small, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(18.dp))
        Btn(tr("Поделиться открыткой", "Share a card"), Modifier.fillMaxWidth()) { nav.share = ShareReq(CardKind.WORKOUT, r.workoutId) }
        if (w != null && w.routineId == 0L) { Spacer(Modifier.height(8.dp))
            var saved by remember { mutableStateOf(false) }
            Ghost(if (saved) tr("Сохранено в программы ✓", "Saved to routines ✓") else tr("Сохранить как программу", "Save as a routine"), Modifier.fillMaxWidth()) {
                if (!saved) { saved = true; val sets = d.setsBy[w.id].orEmpty().filter { it.done && !it.warmup }
                    val plans = sets.groupBy { it.block }.toSortedMap().values.map { l -> Plan(l.first().exId, l.size, l.first().reps, l.first().weight, l.first().secs) }
                    ctx.container.write { dao.insert(Routine(name = w.title.ifEmpty { tr("Программа", "Routine") }, items = Plan.join(plans), created = System.currentTimeMillis()))
                        say(tr("Программа «${w.title}» сохранена – она в журнале", "Routine «${w.title}» saved – find it in the log")) } } } }
        Spacer(Modifier.height(8.dp))
        Ghost(tr("В зал", "Back to the gym"), Modifier.fillMaxWidth()) { nav.pop(); nav.tab = 0 }
    }
}


/** Adds a piece of the user's own equipment by name; [onAdded] gets the name. */
@Composable
fun GearPopup(d: Data, onClose: () -> Unit, onAdded: (String) -> Unit = {}) {
    val c = LocalContext.current.container
    var name by remember { mutableStateOf("") }
    app.pumpling.ui.components.Popup(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) { app.pumpling.ui.components.Badge(Icons.GYM, C.ok, 40.dp)
            Text(tr("Свой инвентарь", "Own equipment"), style = T.h2, modifier = Modifier.padding(start = 12.dp)) }
        Text(tr("Например: тренажёр Смита, слэмбол, TRX, санки, параллетсы", "For example: Smith machine, slam ball, TRX, sled, parallettes"), style = T.small, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
        Field(name, { name = it }, tr("Название", "Name"), Modifier.fillMaxWidth())
        val dup = d.s.gear.any { it.equals(name.trim(), true) } || Equip.entries.any { it.label.equals(name.trim(), true) }
        if (dup) Text(tr("Такой уже есть", "Already there"), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        Btn(tr("Добавить", "Add"), Modifier.fillMaxWidth(), enabled = name.isNotBlank() && !dup) {
            val n = name.trim(); c.write { settings.update { it.copy(gear = it.gear + n) } }; onAdded(n); onClose() }
    }
}

/**
 * «Start a workout?» – what is about to start, with a choice of routine, so a stray tap never starts one.
 * [first] is a routine id, or 0 for a workout without a routine.
 */
@Composable
fun StartSheet(first: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var pick by remember { mutableStateOf(first) }
    val r = d.routines.firstOrNull { it.id == pick }
    val plans = r?.let { Plan.parse(it.items) }.orEmpty()
    app.pumpling.ui.components.Sheet({ nav.askStart = null }) {
        Text(tr("Начать тренировку?", "Start a workout?"), style = T.h2)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            d.routines.forEach { x -> Chip(x.name, pick == x.id) { pick = x.id } }
            Chip(tr("Без программы", "No routine"), pick == 0L, color = C.ok) { pick = 0L }
        }
        Column(Modifier.padding(top = 14.dp).fillMaxWidth().card(18.dp).padding(14.dp)) {
            if (r == null) {
                Row(verticalAlignment = Alignment.CenterVertically) { app.pumpling.ui.components.Badge(Icons.PLUS, C.ok)
                    Text(tr("Упражнения добавите по ходу – кнопкой «+ Упражнение».", "You add exercises as you go – with «+ Exercise»."), style = T.body, modifier = Modifier.padding(start = 12.dp)) }
            } else {
                Text(count(plans.size, "упражнение", "упражнения", "упражнений", "exercise", "exercises") + " · " + tr("${plans.sumOf { it.sets }} подходов", "${plans.sumOf { it.sets }} sets"), style = T.bodyB)
                plans.forEach { p -> val ex = Catalog.of(p.exId)
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Dot(Color(ex.cat.color), 8.dp)
                        Text(ex.name, style = T.body, modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1)
                        Text(when (ex.kind) { Kind.CARDIO, Kind.MINUTES -> tr("${p.secs / 60} мин", "${p.secs / 60} min"); Kind.HOLD -> "${p.sets}\u00a0×\u00a0${p.secs}" + tr("\u00a0с", "\u00a0s"); else -> "${p.sets}\u00a0×\u00a0${p.reps}" }, style = T.smallB)
                    } }
                Text(tr("Веса подставятся из прошлого раза", "Weights come from last time"), style = T.tiny, modifier = Modifier.padding(top = 10.dp))
            }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Отмена", "Cancel"), Modifier.weight(1f)) { nav.askStart = null }
            Btn(tr("Начать", "Start"), Modifier.weight(1.4f)) { nav.askStart = null; c.write { start(r) }; nav.push(Screen.Live) }
        }
    }
}

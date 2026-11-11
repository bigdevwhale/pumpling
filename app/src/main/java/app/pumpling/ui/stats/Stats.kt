package app.pumpling.ui.stats

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pumpling.domain.Catalog
import app.pumpling.domain.Cat
import app.pumpling.domain.Muscle
import app.pumpling.domain.Rec
import app.pumpling.domain.Records
import app.pumpling.domain.Span
import app.pumpling.domain.Stats
import app.pumpling.domain.clock
import app.pumpling.domain.count
import app.pumpling.domain.dayOf
import app.pumpling.domain.dayShort
import app.pumpling.domain.dur
import app.pumpling.domain.fmt
import app.pumpling.domain.fmt1
import app.pumpling.domain.fmtDayYear
import app.pumpling.domain.hours
import app.pumpling.domain.km
import app.pumpling.domain.pace
import app.pumpling.domain.tonnage
import app.pumpling.domain.tr
import app.pumpling.domain.wu
import app.pumpling.domain.wuA
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.components.Bars
import app.pumpling.ui.components.BodyMap
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.ChipRow
import app.pumpling.ui.components.Dot
import app.pumpling.ui.components.Empty
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.components.Heat
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Line
import app.pumpling.ui.components.Meter
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.Row2
import app.pumpling.ui.components.Section
import app.pumpling.ui.components.Segments
import app.pumpling.ui.components.Split
import app.pumpling.ui.components.TileGrid
import app.pumpling.ui.components.card
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.ShareReq
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T
import java.time.LocalDate
import kotlin.math.abs

/** «+12%» against the period before, or null when there's nothing to compare. */
fun delta(now: Double, before: Double): String? {
    if (before <= 0 || now <= 0) return null
    val p = ((now / before - 1) * 100).toInt(); if (p == 0) return null
    return if (p > 0) "+$p%" else "−${abs(p)}%"
}

/** The period being looked at, kept while the user moves between the overview and a section. */
object Period { var kind by androidx.compose.runtime.mutableIntStateOf(Span.MONTH); var back by androidx.compose.runtime.mutableIntStateOf(0) }

@Composable
fun StatsScreen(d: Data, nav: Nav) {
    val first = d.finished.minOfOrNull { dayOf(it.start) } ?: app.pumpling.domain.today()
    val span = Span.of(Period.kind, Period.back, first)
    val st = remember(d.workouts, d.sets, d.bodies, d.s, Period.kind, Period.back) { Stats(d.workouts, d.sets, d.bodies, d.s, span) }
    val prev = remember(d.workouts, d.sets, d.bodies, d.s, Period.kind, Period.back) { if (Period.kind == Span.ALL) null else Stats(d.workouts, d.sets, d.bodies, d.s, span.prev()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 30.dp)) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Летопись", "Stats"), style = T.title, modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.PERIOD, span = span) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) }
        }
        PeriodBar(span)
        app.pumpling.ui.components.Hint("stats", d.s.seen, Icons.STATS, tr("Летопись", "Stats"), tr("Выберите период вверху. Цветные карточки – главное за него, плитки ниже открывают подробности. Значок «поделиться» делает открытку.",
            "Pick a period at the top. The coloured cards are its highlights, the tiles below open the details. The share icon makes a card."), Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp))
        if (st.allDone.isEmpty()) { Empty(tr("Здесь будет вся статистика. Сделайте первую тренировку!", "All the stats will be here. Do your first workout!")); return@Column }
        Hero(st, prev, d, span)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Section(icon = Icons.SPARK, text = tr("Главное", "Highlights"))
        }
        Highlights(st, d, nav)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Section(icon = Icons.LIST, text = tr("Разделы", "Sections"))
            Parts(st, nav)
            Section(if (Period.kind == Span.MONTH || Period.kind == Span.WEEK) tr("Календарь", "Calendar") else tr("По неделям", "By week"))
            Card {
                val to = minOf(st.span.to, app.pumpling.domain.today()); val from = if (Period.kind == Span.ALL || st.span.days > 120) maxOf(st.span.from, to - 364) else st.span.from
                if (Period.kind == Span.MONTH || Period.kind == Span.WEEK) MonthGrid(st, Period.kind) else Heat(st.heat, from, to)
            }
            Section(icon = Icons.DOTS, text = tr("Ещё", "More"))
            Card(pad = 4.dp) {
                Row2(tr("Подробные цифры", "All the numbers"), tr("время, плотность, самые-самые", "time, density, extremes"), null, Modifier.padding(horizontal = 12.dp), lead = { app.pumpling.ui.components.Badge(Icons.LIST) }) { nav.push(Screen.StatsPart(4)) }
                Row2(tr("Итоги года", "Year in review"), tr("листайте как сторис", "swipe like stories"), null, Modifier.padding(horizontal = 12.dp), lead = { app.pumpling.ui.components.Badge(Icons.SPARK, C.gold) }) { nav.push(Screen.Wrapped(LocalDate.ofEpochDay(st.span.to.coerceAtMost(app.pumpling.domain.today())).year)) }
                Row2(tr("Вес и замеры", "Weight and measurements"), null, null, Modifier.padding(horizontal = 12.dp), lead = { app.pumpling.ui.components.Badge(Icons.SCALE, C.ok) }) { nav.push(Screen.BodyLog) }
            }
        }
    }
}

/** Week · month · year · all, and the arrows to step back in time, on one quiet line. */
@Composable
private fun PeriodBar(span: Span) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clip(RoundedCornerShape(50)).background(C.card2).padding(3.dp)) {
            listOf(Span.WEEK to tr("Нед.", "Wk"), Span.MONTH to tr("Мес.", "Mo"), Span.YEAR to tr("Год", "Yr"), Span.ALL to tr("Всё", "All")).forEach { (k, l) ->
                Text(l, style = T.smallB.copy(color = if (Period.kind == k) C.ink else C.mute), modifier = Modifier.clip(RoundedCornerShape(50)).background(if (Period.kind == k) C.card else Color.Transparent)
                    .clickable { Period.kind = k; Period.back = 0 }.padding(horizontal = 11.dp, vertical = 7.dp)) }
        }
        Spacer(Modifier.weight(1f))
        if (Period.kind != Span.ALL) {
            Box(Modifier.size(36.dp).clip(CircleShape).clickable { Period.back++ }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.ink, 18.dp) }
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(enabled = Period.back > 0) { Period.back-- }, contentAlignment = Alignment.Center) { Icon(Icons.NEXT, if (Period.back > 0) C.ink else C.line, 18.dp) }
        }
    }
}

/** The period in one bright card: workouts, three numbers, the change, the leader cheering. */
@Composable
private fun Hero(st: Stats, prev: Stats?, d: Data, span: Span) {
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.acc, Color(0xFFB8324F))))) {
        d.leader?.let { Box(Modifier.align(Alignment.BottomEnd).padding(end = 4.dp)) { app.pumpling.ui.art.PetView(it, 128.dp, app.pumpling.ui.art.Pose.CHEER) } }
        Column(Modifier.padding(20.dp)) {
            Text(span.label().uppercase(), style = T.sec.copy(color = Color(0xCCFFFFFF)))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text("${st.count}", style = T.big.copy(color = Color.White, fontSize = 52.sp))
                Text("  " + app.pumpling.domain.plural(st.count.toLong(), tr("тренировка", "workout"), tr("тренировки", "workouts"), tr("тренировок", "workouts")), style = T.h3.copy(color = Color.White), modifier = Modifier.padding(bottom = 10.dp))
            }
            prev?.let { delta(st.count.toDouble(), it.count.toDouble()) }?.let {
                Text(tr("$it к прошлому периоду", "$it vs the period before"), style = T.smallB.copy(color = Color.White), modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF)).padding(horizontal = 10.dp, vertical = 4.dp)) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                listOf(hours(st.secs) to tr("в зале", "training"), tonnage(st.tonnage) to tr("поднято", "lifted"), "${st.prs}" to tr("рекордов", "records")).forEach { (v, l) ->
                    Column { Text(v, style = T.num.copy(color = Color.White)); Text(l, style = T.tiny.copy(color = Color(0xCCFFFFFF))) } }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    Spacer(Modifier.height(12.dp))
    var metric by remember { mutableIntStateOf(0) }
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().card(20.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (span.kind == Span.WEEK) tr("По дням", "By day") else if (span.kind == Span.MONTH) tr("По неделям", "By week") else tr("По месяцам", "By month"), style = T.h3, modifier = Modifier.weight(1f))
            listOf(tr("трен.", "wk"), tr("тонны", "tons"), tr("мин", "min")).forEachIndexed { i, l ->
                Text(l, style = T.smallB.copy(color = if (metric == i) C.acc else C.mute), modifier = Modifier.clip(RoundedCornerShape(50)).clickable { metric = i }.padding(horizontal = 8.dp, vertical = 4.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        val b = st.buckets { ws, ss -> when (metric) { 0 -> ws.size.toDouble(); 1 -> ss.sumOf { st.tonnageOf(it) } / 1000; else -> ws.sumOf { (it.end - it.start) / 60000.0 } } }
        Bars(b, h = 110.dp, fmtv = { v -> if (metric == 1) fmt1(v) else fmt(v.toLong()) })
    }
}

/** Facts worth a look, on coloured cards that scroll sideways – each says one thing. */
@Composable
private fun Highlights(st: Stats, d: Data, nav: Nav) {
    data class H(val kicker: String, val big: String, val sub: String, val color: Color, val onClick: (() -> Unit)? = null)
    val list = buildList {
        st.recordSets.firstOrNull()?.let { s -> val ex = Catalog.of(s.exId); val r = s.pr.split(',').mapNotNull { Rec.of(it) }.first()
            add(H(tr("Свежий рекорд", "Latest record"), Records.show(ex, r, s).substringBefore(" ("), ex.name, Color(0xFFE09A2D)) { nav.share = ShareReq(CardKind.RECORD, s.id) }) }
        st.exercises.firstOrNull()?.let { e -> add(H(tr("Любимое упражнение", "Favourite"), e.ex.name, tr("${e.sets} подходов", "${e.sets} sets"), Color(e.ex.cat.color)) { nav.push(Screen.Exercise(e.ex.id)) }) }
        st.exercises.filter { it.growth > 0.02 }.maxByOrNull { it.growth }?.let { e -> add(H(tr("Быстрее всех растёт", "Growing fastest"), "+${(e.growth * 100).toInt()}%", e.ex.name, Color(0xFF2F8F83)) { nav.push(Screen.Exercise(e.ex.id)) }) }
        if (st.count > 0) { val wd = st.weekdays; val bd = wd.indices.maxBy { wd[it] }; val part = st.partOfDay.maxBy { it.second }
            add(H(tr("Ваше время", "Your time"), app.pumpling.domain.weekday(LocalDate.of(2024, 1, 1 + bd).toEpochDay()).replaceFirstChar { it.uppercase() }, part.first, Color(0xFF6B5BD2))) }
        if (st.meters > 0) add(H(tr("Кардио", "Cardio"), km(st.meters), st.journey()?.let { (to, f) -> if (f >= 1) to else tr("${(f * 100).toInt()}% пути: $to", "${(f * 100).toInt()}% of $to") } ?: "", Color(0xFF2F8FD0)))
        if (st.weeks.streak > 0) add(H(tr("Серия", "Streak"), count(st.weeks.streak, "неделя", "недели", "недель", "week", "weeks"), tr("подряд с целью · лучшая ${st.weeks.best}", "in a row · best ${st.weeks.best}"), Color(0xFFE04C8A)))
        st.funFacts().firstOrNull()?.let { add(H(tr("Для сравнения", "For comparison"), "🐘", it, Color(0xFF8C6A4E))) }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { h ->
            Column(Modifier.width(180.dp).height(130.dp).clip(RoundedCornerShape(20.dp)).background(h.color).then(if (h.onClick != null) Modifier.clickable(onClick = h.onClick) else Modifier).padding(14.dp)) {
                Text(h.kicker, style = T.tiny.copy(color = Color(0xD9FFFFFF)))
                Spacer(Modifier.weight(1f))
                Text(h.big, style = T.h2.copy(color = Color.White), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(h.sub, style = T.tiny.copy(color = Color(0xD9FFFFFF)), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The four sections as big tiles, each with its one number. */
@Composable
private fun Parts(st: Stats, nav: Nav) {
    val low = Muscle.entries.filter { st.perWeek(it) < 4 && it != Muscle.FORE }
    val tiles = listOf(
        Triple(tr("Упражнения", "Lifts"), "${st.exercises.size}", st.exercises.firstOrNull()?.ex?.name ?: "–"),
        Triple(tr("Мышцы", "Muscles"), "${fmt(st.setsN)}", if (low.isNotEmpty()) tr("отстают: ", "lagging: ") + low.take(2).joinToString(", ") { it.label.lowercase() } else tr("всё ровно", "all even")),
        Triple(tr("Кардио", "Cardio"), km(st.meters), hours(st.cardioSecs.toLong())),
        Triple(tr("Рекорды", "Records"), "${st.prs}", st.recordSets.firstOrNull()?.let { Catalog.of(it.exId).name } ?: "–"))
    val icons = listOf(Icons.GYM, Icons.PAW, Icons.FLAME, Icons.TROPHY)
    val colors = listOf(Color(Cat.PUSH.color), Color(Cat.LEGS.color), Color(Cat.CARDIO.color), C.gold)
    tiles.chunked(2).forEachIndexed { r, row ->
        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEachIndexed { k, (title, big, sub) -> val i = r * 2 + k
                Column(Modifier.weight(1f).card(20.dp).clickable { nav.push(Screen.StatsPart(i)) }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(colors[i].copy(alpha = .15f)), contentAlignment = Alignment.Center) { Icon(icons[i], colors[i], 18.dp) }
                        Text(title, style = T.bodyB, modifier = Modifier.padding(start = 8.dp).weight(1f), maxLines = 1)
                    }
                    Text(big, style = T.num, modifier = Modifier.padding(top = 10.dp), maxLines = 1)
                    Text(sub, style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** One section of the stats on its own page, for the period chosen on the overview. */
@Composable
fun StatsPartPage(part: Int, d: Data, nav: Nav) {
    val first = d.finished.minOfOrNull { dayOf(it.start) } ?: app.pumpling.domain.today()
    val span = Span.of(Period.kind, Period.back, first)
    val st = remember(d.workouts, d.sets, d.bodies, d.s, Period.kind, Period.back) { Stats(d.workouts, d.sets, d.bodies, d.s, span) }
    val prev = remember(d.workouts, d.sets, d.bodies, d.s, Period.kind, Period.back) { if (Period.kind == Span.ALL) null else Stats(d.workouts, d.sets, d.bodies, d.s, span.prev()) }
    val title = listOf(tr("Упражнения", "Lifts"), tr("Мышцы", "Muscles"), tr("Кардио", "Cardio"), tr("Рекорды", "Records"), tr("Все цифры", "All the numbers"))[part]
    Page(title, { nav.pop() }, action = {
        if (part < 4) Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(listOf(CardKind.LIFTS, CardKind.MUSCLES, CardKind.CARDIO, CardKind.RECORDS)[part], span = span) },
            contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) } }) {
        Text(span.label(), style = T.small)
        when (part) { 0 -> Lifts(st, nav); 1 -> Muscles(st); 2 -> Cardio(st); 3 -> RecordsTab(st, d, nav); else -> Numbers(st, prev, nav) }
    }
}

/** Everything else, for those who like numbers. */
@Composable
private fun Numbers(st: Stats, prev: Stats?, nav: Nav) {
    Spacer(Modifier.height(10.dp))
    TileGrid(listOf(
        Triple(fmt(st.setsN), tr("рабочих подходов", "working sets"), prev?.let { delta(st.setsN.toDouble(), it.setsN.toDouble()) }),
        Triple(fmt(st.reps), tr("повторений", "reps"), prev?.let { delta(st.reps.toDouble(), it.reps.toDouble()) }),
        Triple("≈ ${fmt(st.kcal.toLong())}", tr("ккал", "kcal"), prev?.let { delta(st.kcal, it.kcal) }),
        Triple(fmt1(st.food), tr("порций съела команда", "portions for the team"), null),
        Triple(dur(st.avgSecs.toInt()), tr("тренировка в среднем", "an average workout"), null),
        Triple(if (st.avgRest > 0) clock(st.avgRest) else "–", tr("отдых между подходами", "rest between sets"), null),
        Triple("${fmt1(st.density)} ${tr("кг", "kg")}", tr("в минуту – плотность", "a minute – density"), null),
        Triple("${st.activeDays}", tr("дней в движении", "active days"), null)), icons = listOf(Icons.LIST to C.acc, Icons.ARM to C.acc, Icons.FLAME to C.bad, Icons.HEART to C.gold, Icons.CLOCK to C.ok, Icons.TIMER to C.ok, Icons.BOLT to C.gold, Icons.CAL to C.acc))
    Section(icon = Icons.CLOCK, text = tr("Когда вы тренируетесь", "When you train"))
    Card {
        val wd = st.weekdays; Bars((0..6).map { dayShort(it) to wd[it].toDouble() }, hi = wd.indices.maxBy { wd[it] }, h = 90.dp, fmtv = { fmt(it.toLong()) })
        Spacer(Modifier.height(12.dp))
        val parts = st.partOfDay; val tot = parts.sumOf { it.second }.coerceAtLeast(1)
        parts.forEach { (l, n) -> Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(l, style = T.small, modifier = Modifier.width(80.dp)); Meter(n / tot.toFloat(), Modifier.weight(1f), h = 8.dp); Text("${n * 100 / tot}%", style = T.smallB, modifier = Modifier.width(44.dp), textAlign = TextAlign.End) } }
    }
    st.heaviestWorkout?.let { hw -> Section(icon = Icons.STAR, text = tr("Самые-самые", "The extremes")); Card(pad = 4.dp) {
        Row2(tr("Самая тяжёлая", "Heaviest"), fmtDayYear(dayOf(hw.start)) + " · " + hw.title, tonnage(st.work.filter { it.workoutId == hw.id }.sumOf { st.tonnageOf(it) }), Modifier.padding(horizontal = 12.dp)) { nav.push(Screen.WorkoutView(hw.id)) }
        st.longestWorkout?.let { w -> Row2(tr("Самая долгая", "Longest"), fmtDayYear(dayOf(w.start)) + " · " + w.title, app.pumpling.domain.span(w.end - w.start), Modifier.padding(horizontal = 12.dp)) { nav.push(Screen.WorkoutView(w.id)) } }
        st.earliestWorkout?.let { w -> val t = java.time.Instant.ofEpochMilli(w.start).atZone(app.pumpling.domain.zone).toLocalTime()
            Row2(tr("Самая ранняя", "Earliest"), fmtDayYear(dayOf(w.start)), "%d:%02d".format(t.hour, t.minute), Modifier.padding(horizontal = 12.dp)) { nav.push(Screen.WorkoutView(w.id)) } }
    } }
    val facts = st.funFacts()
    if (facts.isNotEmpty()) { Section(icon = Icons.GLOBE, text = tr("Для сравнения", "For comparison")); Card { facts.forEach { Text("• $it", style = T.body, modifier = Modifier.padding(vertical = 3.dp)) } } }
}

@Composable
private fun Lifts(st: Stats, nav: Nav) {
    var sort by remember { mutableIntStateOf(0) }
    var all by remember { mutableStateOf(false) }
    Spacer(Modifier.height(10.dp))
    ChipRow(listOf(tr("Чаще всего", "Most done"), tr("Тоннаж", "Tonnage"), tr("Рост", "Growth"), tr("Максимум", "Best")), sort) { sort = it }
    Spacer(Modifier.height(8.dp))
    val list = when (sort) { 1 -> st.exercises.sortedByDescending { it.tonnage }; 2 -> st.exercises.sortedByDescending { it.growth }; 3 -> st.exercises.sortedByDescending { it.best }; else -> st.exercises }
    Card(pad = 8.dp) {
        (if (all) list else list.take(7)).forEach { e ->
            Row(Modifier.fillMaxWidth().clickable { nav.push(Screen.Exercise(e.ex.id)) }.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(Color(e.ex.cat.color))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(e.ex.name, style = T.bodyB, maxLines = 1)
                    Text(when (sort) { 1 -> if (e.tonnage > 0) tonnage(e.tonnage) else "–"; else -> tr("${e.sets} подх. · ${e.workouts} трен.", "${e.sets} sets · ${e.workouts} workouts") }, style = T.small)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (e.best > 0) Text("≈ ${wuA(e.best)}", style = T.smallB)
                    if (e.growth != 0.0) Text((if (e.growth > 0) "+" else "−") + "${abs((e.growth * 100).toInt())}%", style = T.tiny.copy(color = if (e.growth > 0) C.ok else C.bad))
                }
            }
        }
        if (list.size > 7) Text(if (all) tr("Свернуть", "Show less") else tr("Показать все (${list.size})", "Show all (${list.size})"), style = T.smallB.copy(color = C.acc),
            modifier = Modifier.fillMaxWidth().clickable { all = !all }.padding(12.dp), textAlign = TextAlign.Center)
    }
    if (st.newExercises.isNotEmpty()) app.pumpling.ui.components.Fold(icon = Icons.SPARK, tint = C.ok, title = tr("Новое за период", "New this period"), st.newExercises.joinToString(", ") { it.name }) { Text(st.newExercises.joinToString(", ") { it.name }, style = T.body) }
    val fav = st.favouriteByMonth()
    if (fav.size >= 2) app.pumpling.ui.components.Fold(icon = Icons.HEART, tint = C.bad, title = tr("Любимое упражнение месяца", "Favourite of each month"), fav.last().let { "${it.first} – ${it.second.name}" }) { fav.forEach { (m, e) -> Row2(m, null, e.name) } }
    app.pumpling.ui.components.Fold(icon = Icons.PAW, tint = C.acc, title = tr("Семейства упражнений", "Exercise families"), st.families.take(3).joinToString(", ") { it.first.label.lowercase() }) {
        val tot = st.families.sumOf { it.second }.coerceAtLeast(1)
        st.families.forEach { (c, n) -> Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(c.label, style = T.small, modifier = Modifier.width(120.dp)); Meter(n / tot.toFloat(), Modifier.weight(1f), color = Color(c.color), h = 8.dp)
            Text("$n", style = T.smallB, modifier = Modifier.width(44.dp), textAlign = TextAlign.End) } } }
    app.pumpling.ui.components.Fold(icon = Icons.GYM, tint = C.mute, title = tr("Инвентарь", "Equipment"), st.equipment.take(3).joinToString(", ") { it.first.lowercase() }) {
        val tot = st.equipment.sumOf { it.second }.coerceAtLeast(1)
        st.equipment.forEach { (e, n) -> Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(e, style = T.small, modifier = Modifier.width(120.dp)); Meter(n / tot.toFloat(), Modifier.weight(1f), h = 8.dp)
            Text("${n * 100 / tot}%", style = T.smallB, modifier = Modifier.width(44.dp), textAlign = TextAlign.End) } } }
}

@Composable
private fun Muscles(st: Stats) {
    val max = (st.muscleSets.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val low = Muscle.entries.filter { st.perWeek(it) < 4 && it != Muscle.FORE }
    Spacer(Modifier.height(10.dp))
    Card { BodyMap(st.muscleSets.mapValues { it.value / max })
        Text(tr("Чем ярче, тем больше подходов", "Brighter means more sets"), style = T.small, modifier = Modifier.padding(top = 6.dp))
        if (low.isNotEmpty()) Text(tr("Отстают: ", "Lagging: ") + low.joinToString(", ") { it.label.lowercase() }, style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 4.dp)) }
    Section(icon = Icons.SWAP, text = tr("Баланс", "Balance"))
    Card {
        val (pu, pl) = st.pushPull; Split(pu, pl, tr("Жимы", "Push"), tr("Тяги", "Pull"))
        Spacer(Modifier.height(14.dp)); val (up, lo) = st.upperLower; Split(up, lo, tr("Верх", "Upper"), tr("Низ", "Lower"), C.acc, Color(Cat.LEGS.color))
        if (pu > 0 && pl > 0 && pu / pl > 1.5) Text(tr("Жимов заметно больше, чем тяг – плечам полезно добавить тяги.", "Far more pushing than pulling – your shoulders would like more rows."), style = T.small.copy(color = C.acc), modifier = Modifier.padding(top = 10.dp))
    }
    app.pumpling.ui.components.Fold(icon = Icons.LIST, tint = C.acc, title = tr("Подходов в неделю", "Sets a week"), tr("ориентир для роста – 10–20 на мышцу", "10–20 per muscle for growth")) {
        Muscle.entries.sortedByDescending { st.perWeek(it) }.forEach { m -> val v = st.perWeek(m)
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(m.label, style = T.small, modifier = Modifier.width(110.dp))
                Meter((v / 20).toFloat(), Modifier.weight(1f), color = if (v in 10.0..20.0) C.ok else if (v > 20) C.gold else C.acc, h = 8.dp)
                Text(fmt1(v), style = T.smallB, modifier = Modifier.width(44.dp), textAlign = TextAlign.End) } }
        val (fr, bk) = st.frontBack; Spacer(Modifier.height(12.dp)); Split(fr, bk, tr("Перед", "Front"), tr("Спина", "Back"), C.gold, Color(Cat.PULL.color))
    }
    app.pumpling.ui.components.Fold(icon = Icons.HEART, tint = C.ok, title = tr("Восстановление сейчас", "Recovery now"), tr("какие мышцы ещё отдыхают", "which muscles are still resting")) {
        BodyMap(st.recovery()); Text(tr("Ярко – мышца ещё отдыхает после недавних тренировок.", "Bright means still resting after recent workouts."), style = T.small) }
}

@Composable
private fun Cardio(st: Stats) {
    if (st.cardio.isEmpty()) { Empty(tr("Кардио за этот период нет", "No cardio in this period")); return }
    Spacer(Modifier.height(12.dp))
    TileGrid(listOf(Triple(km(st.meters), tr("всего", "in all"), null), Triple(hours(st.cardioSecs.toLong()), tr("кардио", "of cardio"), null)), icons = listOf(Icons.ROUTE to C.acc, Icons.CLOCK to C.ok))
    Section(icon = Icons.ROUTE, text = tr("Виды", "Kinds"))
    Card(pad = 8.dp) { st.cardio.forEach { c -> Row2(c.ex.name, listOfNotNull(count(c.n, "раз", "раза", "раз", "time", "times"), dur(c.secs), c.bestPace.takeIf { !it.isNaN() }?.let { tr("лучший темп ", "best pace ") + pace(it) }).joinToString(" · "),
        if (c.meters > 0) km(c.meters) else null, Modifier.padding(horizontal = 8.dp)) } }
    val eff = st.bestEfforts().filter { it.second != null }
    if (eff.isNotEmpty()) { Section(icon = Icons.BOLT, text = tr("Лучшее время бега", "Best running times"))
        Card(pad = 8.dp) { eff.forEach { (l, s) -> s!!; Row2(l, fmtDayYear(st.dayOfSet(s)) + " · " + pace(s.secs / (s.meters / 1000)), clock(s.secs), Modifier.padding(horizontal = 8.dp)) } } }
    val runs = st.work.filter { it.exId in setOf("run", "treadmill", "trail_run") && it.meters >= 1000 && it.secs > 0 }
    if (runs.size >= 3) { Section(tr("Темп бега", "Running pace")); Card { Line(runs.map { st.dayOfSet(it).toDouble() to -(it.secs / (it.meters / 1000)) })
        Text(tr("Выше – быстрее", "Higher is faster"), style = T.tiny) } }
    st.journey()?.let { (to, f) -> Section(icon = Icons.GLOBE, text = tr("Путешествие", "Journey"))
        Card { Text(if (f >= 1) tr("Пройдено: $to", "Done: $to") else tr("Следующая цель: $to", "Next: $to"), style = T.bodyB); Meter(f.coerceAtMost(1f), Modifier.padding(top = 6.dp)); Text(km(st.meters), style = T.small) } }
}

@Composable
private fun RecordsTab(st: Stats, d: Data, nav: Nav) {
    val stds = st.standards()
    Spacer(Modifier.height(10.dp))
    if (stds.isNotEmpty() && d.s.sex.isNotEmpty()) {
        app.pumpling.ui.components.Fold(icon = Icons.TARGET, tint = C.gold, title = tr("Нормативы силы", "Strength standards"), stds.joinToString(", ") { "${it.ex.name.substringBefore(' ').lowercase()} – ${if (it.level >= 0) st.levelNames[it.level].lowercase() else "…"}" }) {
            Text(tr("По расчётному максимуму и весу тела ${wu(st.bw)}", "By estimated max and body weight ${wu(st.bw)}"), style = T.small, modifier = Modifier.padding(bottom = 8.dp))
            stds.forEach { s -> Column(Modifier.padding(vertical = 6.dp).clickable { nav.push(Screen.Exercise(s.ex.id)) }) {
                Row { Text(s.ex.name, style = T.bodyB, modifier = Modifier.weight(1f)); Text(if (s.level >= 0) st.levelNames[s.level] else tr("ещё впереди", "ahead"), style = T.smallB.copy(color = C.acc)) }
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) { (0..4).forEach { i -> Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(50)).background(if (i <= s.level) C.acc else C.track)) } }
                Text("≈ ${wuA(s.e1rm)} · ×${fmt1(s.ratio)} " + tr("веса тела", "body weight") + (s.next?.let { tr(" · следующий уровень с ${wuA(it)}", " · next level at ${wuA(it)}") } ?: ""), style = T.tiny, modifier = Modifier.padding(top = 3.dp))
            } }
            st.total()?.let { (t, dots) -> Spacer(Modifier.height(8.dp)); Row2(tr("Сумма троеборья", "Powerlifting total"), tr("присед + жим + тяга · ${fmt1(dots)} DOTS", "squat + bench + deadlift · ${fmt1(dots)} DOTS"), wuA(t)) }
        }
    } else if (stds.isNotEmpty()) Card(onClick = { nav.push(Screen.Settings) }) {
        Text(tr("Нормативы силы и очки DOTS", "Strength standards and DOTS"), style = T.bodyB); Text(tr("Укажите в настройках вес тела и по каким таблицам считать.", "Set body weight and the tables in Settings."), style = T.small) }
    var allRecs by remember { mutableStateOf(false) }
    Section(icon = Icons.TROPHY, text = tr("Рекорды за период", "Records this period") + " · ${st.recordSets.size}")
    val recs = st.recordSets
    if (recs.isEmpty()) Empty(tr("В этом периоде рекордов нет", "No records in this period"))
    else Card(pad = 8.dp) { recs.take(if (allRecs) 60 else 6).forEach { s -> val ex = Catalog.of(s.exId); val r = s.pr.split(',').mapNotNull { Rec.of(it) }.first()
        Row(Modifier.fillMaxWidth().clickable { nav.share = ShareReq(CardKind.RECORD, s.id) }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.TROPHY, C.gold, 18.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(ex.name, style = T.bodyB); Text("${r.label} · ${fmtDayYear(st.dayOfSet(s))}", style = T.small) }
            Text(Records.show(ex, r, s), style = T.smallB)
        } } }
    if (recs.size > 6) Text(if (allRecs) tr("Свернуть", "Show less") else tr("Показать все", "Show all"), style = T.smallB.copy(color = C.acc),
        modifier = Modifier.fillMaxWidth().clickable { allRecs = !allRecs }.padding(12.dp), textAlign = TextAlign.Center)
    app.pumpling.ui.components.Fold(icon = Icons.STAR, tint = C.gold, title = tr("Лучшее по каждому упражнению", "Bests by exercise"), tr("${st.bests.size} упражнений", "${st.bests.size} exercises")) {
        st.bests.entries.sortedByDescending { it.value.sets }.take(40).forEach { (id, b) -> val ex = Catalog.of(id)
            val v = when { b.e1rm > 0 -> "≈ ${wuA(b.e1rm)}"; b.hold > 0 -> dur(b.hold); b.dist > 0 -> km(b.dist); b.reps > 0 -> count(b.reps, "повтор", "повтора", "повторов", "rep", "reps"); b.time > 0 -> dur(b.time); else -> "–" }
            Row2(ex.name, null, v) { nav.push(Screen.Exercise(id)) } }
    }
}

/** A month (or a week) as a calendar: a dot for each day, brighter for more sets. */
@Composable
private fun MonthGrid(st: Stats, kind: Int) {
    val first = LocalDate.ofEpochDay(st.span.from); val lead = if (kind == Span.MONTH) first.dayOfWeek.value - 1 else 0
    val n = st.span.days; val mx = (st.heat.filterKeys { it in st.span.from..st.span.to }.values.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth()) { (0..6).forEach { Text(dayShort(it), style = T.tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
    (0 until (lead + n + 6) / 7).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            (0..6).forEach { col -> val i = row * 7 + col - lead; val day = st.span.from + i
                Box(Modifier.weight(1f).padding(2.dp).height(36.dp), contentAlignment = Alignment.Center) {
                    if (i in 0 until n) { val v = st.heat[day] ?: 0; val future = day > app.pumpling.domain.today()
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(if (v == 0) C.card2 else C.heat[(1 + v * 3 / mx).coerceAtMost(4)]), contentAlignment = Alignment.Center) {
                            Text("${LocalDate.ofEpochDay(day).dayOfMonth}", style = T.smallB.copy(color = if (v > 0 && v * 3 / mx >= 2) Color.White else if (future) C.line else C.mute)) } }
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

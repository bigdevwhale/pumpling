package app.pumpling.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import app.pumpling.container
import app.pumpling.domain.Catalog
import app.pumpling.domain.Muscle
import app.pumpling.domain.Pets
import app.pumpling.domain.Quests
import app.pumpling.domain.Span
import app.pumpling.domain.Stage
import app.pumpling.domain.Stats
import app.pumpling.domain.Weeks
import app.pumpling.domain.count
import app.pumpling.domain.dayOf
import app.pumpling.domain.fmtDayShort
import app.pumpling.domain.fmt
import app.pumpling.domain.fmt1
import app.pumpling.domain.relDay
import app.pumpling.domain.sp
import app.pumpling.domain.stage
import app.pumpling.domain.today
import app.pumpling.domain.tr
import app.pumpling.domain.weekStart
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.art.GymScene
import app.pumpling.ui.art.Pose
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.Dot
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Meter
import app.pumpling.ui.components.Section
import app.pumpling.ui.components.card
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.ShareReq
import app.pumpling.domain.clock
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T
import java.time.LocalTime

@Composable
fun HomeScreen(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val asleep = LocalTime.now().hour !in 7..22
    val trainedToday = d.finished.any { dayOf(it.start) == today() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pumpling", style = T.title)
                Text(when { d.live != null -> tr("Тренировка идёт", "A workout is on"); trainedToday -> tr("Сегодня уже потренировались", "Trained today"); else -> greeting() }, style = T.small)
            }
            Row(Modifier.clip(RoundedCornerShape(50)).background(C.goldSoft).clickable { nav.push(Screen.Shop) }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.PLATE, C.gold, 18.dp); Spacer(Modifier.width(6.dp)); Text(fmt(d.s.plates), style = T.numS.copy(color = C.gold))
            }
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.push(Screen.Settings) }, contentAlignment = Alignment.Center) { Icon(Icons.GEAR, C.mute, 22.dp) }
        }
        app.pumpling.ui.components.Hint("home", d.s.seen, Icons.PAW, tr("Это ваш зал", "This is your gym"), tr("Здесь живёт команда. Начните тренировку – питомцы будут тренироваться вместе с вами и есть за каждый подход. Растут они в дни отдыха. Нажмите на зал, чтобы обставить его за блины.",
            "Your team lives here. Start a workout and the pets train along, eating for every set. They grow on rest days. Tap the gym to furnish it with plates."), Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).card(22.dp).clickable { nav.push(Screen.Shop) }) {
            val sky = remember(d.s.outside, d.s.lat, System.currentTimeMillis() / 300_000) { app.pumpling.ui.art.Sky.of(d.s) }
            GymScene(d.team, d.s.items - d.s.hidden, if (d.live != null) Pose.WORK else if (asleep) Pose.SLEEP else Pose.IDLE, sky = sky)
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(38.dp).clip(CircleShape).background(Color(0x66000000)).clickable { nav.share = ShareReq(CardKind.GYM) },
                contentAlignment = Alignment.Center) { Icon(Icons.SHARE, Color.White, 19.dp) }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            TodayCard(d, nav)
            StatusCard(d, nav)
            MonthRow(d)
            LastCard(d, nav)
            CheckInCard(d, nav)
            QuestFold(d)
            FreshFold(d)
        }
    }
}

private fun greeting(): String { val h = LocalTime.now().hour
    return when (h) { in 5..11 -> tr("Доброе утро", "Good morning"); in 12..17 -> tr("Добрый день", "Good afternoon"); in 18..22 -> tr("Добрый вечер", "Good evening"); else -> tr("Доброй ночи", "Good night") } }

fun shapeWord(v: Double) = when { v >= 85 -> tr("отличная", "great"); v >= 60 -> tr("хорошая", "good"); v >= 35 -> tr("так себе", "so-so"); else -> tr("пельмешек", "dumpling") }

/** The leader and the week in one glance: stage, shape, the seven days and the streak. */
@Composable
private fun StatusCard(d: Data, nav: Nav) {
    val p = d.leader
    val mon = weekStart(today())
    val days = d.finished.map { dayOf(it.start) }.toSet()
    val weeks = Weeks(days, d.s.weekGoal, d.s.sickDays)
    val n = weeks.count[mon] ?: 0
    Column(Modifier.fillMaxWidth().card(20.dp).clickable { if (p != null) nav.push(Screen.PetInfo(p.id)) else nav.tab = 2 }.padding(16.dp)) {
        if (p == null) Text(tr("Возьмите питомца в команду на вкладке «Питомцы»", "Put a pet on the team in the Pets tab"), style = T.body)
        else {
            val st = p.stage()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(p.sp().color).copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    app.pumpling.ui.art.PetView(p, 50.dp, Pose.IDLE) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = T.h3)
                    Text("${p.sp().name} · ${st.label}" + if (st != Stage.EGG) " · " + tr("форма ${shapeWord(p.shape)}", "shape ${shapeWord(p.shape)}") else "", style = T.small, maxLines = 1)
                }
                if (p.belly > 1) Column(horizontalAlignment = Alignment.End) { Text(fmt1(p.belly), style = T.numS.copy(color = C.acc)); Text(tr("переварит", "to digest"), style = T.tiny) }
            }
            Say(d)
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.SPARK, C.acc, 14.dp)
                Meter(if (st == Stage.EGG) (p.growth / Pets.HATCH).toFloat() else Stage.progress(p), Modifier.weight(1f).padding(horizontal = 8.dp), h = 6.dp)
                Text(if (st == Stage.MASTER) tr("мастер", "master") else "${(Stage.progress(p) * 100).toInt()}%", style = T.tiny)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.CAL, C.ok, 16.dp); Spacer(Modifier.width(6.dp))
            Text(tr("Неделя: $n из ${d.s.weekGoal}", "Week: $n of ${d.s.weekGoal}"), style = T.bodyB, modifier = Modifier.weight(1f))
            if (weeks.streak > 0) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.FLAME, C.acc, 16.dp)
                Text(" " + count(weeks.streak, "неделя", "недели", "недель", "week", "weeks") + tr(" подряд", " in a row"), style = T.smallB) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { i -> val day = mon + i; val on = day in days
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(if (on) C.ok else if (day in d.s.sickDays) C.goldSoft else C.card2), contentAlignment = Alignment.Center) {
                        if (on) Icon(Icons.CHECK, Color.White, 16.dp) else if (day == today()) Dot(C.acc, 7.dp) }
                    Text(app.pumpling.domain.dayShort(i), style = T.tiny, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        if (d.s.sick > 0) Text(tr("Больничный: серии и форма не страдают", "Sick leave: streaks and shape are safe"), style = T.small.copy(color = C.gold), modifier = Modifier.padding(top = 8.dp))
    }
}

/** This week's three quests, folded into one line with how many are done. */
@Composable
private fun QuestFold(d: Data) {
    val c = LocalContext.current.container
    val mon = weekStart(today())
    val week by produceState<app.pumpling.domain.Week?>(null, d.workouts, d.sets, d.bodies) { value = c.weekData(mon) }
    val qs = (if (d.s.questWeek == mon) d.s.quests else Quests.deal(mon)).mapNotNull { Quests.byId[it] }
    val done = qs.count { it.id in d.s.questsDone && d.s.questWeek == mon }
    app.pumpling.ui.components.Fold(icon = Icons.TARGET, tint = C.gold, title = tr("Задания недели · $done из ${qs.size}", "Weekly quests · $done of ${qs.size}"),
        qs.firstOrNull { it.id !in d.s.questsDone }?.let { tr("Дальше: ", "Next: ") + it.label.lowercase() } ?: tr("Все выполнены!", "All done!")) {
        qs.forEachIndexed { i, q ->
            val ok = q.id in d.s.questsDone && d.s.questWeek == mon
            val prog = week?.let { (q.progress(it) / q.need).toFloat().coerceIn(0f, 1f) } ?: 0f
            if (i > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (ok) C.ok else C.card2), contentAlignment = Alignment.Center) { if (ok) Icon(Icons.CHECK, Color.White, 14.dp) }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(q.label, style = T.body.copy(color = if (ok) C.mute else C.ink))
                    if (!ok) Meter(prog, Modifier.padding(top = 4.dp), h = 5.dp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.PLATE, C.gold, 14.dp); Text(" ${q.plates}", style = T.smallB.copy(color = C.gold)) }
            }
        }
    }
}

/** Muscles still resting after recent workouts; nothing at all when everything is fresh. */
@Composable
private fun FreshFold(d: Data) {
    val rec = remember(d.workouts, d.sets) { Stats(d.workouts, d.sets, d.bodies, d.s, Span.of(Span.ALL)).recovery() }
    val tired = Muscle.entries.filter { (rec[it] ?: 0.0) >= .35 }
    if (d.finished.isEmpty()) return
    app.pumpling.ui.components.Fold(icon = Icons.HEART, tint = C.ok, title = tr("Восстановление", "Recovery"),
        if (tired.isEmpty()) tr("Все мышцы отдохнули", "Every muscle is rested") else tr("Отдыхают: ", "Resting: ") + tired.joinToString(", ") { it.label.lowercase() }) {
        app.pumpling.ui.components.BodyMap(rec, color = C.acc, h = 150.dp)
        val fresh = Muscle.entries.filter { (rec[it] ?: 0.0) < .2 && it != Muscle.FORE && it != Muscle.CALVES }
        if (fresh.isNotEmpty()) Text(tr("Свежие: ", "Fresh: ") + fresh.joinToString(", ") { it.label.lowercase() }, style = T.small.copy(color = C.ok), modifier = Modifier.padding(top = 6.dp))
    }
}

/** A nudge to weigh in and measure, every [Settings.checkIn] days, so the body stats have points. */
@Composable
private fun CheckInCard(d: Data, nav: Nav) {
    if (d.s.checkIn <= 0) return
    val last = d.bodies.maxOfOrNull { it.day }
    if (last != null && today() - last < d.s.checkIn) return
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.okSoft).clickable { nav.push(Screen.BodyLog) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        app.pumpling.ui.components.Badge(Icons.SCALE, C.ok, 40.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(tr("Пора взвеситься и измериться", "Time to weigh in and measure"), style = T.bodyB)
            Text(last?.let { tr("Последний раз – ${app.pumpling.domain.fmtDayShort(it)}. Это займёт минуту.", "Last time – ${app.pumpling.domain.fmtDayShort(it)}. It takes a minute.") }
                ?: tr("Первая точка для графика веса и замеров", "The first point for the weight and measurement charts"), style = T.small)
        }
        Icon(Icons.NEXT, C.ok, 18.dp)
    }
}

/** The leader says one thing worth hearing right now: how today went, what's been forgotten, what's next. */
@Composable
private fun Say(d: Data) {
    val p = d.leader ?: return
    if (p.stage() == Stage.EGG) return
    val line = remember(d.finished, d.live, p.belly) {
        val t = today()
        val lastDay = d.finished.maxOfOrNull { dayOf(it.start) }
        val lastBy = HashMap<app.pumpling.domain.Cat, Long>()
        d.finished.forEach { w -> val day = dayOf(w.start); d.setsBy[w.id].orEmpty().filter { it.done }.forEach { x -> val c = Catalog.of(x.exId).cat; if ((lastBy[c] ?: 0) < day) lastBy[c] = day } }
        val forgot = lastBy.filter { t - it.value in 8..60 }.maxByOrNull { t - it.value }
        when {
            d.live != null -> tr("Мы с вами! Каждый подход – мне на обед.", "We're with you! Every set is my lunch.")
            lastDay == t -> if (p.belly > 1) tr("Хорошо поработали. Переварю ${fmt1(p.belly)} – и подрасту.", "Good work. I'll digest ${fmt1(p.belly)} and grow.") else tr("Сегодня уже было. Теперь отдыхаем.", "Done for today. Now we rest.")
            forgot != null -> tr("«${forgot.key.label}» не было уже ${count((t - forgot.value).toInt(), "день", "дня", "дней", "day", "days")}. Может, сегодня?",
                "No «${forgot.key.label.lowercase()}» for ${count((t - forgot.value).toInt(), "день", "дня", "дней", "day", "days")}. Today, maybe?")
            lastDay == null -> tr("Я готов. Начнём, когда скажете.", "I'm ready. We start when you say.")
            t - lastDay >= 4 -> tr("Последний раз мы тренировались ${count((t - lastDay).toInt(), "день", "дня", "дней", "day", "days")} назад. Соскучился!", "Our last workout was ${count((t - lastDay).toInt(), "день", "дня", "дней", "day", "days")} ago. I miss it!")
            p.belly > 1 -> tr("Перевариваю ${fmt1(p.belly)}. В дни отдыха я расту.", "Digesting ${fmt1(p.belly)}. I grow on rest days.")
            else -> tr("Отдохнули – можно и потренироваться.", "Rested – time to train.")
        }
    }
    // a speech bubble hanging from the pet's picture above it
    Box(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)).background(Color(p.sp().color).copy(alpha = .12f))
        .padding(horizontal = 12.dp, vertical = 9.dp)) { Text(line, style = T.small.copy(color = C.ink)) }
}

/** The routine next in line, with its exercises as pictures and one button; hidden once today is done. */
@Composable
private fun TodayCard(d: Data, nav: Nav) {
    if (d.live != null || d.routines.isEmpty() || d.finished.any { dayOf(it.start) == today() }) return
    val c = LocalContext.current.container
    val lastUse = remember(d.finished) { d.finished.filter { it.routineId > 0 }.groupBy { it.routineId }.mapValues { e -> e.value.maxOf { it.start } } }
    val r = d.routines.minByOrNull { lastUse[it.id] ?: 0L } ?: return
    val plans = remember(r.items) { app.pumpling.Plan.parse(r.items) }
    val col = plans.groupBy { Catalog.of(it.exId).cat }.maxByOrNull { e -> e.value.sumOf { it.sets } }?.key?.let { Color(it.color) } ?: C.acc
    Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(20.dp).clickable { nav.push(Screen.RoutineEdit(r.id)) }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("На сегодня", "For today"), style = T.sec.copy(color = col))
                Text(r.name, style = T.h3, maxLines = 1)
                Text(count(plans.size, "упражнение", "упражнения", "упражнений", "exercise", "exercises") + " · " + count(plans.sumOf { it.sets }, "подход", "подхода", "подходов", "set", "sets"), style = T.small)
            }
            Box(Modifier.size(50.dp).clip(CircleShape).background(col).clickable { nav.askStart = r.id }, contentAlignment = Alignment.Center) { Icon(Icons.NEXT, Color.White, 24.dp) }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            plans.take(6).forEach { app.pumpling.ui.art.ExThumb(Catalog.of(it.exId), 42.dp) }
            if (plans.size > 6) Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(C.card2), contentAlignment = Alignment.Center) { Text("+${plans.size - 6}", style = T.smallB) }
        }
    }
}

/** This month in three numbers. */
@Composable
private fun MonthRow(d: Data) {
    if (d.finished.isEmpty()) return
    val m0 = java.time.LocalDate.ofEpochDay(today()).withDayOfMonth(1).toEpochDay()
    val ws = d.finished.filter { dayOf(it.start) >= m0 }
    val st = remember(d.workouts, d.sets) { Stats(d.workouts, d.sets, d.bodies, d.s, Span.of(Span.ALL)) }
    val tons = ws.sumOf { w -> d.setsBy[w.id].orEmpty().filter { it.done && !it.warmup }.sumOf { st.tonnageOf(it) } }
    Text(java.time.LocalDate.ofEpochDay(today()).month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, java.util.Locale(if (app.pumpling.domain.Lang.ru) "ru" else "en")).replaceFirstChar { it.uppercase() },
        style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp))
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple(Icons.GYM, C.acc, "${ws.size}" to count(ws.size, "тренировка", "тренировки", "тренировок", "workout", "workouts").substringAfter(' ')),
            Triple(Icons.ARM, C.ok, app.pumpling.domain.tonnage(tons) to tr("поднято", "lifted")),
            Triple(Icons.TROPHY, C.gold, "${ws.sumOf { it.prs }}" to tr("рекордов", "records"))).forEach { (ic, col, v) ->
            Column(Modifier.weight(1f).fillMaxHeight().card(16.dp).padding(12.dp)) {
                Icon(ic, col, 18.dp)
                Text(v.first, style = T.h3, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                Text(v.second, style = T.tiny)
            }
        }
    }
}

/** The last workout in one line, open on tap. */
@Composable
private fun LastCard(d: Data, nav: Nav) {
    val w = d.finished.maxByOrNull { it.start } ?: return
    val work = d.setsBy[w.id].orEmpty().filter { it.done && !it.warmup }
    val cats = work.groupingBy { Catalog.of(it.exId).cat }.eachCount().entries.sortedByDescending { it.value }
    val col = cats.firstOrNull()?.key?.let { Color(it.color) } ?: C.acc
    Text(tr("Последняя тренировка", "Last workout"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp))
    Row(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.WorkoutView(w.id)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        work.firstOrNull()?.let { app.pumpling.ui.art.ExThumb(Catalog.of(it.exId), 44.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(w.title.ifEmpty { tr("Тренировка", "Workout") }, style = T.bodyB, maxLines = 1)
            Text(relDay(dayOf(w.start)).replaceFirstChar { it.uppercase() } + " · " + app.pumpling.domain.span(w.end - w.start) + (if (w.prs > 0) " · " + count(w.prs, "рекорд", "рекорда", "рекордов", "record", "records") else ""), style = T.small, maxLines = 1)
            if (cats.isNotEmpty()) Row(Modifier.padding(top = 6.dp).width(140.dp).height(4.dp).clip(RoundedCornerShape(50))) {
                cats.forEach { (cc, n) -> Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(Color(cc.color))) } }
        }
        Icon(Icons.NEXT, C.mute, 18.dp)
    }
}

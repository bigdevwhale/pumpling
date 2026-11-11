package app.pumpling.ui.stats

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pumpling.domain.Catalog
import app.pumpling.domain.Kind
import app.pumpling.domain.Rec
import app.pumpling.domain.Records
import app.pumpling.domain.Span
import app.pumpling.domain.Stats
import app.pumpling.domain.clock
import app.pumpling.domain.count
import app.pumpling.domain.e1rm
import app.pumpling.domain.fmt
import app.pumpling.domain.fmtDayYear
import app.pumpling.domain.hours
import app.pumpling.domain.km
import app.pumpling.domain.setLine
import app.pumpling.domain.tonnage
import app.pumpling.domain.tr
import app.pumpling.domain.wu
import app.pumpling.domain.wuA
import app.pumpling.domain.sp
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.art.PetView
import app.pumpling.ui.art.Pose
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.ChipRow
import app.pumpling.ui.components.Empty
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Line
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.Row2
import app.pumpling.ui.components.Section
import app.pumpling.ui.components.TileGrid
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.ShareReq
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T
import java.time.LocalDate

@Composable
fun ExercisePage(exId: String, d: Data, nav: Nav) {
    val ex = Catalog.of(exId)
    val st = remember(d.workouts, d.sets) { Stats(d.workouts, d.sets, d.bodies, d.s, Span.of(Span.ALL)) }
    val hist = remember(st) { st.history(exId) }
    val all = st.allDone.filter { it.exId == exId && !it.warmup }
    Page(ex.name, { nav.pop() }, action = {
        if (ex.own) Text(tr("Изменить", "Edit"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(end = 4.dp).clickable { nav.push(Screen.OwnEdit(exId.drop(1).toLong())) })
        if (all.isNotEmpty()) Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.EXERCISE, ex = exId) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) } }) {
        Text((ex.primary.map { it.label } + ex.secondary.map { it.label.lowercase() }).joinToString(", ") + " · " + ex.equipLabel + " · " + ex.cat.label, style = T.small, modifier = Modifier.padding(bottom = 10.dp))
        if (all.isEmpty()) { Empty(tr("Это упражнение ещё не делали", "Not done yet")); return@Page }
        val b = st.bests[exId]
        TileGrid(listOfNotNull(
            Triple("${hist.size}", tr("тренировок", "workouts"), null), Triple("${all.size}", tr("подходов", "sets"), null),
            if (ex.kind == Kind.WEIGHT || ex.bodyweight) Triple(fmt(all.sumOf { it.reps }), tr("повторений", "reps"), null) else Triple(hours(all.sumOf { it.secs }.toLong()), tr("всего", "in all"), null),
            when { b != null && b.e1rm > 0 -> Triple("≈ ${wuA(b.e1rm)}", tr("расчётный максимум", "estimated max"), null); ex.kind == Kind.CARDIO -> Triple(km(all.sumOf { it.meters }), tr("всего", "in all"), null)
                b != null && b.hold > 0 -> Triple(clock(b.hold), tr("дольше всего", "longest"), null); b != null && b.reps > 0 -> Triple("${b.reps}", tr("повторов за подход", "reps in a set"), null); else -> null },
            if (ex.kind == Kind.WEIGHT) Triple(tonnage(all.sumOf { st.tonnageOf(it) }), tr("тоннаж", "tonnage"), null) else null,
            Triple(fmtDayYear(hist.first().first), tr("первый раз", "first time"), null)), icons = listOf(Icons.CAL to C.acc, Icons.LIST to C.acc, Icons.ARM to C.ok, Icons.TROPHY to C.gold, Icons.GYM to C.acc, Icons.STAR to C.gold))
        var metric by remember { mutableIntStateOf(0) }
        if (hist.size >= 2) {
            Section(icon = Icons.STATS, text = tr("Прогресс", "Progress"))
            Card {
                val opts = when (ex.kind) { Kind.WEIGHT -> listOf(tr("Макс. (расчёт)", "Est. max"), tr("Лучший вес", "Top weight"), tr("Тоннаж", "Tonnage"))
                    Kind.CARDIO -> listOf(tr("Дистанция", "Distance"), tr("Темп", "Pace"), tr("Время", "Time")); Kind.HOLD, Kind.MINUTES -> listOf(tr("Время", "Time")); else -> listOf(tr("Повторы", "Reps"), tr("Добавочный вес", "Added weight")) }
                if (opts.size > 1) ChipRow(opts, metric.coerceAtMost(opts.size - 1)) { metric = it }
                Spacer(Modifier.height(10.dp))
                val pts = hist.map { (day, vol, s) -> day.toDouble() to when (ex.kind) {
                    Kind.WEIGHT -> when (metric) { 0 -> e1rm(s.weight, s.reps); 1 -> all.filter { st.dayOfSet(it) == day }.maxOf { it.weight }; else -> vol }
                    Kind.CARDIO -> when (metric) { 0 -> s.meters / 1000; 1 -> if (s.meters > 0) -(s.secs / (s.meters / 1000)) / 60 else 0.0; else -> s.secs / 60.0 }
                    Kind.HOLD, Kind.MINUTES -> s.secs.toDouble()
                    else -> if (metric == 0) s.reps.toDouble() else s.weight } }
                Line(pts)
                Text("${fmtDayYear(hist.first().first)} – ${fmtDayYear(hist.last().first)}", style = T.tiny)
            }
        }
        if (ex.kind == Kind.WEIGHT) {
            Section(icon = Icons.ARM, text = tr("Лучший вес на число повторов", "Best weight per rep count"))
            val table = st.repTable(exId).filter { it.second != null }
            // a row only where the weight is more than for one rep more: the rest repeat the same set
            val rows = table.filterIndexed { i, (_, s) -> i == table.lastIndex || s!!.weight > table[i + 1].second!!.weight }
            Card(pad = 8.dp) { rows.forEach { (r, s) -> s!!
                Row2(count(r, "повтор", "повтора", "повторов", "rep", "reps"), fmtDayYear(st.dayOfSet(s)), "${wu(s.weight)}" + if (s.reps > r) " (×${s.reps})" else "", Modifier.padding(horizontal = 8.dp)) } }
        }
        val recs = all.filter { it.pr.isNotEmpty() }.sortedByDescending { st.dayOfSet(it) }
        if (recs.isNotEmpty()) { Section(icon = Icons.TROPHY, text = tr("Рекорды", "Records") + " · ${recs.size}")
            Card(pad = 8.dp) { recs.take(12).forEach { s -> val r = s.pr.split(',').mapNotNull { Rec.of(it) }.first()
                Row2(Records.show(ex, r, s), r.label + " · " + fmtDayYear(st.dayOfSet(s)), null, Modifier.padding(horizontal = 8.dp)) { nav.share = ShareReq(CardKind.RECORD, s.id) } } } }
        Section(icon = Icons.LOG, text = tr("История", "History"))
        Card(pad = 8.dp) {
            all.groupBy { it.workoutId }.entries.sortedByDescending { st.dayOfSet(it.value.first()) }.take(30).forEach { (w, l) ->
                Row2(fmtDayYear(st.dayOfSet(l.first())), l.sortedBy { it.idx }.joinToString(", ") { setLine(ex, it) }, null, Modifier.padding(horizontal = 8.dp)) { nav.push(Screen.WorkoutView(w)) } }
        }
    }
}

/** The year in review, page by page, like a story. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedPage(year: Int, d: Data, nav: Nav) {
    val span = Span(LocalDate.of(year, 1, 1).toEpochDay(), LocalDate.of(year, 12, 31).toEpochDay(), Span.YEAR)
    val st = remember(d.workouts, d.sets) { Stats(d.workouts, d.sets, d.bodies, d.s, span) }
    androidx.activity.compose.BackHandler { nav.pop() }
    val pages = buildList<Triple<String, String, String>> {
        add(Triple(tr("Ваш $year", "Your $year"), tr("в Pumpling", "in Pumpling"), tr("Листайте вправо", "Swipe right")))
        add(Triple("${st.count}", tr("тренировок", "workouts"), tr("${st.activeDays} дней с движением и ${hours(st.secs)} в зале", "${st.activeDays} active days and ${hours(st.secs)} training")))
        if (st.tonnage > 0) add(Triple(tonnage(st.tonnage), tr("поднято за год", "lifted this year"), st.funFacts().firstOrNull() ?: ""))
        st.exercises.firstOrNull()?.let { add(Triple(it.ex.name, tr("упражнение года", "exercise of the year"), tr("${it.sets} подходов за ${it.workouts} тренировок", "${it.sets} sets in ${it.workouts} workouts"))) }
        if (st.prs > 0) add(Triple("${st.prs}", tr("рекордов", "records"), st.recordSets.firstOrNull()?.let { s -> val ex = Catalog.of(s.exId); val r = s.pr.split(',').mapNotNull { Rec.of(it) }.first()
            tr("Последний: ${ex.name}, ${Records.show(ex, r, s)}", "Latest: ${ex.name}, ${Records.show(ex, r, s)}") } ?: ""))
        if (st.meters > 0) add(Triple(km(st.meters), tr("кардио", "of cardio"), st.journey()?.let { (to, f) -> if (f >= 1) to else tr("${(f * 100).toInt()}% пути $to", "${(f * 100).toInt()}% of $to") } ?: ""))
        val wd = st.weekdays; val best = wd.indices.maxBy { wd[it] }
        add(Triple(app.pumpling.domain.weekday(LocalDate.of(2024, 1, 1 + best).toEpochDay()).replaceFirstChar { it.uppercase() }, tr("ваш день", "your day"), tr("Лучшая серия – ${st.weeks.best} недель подряд", "Best streak – ${st.weeks.best} weeks in a row")))
        st.favouriteByMonth().let { if (it.size >= 3) add(Triple(tr("Месяцы", "Months"), tr("любимое упражнение", "favourites"), it.joinToString("\n") { (m, e) -> "$m – ${e.name}" })) }
        add(Triple(d.leader?.name ?: "Pumpling", d.leader?.sp()?.name ?: "", tr("Тренировался с вами весь год. До встречи в следующем!", "Trained with you all year. See you next year!")))
    }
    val pager = rememberPagerState { pages.size }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE8613C), Color(0xFF7A3FB0)))).statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i -> val (big, mid, small) = pages[i]
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (i == 0 || i == pages.lastIndex) d.leader?.let { PetView(it, 200.dp, Pose.CHEER) }
                Text(big, style = T.big.copy(color = Color.White, fontSize = if (big.length > 10) 34.sp else 56.sp, lineHeight = 60.sp), textAlign = TextAlign.Center)
                Text(mid, style = T.h2.copy(color = Color.White), textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Text(small, style = T.body.copy(color = Color(0xEEFFFFFF)), textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pages.indices.forEach { Box(Modifier.width(22.dp).height(4.dp).clip(RoundedCornerShape(50)).background(if (it <= pager.currentPage) Color.White else Color(0x55FFFFFF))) } }
        Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(44.dp).clip(CircleShape).clickable { nav.pop() }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, Color.White, 22.dp) }
        Btn(tr("Поделиться годом", "Share the year"), Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth(), color = Color.White, ink = Color(0xFFE8613C)) { nav.share = ShareReq(CardKind.YEAR, span = span) }
    }
}


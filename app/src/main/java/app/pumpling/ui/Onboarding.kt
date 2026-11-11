package app.pumpling.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pumpling.container
import app.pumpling.data.Routine
import app.pumpling.data.Body
import androidx.compose.foundation.layout.size
import app.pumpling.domain.Pets
import app.pumpling.domain.Stage
import app.pumpling.domain.toKg
import app.pumpling.domain.today
import app.pumpling.domain.tr
import app.pumpling.domain.unit
import app.pumpling.ui.art.Creature
import app.pumpling.ui.art.EggView
import app.pumpling.ui.art.Pose
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Chip
import app.pumpling.ui.components.Field
import app.pumpling.ui.components.Segments
import app.pumpling.ui.components.card
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var step by remember { mutableIntStateOf(0) }
    val places = remember { androidx.compose.runtime.mutableStateListOf<app.pumpling.ui.journal.Place>() }
    val kit = remember { androidx.compose.runtime.mutableStateListOf<app.pumpling.ui.journal.Kit>() }
    var level by remember { mutableStateOf(app.pumpling.ui.journal.Level.NEW) }
    val chosen = remember { androidx.compose.runtime.mutableStateListOf<app.pumpling.ui.journal.Template>() }
    val goals = remember { androidx.compose.runtime.mutableStateListOf<app.pumpling.ui.journal.Goal>() }
    var checkIn by remember { mutableIntStateOf(14) }
    var pick by remember { mutableStateOf(Pets.starters[0].id) }
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableIntStateOf(3) }
    var lb by remember { mutableStateOf(false) }
    var bw by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (step) {
            0 -> {
                Spacer(Modifier.height(30.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Creature("otter", Stage.ADULT, Pose.CHEER, 110.dp); Creature("bull", Stage.ADULT, Pose.WORK, 160.dp); Creature("lynx", Stage.ADULT, Pose.CHEER, 110.dp) }
                Text("Pumpling", style = T.title.copy(fontSize = T.big.fontSize), modifier = Modifier.padding(top = 16.dp))
                Text(tr("Питомец, который тренируется с вами", "A pet that trains with you"), style = T.h3.copy(color = C.acc), textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                listOf(
                    tr("Записывайте подходы, веса и кардио – питомец в это время делает то же самое.", "Log sets, weights and cardio – your pet does the same alongside."),
                    tr("Тренировка его кормит, а растёт он в дни отдыха – как настоящие мышцы.", "Training feeds it, and it grows on rest days – like real muscles."),
                    tr("Двадцать питомцев со своими способностями. Яйца находятся по тому, как вы тренируетесь.", "Twenty pets with their own abilities. Eggs turn up from the way you train."),
                    tr("Очень много статистики и красивые открытки, чтобы делиться.", "Loads of statistics and pretty cards to share."),
                ).forEach { Text("• $it", style = T.body, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
                Spacer(Modifier.height(24.dp))
                Btn(tr("Выбрать яйцо", "Pick an egg"), Modifier.fillMaxWidth()) { step = 1 }
            }
            1 -> {
                Text(tr("Первое яйцо", "Your first egg"), style = T.title, modifier = Modifier.padding(top = 10.dp))
                Text(tr("Оно вылупится после первой тренировки. Остальных питомцев найдёте потом.", "It hatches after your first workout. The others turn up later."), style = T.small, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Pets.starters.forEach { sp ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).card(20.dp).border(if (pick == sp.id) 3.dp else 0.dp, C.acc, RoundedCornerShape(20.dp))
                        .clickable { pick = sp.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box { Creature(sp.id, Stage.ADULT, if (pick == sp.id) Pose.WORK else Pose.IDLE, 96.dp) }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(sp.name, style = T.h3)
                            Text(sp.ability, style = T.small)
                        }
                    }
                }
                Field(name, { name = it }, tr("Имя (можно позже)", "Name (or later)"), Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Btn(tr("Дальше", "Next"), Modifier.fillMaxWidth()) { step = 2 }
            }
            2 -> {
                Box(Modifier.padding(top = 10.dp)) { EggView(pick, 140.dp, .7f) }
                Text(tr("Немного о вас", "A little about you"), style = T.title)
                Spacer(Modifier.height(16.dp))
                Text(tr("Сколько тренировок в неделю?", "How many workouts a week?"), style = T.bodyB, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { (1..6).forEach { n -> Chip("$n", goal == n) { goal = n } } }
                Text(tr("Пропуски не наказываются: важна неделя, а не каждый день.", "Missed days aren't punished: it's the week that counts."), style = T.small, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                Spacer(Modifier.height(16.dp))
                Text(tr("Веса", "Weights"), style = T.bodyB, modifier = Modifier.fillMaxWidth())
                Segments(listOf(tr("килограммы", "kilograms"), tr("фунты", "pounds")), if (lb) 1 else 0, Modifier.padding(top = 8.dp)) { lb = it == 1 }
                Spacer(Modifier.height(16.dp))
                Text(tr("Ваш вес, ${if (lb) "lb" else unit()} – по желанию", "Your weight, ${if (lb) "lb" else unit()} – optional"), style = T.bodyB, modifier = Modifier.fillMaxWidth())
                Field(bw, { bw = it }, "–", Modifier.fillMaxWidth().padding(top = 6.dp), number = true)
                Text(tr("Для нормативов силы и тоннажа подтягиваний.", "For strength standards and pull-up tonnage."), style = T.small, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                Spacer(Modifier.height(24.dp))
                Btn(tr("Дальше", "Next"), Modifier.fillMaxWidth()) { step = 3 }
            }
            3 -> {
                Text(tr("Где и как вы тренируетесь?", "Where and how do you train?"), style = T.title, modifier = Modifier.padding(top = 10.dp), textAlign = TextAlign.Center)
                Text(tr("Подберу пару программ – потом их можно поменять", "I'll pick a couple of routines – you can change them later"), style = T.small, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Text(tr("Где занимаетесь? Можно несколько", "Where? Pick any"), style = T.bodyB, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    app.pumpling.ui.journal.Place.entries.forEach { pl -> val on = pl in places
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) C.acc else C.card).clickable { if (on) places.remove(pl) else places.add(pl) }.padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            app.pumpling.ui.components.Icon(app.pumpling.ui.journal.placeIcon(pl), if (on) C.accInk else C.mute, 26.dp)
                            Text(pl.label, style = T.smallB.copy(color = if (on) C.accInk else C.ink), modifier = Modifier.padding(top = 6.dp)) } } }
                if (places.any { it != app.pumpling.ui.journal.Place.GYM }) {
                    Spacer(Modifier.height(16.dp))
                    Text(tr("Что есть дома или во дворе?", "What do you have at home or outside?"), style = T.bodyB, modifier = Modifier.fillMaxWidth())
                    androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        app.pumpling.ui.journal.Kit.entries.forEach { k -> Chip(k.label, k in kit) { if (k in kit) kit.remove(k) else kit.add(k) } } }
                    Text(tr("Ничего нет – тоже хорошо: есть программы на своём весе", "Nothing? Fine – there are bodyweight routines"), style = T.tiny, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(tr("Зачем тренируетесь? Можно несколько", "What for? Pick any"), style = T.bodyB, modifier = Modifier.fillMaxWidth())
                androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    app.pumpling.ui.journal.Goal.entries.forEach { g -> Chip(g.label, g in goals, color = C.ok) { if (g in goals) goals.remove(g) else goals.add(g) } } }
                Spacer(Modifier.height(16.dp))
                Text(tr("Ваш уровень", "Your level"), style = T.bodyB, modifier = Modifier.fillMaxWidth())
                app.pumpling.ui.journal.Level.entries.forEach { lv -> val on = level == lv
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) C.accSoft else C.card)
                        .border(if (on) 2.dp else 0.dp, C.acc, RoundedCornerShape(16.dp)).clickable { level = lv }.padding(14.dp)) {
                        Column { Text(lv.label, style = T.bodyB); Text(lv.about, style = T.small) } } }
                Spacer(Modifier.height(24.dp))
                Btn(tr("Подобрать программы", "Suggest routines"), Modifier.fillMaxWidth(), enabled = places.isNotEmpty()) {
                    chosen.clear(); chosen.addAll(app.pumpling.ui.journal.suggest(places.toSet(), kit.toSet(), level, goals.toSet()).take(2)); step = 4 }
                if (places.isEmpty()) Text(tr("Выберите, где занимаетесь", "Choose where you train"), style = T.small, modifier = Modifier.padding(top = 6.dp))
            }
            else -> {
                val all = app.pumpling.ui.journal.suggest(places.toSet(), kit.toSet(), level, goals.toSet())
                Text(tr("Ваши первые программы", "Your first routines"), style = T.title, modifier = Modifier.padding(top = 10.dp), textAlign = TextAlign.Center)
                Text(tr("Отметьте, какие взять. Позже найдёте ещё в журнале.", "Tick the ones to take. More are in the log later."), style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 14.dp))
                if (all.isEmpty()) Text(tr("Подходящих готовых программ нет – соберёте свою в журнале.", "No ready routine fits – build your own in the log."), style = T.body)
                all.take(6).forEach { t -> val on = t in chosen
                    app.pumpling.ui.journal.TemplateRow(t, app.pumpling.ui.Data(app.pumpling.data.Settings(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), trailing = {
                        Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (on) C.acc else C.card2), contentAlignment = Alignment.Center) {
                            if (on) app.pumpling.ui.components.Icon(app.pumpling.ui.components.Icons.CHECK, C.accInk, 16.dp) } }) { if (on) chosen.remove(t) else chosen.add(t) } }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.card).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { app.pumpling.ui.components.Badge(app.pumpling.ui.components.Icons.SCALE, C.ok)
                        Column(Modifier.padding(start = 12.dp)) { Text(tr("Взвешиваться и мерить талию?", "Weigh in and measure?"), style = T.bodyB)
                            Text(tr("Напомню на главной – так в статистике будет видно, как меняется тело", "I'll remind you on the home screen, so the stats show how your body changes"), style = T.small) } }
                    androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7 to tr("раз в неделю", "weekly"), 14 to tr("раз в 2 недели", "every 2 weeks"), 30 to tr("раз в месяц", "monthly"), 0 to tr("не надо", "no")).forEach { (n, l) ->
                            Chip(l, checkIn == n, color = C.ok) { checkIn = n } } }
                }
                Spacer(Modifier.height(20.dp))
                Btn(if (chosen.isEmpty()) tr("В зал без программ", "To the gym, no routines") else tr("Взять (${chosen.size}) и в зал!", "Take ${chosen.size} and go!"), Modifier.fillMaxWidth()) {
                    app.pumpling.domain.Units.lb = lb
                    val kg = bw.replace(',', '.').toDoubleOrNull()?.let { toKg(it) } ?: 0.0
                    c.write {
                        val p = newPet(pick, 0).let { if (name.isNotBlank()) it.copy(name = name.trim()) else it }.copy(growth = 20.0, shiny = false)
                        dao.insert(p)
                        chosen.forEachIndexed { i, t -> dao.insert(Routine(name = tr(t.ru, t.en), items = t.items, created = System.currentTimeMillis() + i, order = i)) }
                        settings.update { it.copy(onboarded = true, weekGoal = goal, lb = lb, bodyweight = kg, digested = today() - 1, plates = 50, goals = goals.map { g -> g.name }.toSet(), checkIn = checkIn) }
                        if (kg > 0) addBody(Body(day = today(), weight = kg))
                    }
                }
            }
        }
    }
}

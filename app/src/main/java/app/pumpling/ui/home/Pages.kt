package app.pumpling.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pumpling.container
import app.pumpling.data.OutsideSource
import app.pumpling.domain.Lang
import app.pumpling.currentLanguage
import app.pumpling.domain.Form
import app.pumpling.domain.Pets
import app.pumpling.domain.Shop
import app.pumpling.domain.Stage
import app.pumpling.domain.fmt
import app.pumpling.domain.fmt1
import app.pumpling.domain.nat
import app.pumpling.domain.sp
import app.pumpling.domain.today
import app.pumpling.domain.toKg
import app.pumpling.domain.tr
import app.pumpling.domain.unit
import app.pumpling.domain.w
import app.pumpling.setLanguage
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.art.Art
import app.pumpling.ui.art.Creature
import app.pumpling.ui.art.EggView
import app.pumpling.ui.art.GymScene
import app.pumpling.ui.art.Pose
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.Chip
import app.pumpling.ui.components.Field
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.components.Icon
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.Popup
import app.pumpling.ui.components.Section
import app.pumpling.ui.components.Segments
import app.pumpling.ui.components.card
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T

/** News waiting to be seen, one at a time: an egg found, a hatching, a new stage. */
@Composable
fun Inbox(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val msg = d.s.inbox.firstOrNull() ?: return
    val parts = msg.split(':')
    fun close() = c.write { settings.update { s -> s.copy(inbox = s.inbox.drop(1)) } }
    when (parts[0]) {
        "egg" -> { val sp = Pets.of(parts[1])
            Popup(::close) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { EggView(sp.id, 150.dp, .8f) }
                Text(tr("Найдено яйцо!", "An egg!"), style = T.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(tr("В нём кто-то шевелится. ", "Something stirs inside. ") + sp.find, style = T.body, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                Text(tr("Возьмите его в команду – и оно вылупится после тренировки.", "Put it on the team and it hatches with training."), style = T.small, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp)); Btn(tr("Посмотреть", "Have a look"), Modifier.fillMaxWidth()) { close(); nav.tab = 2 }
            } }
        "hatch", "stage" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() }
            if (p == null) { close(); return }
            val st = if (parts[0] == "hatch") Stage.BABY else Stage.entries.getOrElse(parts.getOrNull(2)?.toIntOrNull() ?: 1) { Stage.BABY }
            Popup(::close) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Creature(p.species, st, Pose.CHEER, 170.dp, p.shape.toFloat(), p.shiny) }
                Text(if (parts[0] == "hatch") tr("Вылупился ${p.sp().name.lowercase()}!", "A ${p.sp().name.lowercase()} hatched!") else "${p.name}: ${st.label.lowercase()}", style = T.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(if (parts[0] == "hatch") tr("Зовут ${p.name}. Характер: ${p.nat().label.lowercase()} – ${p.nat().about}.", "Name: ${p.name}. Nature: ${p.nat().label.lowercase()} – ${p.nat().about}.")
                    else if (st == Stage.ADULT) tr("Вырос! Телосложение: ${Form.of(p.form)?.label ?: ""} – ${Form.of(p.form)?.about ?: ""}.", "All grown up! Build: ${Form.of(p.form)?.label ?: ""} – ${Form.of(p.form)?.about ?: ""}.")
                    else if (st == Stage.MASTER) tr("Мастер: вокруг него теперь светится золото.", "A master now, with a golden glow.")
                    else tr("Подрос на тренировках и отдыхе.", "Grown with training and rest."), style = T.body, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                if (p.shiny && parts[0] == "hatch") Text(tr("Редкий окрас – такой бывает один раз из шестнадцати!", "A rare colouring – one in sixteen!"), style = T.smallB.copy(color = C.gold), modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp)); Btn(tr("Ура!", "Hooray!"), Modifier.fillMaxWidth()) { close() }
            } }
        else -> close()
    }
}

@Composable
fun ShopPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var preview by remember { mutableStateOf<String?>(null) }
    Page(tr("Зал", "The gym"), { nav.pop() }, action = {
        Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.PLATE, C.gold, 18.dp); Text(" ${fmt(d.s.plates)}", style = T.numS.copy(color = C.gold)) } }) {
        Box(Modifier.card(22.dp)) { GymScene(d.team, d.s.items - d.s.hidden, Pose.IDLE, preview = preview, sky = app.pumpling.ui.art.Sky.of(d.s)) }
        app.pumpling.ui.components.Hint("shop", d.s.seen, Icons.PLATE, tr("Обставьте зал", "Furnish the gym"), tr("Блины дают за тренировки, рекорды и задания. Нажмите на вещь – она появится в зале на примерку.", "Plates come from workouts, records and quests. Tap a thing to try it in the gym."), Modifier.padding(vertical = 12.dp))
        if ("shop" in d.s.seen) Spacer(Modifier.height(12.dp))
        val ctx = LocalContext.current
        Shop.all.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { it ->
                    val owned = it.id in d.s.items; val shown = owned && it.id !in d.s.hidden
                    Column(Modifier.weight(1f).card(18.dp).clickable { preview = it.id }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val img = Art.item(ctx, it.id)
                        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                            if (img != null) androidx.compose.foundation.Image(img, null, Modifier.size(80.dp), filterQuality = FilterQuality.Medium) }
                        Text(it.name, style = T.smallB, textAlign = TextAlign.Center, maxLines = 2)
                        Spacer(Modifier.height(8.dp))
                        if (owned) Ghost(if (shown) tr("Убрать", "Put away") else tr("Поставить", "Put out"), small = true) {
                            c.write { settings.update { s -> s.copy(hidden = if (shown) s.hidden + it.id else s.hidden - it.id) } } }
                        else Btn("${it.price}", enabled = d.s.plates >= it.price, small = true, color = C.gold) { c.write { if (buy(it.id)) say(tr("${it.name} – в зале!", "${it.name} is in the gym!")) } }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A settings block: an icon, what it is, a line on why, and its control under it. */
@Composable
private fun Setting(icon: Icons, tint: androidx.compose.ui.graphics.Color, title: String, sub: String? = null, content: @Composable () -> Unit = {}) {
    Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(20.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app.pumpling.ui.components.Badge(icon, tint, 38.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = T.bodyB)
                if (sub != null) Text(sub, style = T.small)
            }
        }
        Box(Modifier.padding(top = 10.dp)) { content() }
    }
}

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "pumpling"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
    const val SITE = "https://bigdevwhale.github.io/stillcraft/"
    const val CODE = "https://github.com/bigdevwhale/pumpling"
}

/** A row that opens a link. */
@Composable
private fun LinkRow(icon: Icons, tint: androidx.compose.ui.graphics.Color, title: String, sub: String?, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
        runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        app.pumpling.ui.components.Badge(icon, tint, 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Icon(Icons.NEXT, C.mute, 18.dp)
    }
}

/** Settings in four tabs: training, you, look, more. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val s = d.s
    fun set(f: (app.pumpling.data.Settings) -> app.pumpling.data.Settings) = c.write { settings.update(f) }
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    Page(tr("Настройки", "Settings"), { nav.pop() }) {
        // the tabs: big icons with labels
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Icons.GYM to tr("Тренировки", "Training"), Icons.ARM to tr("Профиль", "Profile"), Icons.PALETTE to tr("Вид", "Look"), Icons.DOTS to tr("Ещё", "More"))
                .forEachIndexed { i, (ic, l) -> val on = tab == i
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) C.acc else C.card).clickable { tab = i }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(ic, if (on) C.accInk else C.mute, 22.dp)
                        Text(l, style = T.tiny.copy(color = if (on) C.accInk else C.mute), modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                    } }
        }
        when (tab) {
            0 -> {
                Setting(Icons.CAL, C.ok, tr("Тренировок в неделю", "Workouts a week"), tr("Неделя с целью продлевает серию; пропуски внутри недели не страшны", "A week with the goal keeps the streak; gaps inside it are fine")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (1..7).forEach { n -> Chip("$n", s.weekGoal == n) { set { it.copy(weekGoal = n) } } } } }
                Setting(Icons.TARGET, C.gold, tr("Цель на год", "Year goal"), tr("Видна в «Летописи» за год – с прогнозом", "Shown in the year stats, with a forecast")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(0, 50, 100, 150, 200).forEach { n -> Chip(if (n == 0) tr("нет", "none") else "$n", s.yearGoal == n) { set { it.copy(yearGoal = n) } } } } }
                Setting(Icons.TIMER, C.acc, tr("Отдых между подходами", "Rest between sets"), tr("Таймер запускается, когда отмечаете подход", "Starts when you tick a set")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(45, 60, 90, 120, 180).forEach { n -> Chip(app.pumpling.domain.clock(n), s.rest == n) { set { it.copy(rest = n) } } } } }
                Setting(Icons.SCALE, C.mute, tr("Единицы", "Units")) {
                    Segments(listOf(tr("килограммы", "kilograms"), tr("фунты", "pounds")), if (s.lb) 1 else 0) { i -> set { it.copy(lb = i == 1) } } }
                Setting(Icons.GYM, C.ok, tr("Свой инвентарь", "Own equipment"), tr("Появляется в своих упражнениях, фильтре и статистике", "Shows up in own exercises, the filter and the stats")) {
                    Column {
                        var add by remember { mutableStateOf(false) }
                        s.gear.forEach { g -> val used = d.own.count { it.equip == app.pumpling.OWN_GEAR + g && !it.archived }
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(g, style = T.body, modifier = Modifier.weight(1f))
                                Text(if (used > 0) tr("в $used упражн.", "in $used") else tr("не используется", "unused"), style = T.tiny)
                                if (used == 0) Box(Modifier.size(34.dp).clip(CircleShape).clickable { c.write { settings.update { it.copy(gear = it.gear - g) } } }, contentAlignment = Alignment.Center) { Icon(Icons.TRASH, C.mute, 17.dp) }
                            } }
                        Ghost(tr("+ Добавить инвентарь", "+ Add equipment"), Modifier.fillMaxWidth().padding(top = 6.dp), small = true) { add = true }
                        if (add) app.pumpling.ui.workout.GearPopup(d, { add = false })
                    }
                }
                Setting(Icons.HEART, C.bad, tr("Больничный", "Sick leave"), tr("Пока болеете, серии недель и форма питомцев не сгорают", "While ill, week streaks and pets' shape are kept")) {
                    Chip(if (s.sick > 0) tr("болею – включён", "on – I'm ill") else tr("выключен", "off"), s.sick > 0, color = C.bad) {
                        set { it.copy(sick = if (it.sick > 0) 0 else today(), sickDays = if (it.sick > 0) it.sickDays + (it.sick..today()).toSet() else it.sickDays) } } }
            }
            1 -> {
                Setting(Icons.SCALE, C.ok, tr("Вес тела, ${unit()}", "Body weight, ${unit()}"), tr("Для нормативов силы, тоннажа подтягиваний и муравья", "For strength standards, pull-up tonnage and the ant")) {
                    var bw by remember { mutableStateOf(if (s.bodyweight > 0) w(s.bodyweight) else "") }
                    Field(bw, { v -> bw = v; v.replace(',', '.').toDoubleOrNull()?.let { x -> set { it.copy(bodyweight = toKg(x)) } } }, "70", Modifier.fillMaxWidth(), number = true) }
                Setting(Icons.TROPHY, C.gold, tr("Нормативы силы", "Strength standards"), tr("По каким таблицам считать уровень: новичок … элита", "Which tables to rate you by: beginner … elite")) {
                    Segments(listOf(tr("мужские", "men"), tr("женские", "women"), tr("не надо", "skip")), when (s.sex) { "m" -> 0; "f" -> 1; else -> 2 }) { i -> set { it.copy(sex = listOf("m", "f", "")[i]) } } }
                Setting(Icons.TARGET, C.ok, tr("Цели", "Goals"), tr("Помогают подбирать программы", "Help pick routines")) {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        app.pumpling.ui.journal.Goal.entries.forEach { g -> val on = g.name in s.goals
                            Chip(g.label, on, color = C.ok) { set { it.copy(goals = if (on) it.goals - g.name else it.goals + g.name) } } } } }
                Setting(Icons.CAMERA, C.acc, tr("Вес и замеры", "Weight and measurements"), tr("Талия, грудь, фото «до и после»", "Waist, chest, before-and-after photos")) {
                    Column {
                        Text(tr("Напоминать", "Remind me"), style = T.smallB)
                        Row(Modifier.padding(top = 6.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(7 to tr("неделя", "week"), 14 to tr("2 недели", "2 weeks"), 30 to tr("месяц", "month"), 0 to tr("нет", "no")).forEach { (n, l) -> Chip(l, s.checkIn == n) { set { it.copy(checkIn = n) } } } }
                        Ghost(tr("Открыть", "Open"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.BodyLog) }
                    } }
            }
            2 -> {
                Setting(Icons.PALETTE, C.acc, tr("Тема", "Theme")) {
                    Segments(listOf(tr("светлая", "light"), tr("тёмная", "dark"), tr("как в системе", "system")), when (s.theme) { "light" -> 0; "dark" -> 1; else -> 2 }) { i -> set { it.copy(theme = listOf("light", "dark", "system")[i]) } } }
                Setting(Icons.GLOBE, C.ok, tr("Язык", "Language")) {
                    val lang = currentLanguage()
                    Segments(listOf(tr("системный", "system"), "Русский", "English"), when (lang) { "ru" -> 1; "en" -> 2; else -> 0 }) { i -> setLanguage(listOf("", "ru", "en")[i]) } }
                val o = app.pumpling.data.Outside.of(s.outside)
                Setting(Icons.SUN, C.gold, tr("За окном зала", "Outside the gym window"),
                    (if (s.town.isNotEmpty()) s.town else tr("Город не выбран", "No town chosen")) + (o?.let { " · ${weatherWord(it.weather)}" + (if (it.temp.isNaN()) "" else ", ${Math.round(it.temp)}°") } ?: "")) {
                    Column {
                        var q by remember { mutableStateOf("") }
                        var found by remember { mutableStateOf<List<app.pumpling.data.Town>>(emptyList()) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Field(q, { q = it }, tr("Другой город", "Another town"), Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Btn(tr("Найти", "Find"), small = true, enabled = q.isNotBlank()) { c.write {
                                found = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { OutsideSource.towns(q.trim(), Lang.ru) }.getOrDefault(emptyList()) }
                                if (found.isEmpty()) say(tr("Ничего не нашлось", "Nothing found")) } }
                        }
                        found.forEach { t -> Text("${t.name}, ${t.region}", style = T.body, modifier = Modifier.fillMaxWidth().clickable {
                            c.write { settings.update { it.copy(town = t.name, lat = t.lat, lon = t.lon, guessed = true, outside = "") }; refreshOutside(true) }; found = emptyList(); q = "" }.padding(vertical = 8.dp)) }
                        Text(tr("Окно показывает время суток и погоду там. Отправляются только координаты города – в Open-Meteo или MET Norway.",
                            "The window shows the time of day and the weather there. Only the town's coordinates are sent – to Open-Meteo or MET Norway."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Setting(Icons.PLATE, C.gold, tr("Вещи в зале", "Things in the gym"), tr("Купить, поставить или убрать", "Buy, put out or put away")) {
                    Ghost(tr("Открыть зал", "Open the gym"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.Shop) } }
            }
            else -> {
                // support first: the app is free, this is how it stays so
                Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.gold, C.acc))).padding(18.dp)) {
                    Text(tr("Поддержать Pumpling", "Support Pumpling"), style = T.h2.copy(color = androidx.compose.ui.graphics.Color.White))
                    Text(tr("Приложение бесплатное, без рекламы и аккаунта. Если оно вам помогает, можно поддержать автора – так появятся новые питомцы и программы.",
                        "The app is free, with no ads and no account. If it helps you, you can support the author – that's how new pets and routines appear."),
                        style = T.small.copy(color = androidx.compose.ui.graphics.Color(0xEEFFFFFF)), modifier = Modifier.padding(top = 4.dp))
                    val ctx = LocalContext.current
                    Btn(tr("💛 Поддержать", "💛 Support"), Modifier.fillMaxWidth().padding(top = 14.dp), color = androidx.compose.ui.graphics.Color.White, ink = C.acc) {
                        runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(Community.SUPPORT)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                }
                Setting(Icons.GLOBE, C.ok, "StillCraft", tr("Pumpling – одно из небольших приложений StillCraft", "Pumpling is one of the small StillCraft apps")) {
                    Column {
                        LinkRow(Icons.HEART, C.acc, tr("Сообщество в Telegram", "Telegram community"), tr("новости, обсуждения, другие приложения", "news, talk and the other apps"), Community.CHAT)
                        LinkRow(Icons.CLOSE, C.bad, tr("Сообщить об ошибке", "Report a bug"), tr("через бота StillCraft", "through the StillCraft bot"), Community.BUG)
                        LinkRow(Icons.SPARK, C.gold, tr("Предложить идею", "Suggest an idea"), null, Community.IDEA)
                        LinkRow(Icons.STAR, C.ok, tr("Следить за обновлениями", "Follow updates"), tr("бот напишет, когда выйдет новая версия", "the bot writes when a new version is out"), Community.FOLLOW)
                        LinkRow(Icons.HOME, C.acc, tr("Все приложения StillCraft", "All StillCraft apps"), null, Community.SITE)
                        LinkRow(Icons.LIST, C.mute, tr("Исходный код", "Source code"), "GitHub", Community.CODE)
                    }
                }
                Setting(Icons.SPARK, C.acc, tr("Подсказки", "Hints"), tr("Короткие пояснения на каждом экране", "A short note on every screen")) {
                    Ghost(tr("Показать заново", "Show again"), Modifier.fillMaxWidth(), small = true) { c.write { settings.update { it.copy(seen = emptySet()) } }; c.say(tr("Подсказки покажутся снова", "Hints will show again")) } }
                Setting(Icons.STAR, C.gold, tr("Итоги года", "Year in review"), tr("Ваш год в сторис", "Your year as stories")) {
                    Ghost(tr("Листать", "Open"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.Wrapped(java.time.LocalDate.now().year)) } }
                Setting(Icons.HOME, C.ok, tr("О приложении", "About"), tr("Pumpling работает без аккаунта. Все тренировки и питомцы хранятся только на телефоне; в интернет уходят лишь координаты города для погоды.",
                    "Pumpling needs no account. Workouts and pets stay on the phone; only the town's coordinates go online, for the weather."))
                Text(tr("Контуры карты мышц – react-native-body-highlighter, MIT © 2022 ELABBASSI Hicham. Погода – Open-Meteo и MET Norway.", "Muscle map outlines: react-native-body-highlighter, MIT © 2022 ELABBASSI Hicham. Weather: Open-Meteo and MET Norway."), style = T.tiny, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
            }
        }
    }
}

fun weatherWord(w: app.pumpling.data.Weather) = when (w) {
    app.pumpling.data.Weather.CLEAR -> tr("ясно", "clear"); app.pumpling.data.Weather.CLOUDY -> tr("облачно", "cloudy"); app.pumpling.data.Weather.OVERCAST -> tr("пасмурно", "overcast")
    app.pumpling.data.Weather.RAIN -> tr("дождь", "rain"); app.pumpling.data.Weather.SNOW -> tr("снег", "snow"); app.pumpling.data.Weather.FOG -> tr("туман", "fog"); app.pumpling.data.Weather.STORM -> tr("гроза", "storm")
}

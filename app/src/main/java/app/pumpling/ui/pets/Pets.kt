package app.pumpling.ui.pets

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Icon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pumpling.container
import app.pumpling.data.Pet
import app.pumpling.domain.Cat
import app.pumpling.domain.Engine
import app.pumpling.domain.Form
import app.pumpling.domain.Pets
import app.pumpling.domain.Stage
import app.pumpling.domain.Tally
import app.pumpling.domain.catMap
import app.pumpling.domain.count
import app.pumpling.domain.fmt
import app.pumpling.domain.fmt1
import app.pumpling.domain.fmtDayYear
import app.pumpling.domain.nat
import app.pumpling.domain.sp
import app.pumpling.domain.stage
import app.pumpling.domain.tr
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.Screen
import app.pumpling.ui.art.Art
import app.pumpling.ui.art.Creature
import app.pumpling.ui.art.PetView
import app.pumpling.ui.art.Pose
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.Field
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.components.Meter
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.Section
import app.pumpling.ui.components.Segments
import app.pumpling.ui.components.card
import app.pumpling.ui.home.shapeWord
import app.pumpling.ui.share.CardKind
import app.pumpling.ui.share.ShareReq
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T

@Composable
fun PetsScreen(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var tab by remember { mutableStateOf(0) }
    var pick by remember { mutableStateOf<Int?>(null) }
    val tally = remember(d.workouts, d.sets, d.s) { Tally(d.finished, d.doneSets, d.s) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 30.dp)) {
        Row(Modifier.padding(top = 14.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Питомцы", "Pets"), style = T.title)
                Text(tr("${d.pets.size} из ${Pets.all.size} найдено", "${d.pets.size} of ${Pets.all.size} found"), style = T.small)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(if (tab == 0) CardKind.GYM else CardKind.BESTIARY) },
                contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) }
        }
        app.pumpling.ui.components.Hint("pets", d.s.seen, Icons.PAW, tr("Ваши питомцы", "Your pets"), tr("Ведущий ест всё, двое рядом – по половине, и способности всех троих складываются. Нажмите на питомца, чтобы поменять команду. Новые яйца находятся по тому, как вы тренируетесь – кого и как искать, написано в бестиарии.",
            "The leader eats it all, the two beside it half each, and their abilities add up. Tap a pet to change the team. New eggs turn up from how you train – the bestiary tells who and how."), Modifier.padding(top = 10.dp))
        Segments(listOf(tr("Мои", "Mine"), tr("Бестиарий", "Bestiary")), tab, Modifier.padding(top = 10.dp)) { tab = it }
        if (tab == 0) {
            TeamStage(d, nav) { pick = it }
            val home = d.pets.filter { it.slot !in 0..2 && it.hatched > 0 }.sortedByDescending { it.growth }
            val eggs = d.pets.filter { it.slot !in 0..2 && it.hatched == 0L }.sortedByDescending { it.growth }
            if (home.isNotEmpty()) {
                Section(icon = Icons.HOME, text = tr("Дома", "At home") + " · ${home.size}")
                home.chunked(2).forEach { row ->
                    Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { p -> PetTile(p, Modifier.weight(1f)) { nav.push(Screen.PetInfo(p.id)) } }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (eggs.isNotEmpty()) {
                Section(icon = Icons.EGG, text = tr("Яйца", "Eggs") + " · ${eggs.size}")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    eggs.forEach { p -> Column(Modifier.width(96.dp).card(16.dp).clickable { nav.push(Screen.PetInfo(p.id)) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        PetView(p, 72.dp)
                        Meter((p.growth / Pets.HATCH).toFloat(), Modifier.padding(top = 4.dp), h = 5.dp)
                        Text("${((p.growth / Pets.HATCH) * 100).toInt().coerceAtMost(99)}%", style = T.tiny)
                    } }
                }
                Text(tr("Дома яйца греются на четверти еды команды. В команде вылупятся быстрее.", "Eggs at home warm on a quarter of the team's food; on the team they hatch faster."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
            }
            if (home.isEmpty() && eggs.isEmpty()) Text(tr("Новые яйца найдутся по тому, как вы тренируетесь. Загляните в бестиарий – там написано, кого и как искать.",
                "New eggs turn up from the way you train. The bestiary tells who and how."), style = T.small, modifier = Modifier.padding(top = 14.dp))
        } else Bestiary(d, tally, nav)
    }
    pick?.let { sl -> SlotPicker(sl, d, { p -> c.write { setSlot(p, sl) }; pick = null }) { pick = null } }
}

/** The team standing together on a soft stage: the leader in the middle, the companions either side. */
@Composable
private fun TeamStage(d: Data, nav: Nav, onEmpty: (Int) -> Unit) {
    val leadColor = d.leader?.let { Color(it.sp().color) } ?: C.acc
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(leadColor.copy(alpha = .22f), C.card))).padding(14.dp)) {
        Text(tr("Команда", "The team"), style = T.h3)
        Text(tr("Ведущий ест всё, двое рядом – по половине", "The leader eats it all, the two beside it half each"), style = T.small)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            listOf(1, 0, 2).forEach { slot -> val p = d.pets.firstOrNull { it.slot == slot }
                val sz = if (slot == 0) 128.dp else 92.dp
                Column(Modifier.weight(if (slot == 0) 1.3f else 1f).clip(RoundedCornerShape(16.dp)).clickable { if (p != null) nav.push(Screen.PetInfo(p.id)) else onEmpty(slot) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (p != null) { PetView(p, sz, Pose.IDLE); Text(p.name, style = T.bodyB, maxLines = 1); Text(p.stage().label, style = T.tiny) }
                    else { Box(Modifier.size(sz * .8f).clip(CircleShape).background(C.card2), contentAlignment = Alignment.Center) { Text("+", style = T.title.copy(color = C.mute)) }
                        Text(tr("свободно", "free"), style = T.small) }
                }
            }
        }
        val bonuses = d.team.mapNotNull { it.sp().likes?.label?.lowercase() }.distinct()
        if (d.team.isNotEmpty()) Text(tr("Команда любит: ", "The team loves: ") + (bonuses.ifEmpty { listOf(tr("особые способности", "special abilities")) }).joinToString(", "),
            style = T.small.copy(color = C.ink), modifier = Modifier.padding(top = 8.dp))
    }
}

/** A pet resting at home, as a card in the grid. */
@Composable
private fun PetTile(p: Pet, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.card(18.dp).clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(p.sp().color).copy(alpha = .14f)), contentAlignment = Alignment.Center) { PetView(p, 104.dp) }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, style = T.bodyB, maxLines = 1)
            if (p.shiny) Text(" ★", style = T.smallB.copy(color = C.gold))
        }
        Text("${p.sp().name} · ${p.stage().label}", style = T.tiny, maxLines = 1)
        Meter(Stage.progress(p), Modifier.padding(top = 6.dp), h = 5.dp)
    }
}

/** All twenty, in tiles of the same size: found ones alive, the rest as shadows with how to find them. */
@Composable
private fun Bestiary(d: Data, tally: Tally, nav: Nav) {
    Spacer(Modifier.height(12.dp))
    Pets.all.chunked(2).forEach { row ->
        Row(Modifier.padding(bottom = 10.dp).height(278.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { sp -> val owned = d.pets.firstOrNull { it.species == sp.id }
                Column(Modifier.weight(1f).fillMaxHeight().card(18.dp).clickable { if (owned != null) nav.push(Screen.PetInfo(owned.id)) }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)).background(if (owned != null) Color(sp.color).copy(alpha = .14f) else C.card2),
                        contentAlignment = Alignment.Center) { if (owned != null) PetView(owned, 96.dp) else Silhouette(sp.id) }
                    Text(if (owned != null) sp.name else "???", style = T.bodyB, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                    Text(if (sp.legendary) tr("легенда", "legend") else if (owned != null) owned.stage().label else tr("не найден", "not found"),
                        style = T.tiny.copy(color = if (sp.legendary) C.gold else C.mute))
                    Text(sp.ability, style = T.tiny.copy(color = C.ink), textAlign = TextAlign.Center, minLines = 3, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.weight(1f))
                    if (owned == null) { val (f, txt) = Engine.near(sp.id, tally)
                        Text(sp.find, style = T.tiny, textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        Meter(f, Modifier.padding(top = 5.dp), h = 5.dp); Text(txt, style = T.tiny, maxLines = 1) }
                    else { Text(tr("найден ${fmtDayYear(owned.found)}", "found ${fmtDayYear(owned.found)}"), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
                        Meter(Stage.progress(owned), Modifier.padding(top = 5.dp), h = 5.dp, color = C.ok) }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SlotPicker(slot: Int, d: Data, onPick: (Pet) -> Unit, onClose: () -> Unit) {
    app.pumpling.ui.components.Sheet(onClose) {
        Text(if (slot == 0) tr("Кто поведёт команду?", "Who leads the team?") else tr("Кто встанет рядом?", "Who stands beside?"), style = T.h2)
        Spacer(Modifier.height(10.dp))
        val free = d.pets.filter { it.slot !in 0..2 }.sortedWith(compareBy({ it.hatched == 0L }, { -it.growth }))
        if (free.isEmpty()) Text(tr("Все питомцы уже в команде", "Every pet is already on the team"), style = T.body)
        free.chunked(3).forEach { row -> Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { p -> Column(Modifier.weight(1f).card(16.dp).clickable { onPick(p) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PetView(p, 72.dp); Text(p.name, style = T.smallB, maxLines = 1); Text(p.sp().name, style = T.tiny, maxLines = 1) } }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } }
    }
}

@Composable
private fun Silhouette(species: String) {
    val img = Art.pet(LocalContext.current, species, 2)
    if (img != null) androidx.compose.foundation.Image(img, null, Modifier.size(84.dp).alpha(.45f), colorFilter = ColorFilter.tint(C.mute))
    else Text("?", style = T.title.copy(color = C.mute))
}

@Composable
fun PetPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val p = d.pets.firstOrNull { it.id == id } ?: run { if (d.pets.isNotEmpty()) nav.pop(); return }
    val st = p.stage(); val sp = p.sp()
    var rename by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(p.name) }
    var pose by remember { mutableStateOf(Pose.IDLE) }
    Page(p.name, { nav.pop() }) {
        Box(Modifier.fillMaxWidth().card(22.dp).background(Color(sp.color).copy(alpha = .18f)).clickable { pose = Pose.entries[(pose.ordinal + 1) % Pose.entries.size] }.padding(12.dp), contentAlignment = Alignment.Center) {
            if (st == Stage.EGG) PetView(p, 200.dp) else Creature(p.species, st, pose, 220.dp, p.shape.toFloat(), p.shiny)
        }
        if (st != Stage.EGG) Text(tr("Нажмите, чтобы он показал, что умеет", "Tap to see what it can do"), style = T.tiny, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Card {
            Text("${sp.name} · ${st.label}" + if (p.shiny) tr(" · редкий окрас", " · rare colouring") else "", style = T.bodyB)
            Text(tr("Найден ${fmtDayYear(p.found)}", "Found ${fmtDayYear(p.found)}") + (if (p.hatched > 0) tr(", вылупился ${fmtDayYear(p.hatched)}", ", hatched ${fmtDayYear(p.hatched)}") else ""), style = T.small)
            Spacer(Modifier.height(10.dp))
            Text(tr("Способность", "Ability"), style = T.sec); Text(sp.ability, style = T.body)
            Spacer(Modifier.height(8.dp))
            Text(tr("Характер: ${p.nat().label}", "Nature: ${p.nat().label}"), style = T.sec); Text(p.nat().about, style = T.body)
            Form.of(p.form)?.let { Spacer(Modifier.height(8.dp)); Text(tr("Телосложение: ${it.label}", "Build: ${it.label}"), style = T.sec); Text(it.about, style = T.body) }
        }
        Section(icon = Icons.SPARK, text = tr("Рост", "Growth"))
        Card {
            if (st == Stage.EGG) { Text(tr("До вылупления: ${fmt1(p.growth)} из ${fmt(Pets.HATCH.toLong())}", "To hatching: ${fmt1(p.growth)} of ${fmt(Pets.HATCH.toLong())}"), style = T.body)
                Meter((p.growth / Pets.HATCH).toFloat(), Modifier.padding(top = 6.dp)) }
            else {
                Row { Text(tr("Рост", "Growth"), style = T.small, modifier = Modifier.weight(1f)); Text(fmt1(p.growth), style = T.smallB) }
                Meter(Stage.progress(p), Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(8.dp))
                Row { Text(tr("В животике", "In the belly"), style = T.small, modifier = Modifier.weight(1f)); Text(fmt1(p.belly), style = T.smallB) }
                Row(Modifier.padding(top = 6.dp)) { Text(tr("Форма", "Shape"), style = T.small, modifier = Modifier.weight(1f)); Text(shapeWord(p.shape), style = T.smallB) }
                Meter((p.shape / 100).toFloat(), Modifier.padding(top = 4.dp), color = C.ok)
                Text(count(p.workouts, "тренировка вместе", "тренировки вместе", "тренировок вместе", "workout together", "workouts together"), style = T.small, modifier = Modifier.padding(top = 8.dp))
            }
        }
        val cats = p.catMap().filterValues { it > 0 }
        if (cats.isNotEmpty()) {
            Section(icon = Icons.HEART, text = tr("Чем кормили", "What it ate"))
            Card { val total = cats.values.sum()
                cats.entries.sortedByDescending { it.value }.forEach { (k, v) -> val cat = Cat.of(k)
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.label, style = T.small, modifier = Modifier.width(110.dp)); Meter((v / total).toFloat(), Modifier.weight(1f), color = Color(cat.color), h = 7.dp)
                        Text("${(v / total * 100).toInt()}%", style = T.smallB, modifier = Modifier.width(44.dp), textAlign = TextAlign.End) } }
                if (st < Stage.ADULT) Text(tr("Когда вырастет, телосложение выберется по этой еде: сейчас это ${Form.from(cats).label.lowercase()}.", "When grown, its build follows this food: right now, ${Form.from(cats).label.lowercase()}."), style = T.small, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Section(icon = Icons.TARGET, text = tr("Место", "Place"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(tr("Ведущий", "Leader") to 0, tr("Рядом", "Beside") to 1, tr("Рядом", "Beside") to 2, tr("Дома", "At home") to -1).forEach { (l, sl) ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (p.slot == sl) C.acc else C.card2).clickable { c.write { setSlot(p, sl) } }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(l, style = T.smallB.copy(color = if (p.slot == sl) Color.White else C.ink)) } }
        }
        Spacer(Modifier.height(14.dp))
        if (rename) { Field(name, { name = it }, tr("Имя", "Name"), Modifier.fillMaxWidth())
            Btn(tr("Сохранить имя", "Save the name"), Modifier.fillMaxWidth().padding(top = 8.dp), enabled = name.isNotBlank()) { c.write { dao.update(p.copy(name = name.trim())) }; rename = false } }
        else Ghost(tr("Переименовать", "Rename"), Modifier.fillMaxWidth()) { rename = true }
        Spacer(Modifier.height(8.dp))
        if (st != Stage.EGG) Btn(tr("Поделиться открыткой", "Share a card"), Modifier.fillMaxWidth()) { nav.share = ShareReq(CardKind.PET, p.id) }
    }
}

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
import app.pumpling.ui.components.Chip
import app.pumpling.domain.fmtDayShort
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PetPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val p = d.pets.firstOrNull { it.id == id } ?: run { if (d.pets.isNotEmpty()) nav.pop(); return }
    val st = p.stage(); val sp = p.sp(); val col = Color(sp.color)
    var rename by remember { mutableStateOf(false) }
    var pose by remember { mutableStateOf(Pose.IDLE) }
    val deep = Color(col.red * .55f, col.green * .55f, col.blue * .55f)
    Page(p.name, { nav.pop() }, action = {
        if (st != Stage.EGG) Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.PET, p.id) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) } }) {
        // the hero: the pet big on its colour, the name and what it is on top of it
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(lerpP(col, Color.White, .35f), col, deep)))) {
            Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                if (st == Stage.EGG) PetView(p, 210.dp) else Creature(p.species, st, pose, 240.dp, p.shape.toFloat(), p.shiny)
            }
            Column(Modifier.fillMaxWidth().background(Color(0x33000000)).padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = T.title.copy(color = Color.White), modifier = Modifier.weight(1f, fill = false), maxLines = 1)
                    Box(Modifier.padding(start = 6.dp).size(34.dp).clip(CircleShape).clickable { rename = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, Color.White, 18.dp) }
                }
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf(sp.name, st.label) + listOfNotNull(Form.of(p.form)?.label, if (p.shiny) tr("редкий окрас", "rare") else null)).forEach { t ->
                        Text(t, style = T.smallB.copy(color = Color.White), modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF)).padding(horizontal = 10.dp, vertical = 4.dp)) }
                }
            }
        }
        if (st != Stage.EGG) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Pose.IDLE to tr("стоит", "stands"), Pose.WORK to tr("качается", "lifts"), Pose.CHEER to tr("радуется", "cheers"), Pose.SLEEP to tr("спит", "sleeps")).forEach { (ps, l) ->
                Chip(l, pose == ps, Modifier.weight(1f), color = col) { pose = ps } }
        }
        // three numbers
        Row(Modifier.fillMaxWidth().padding(top = 12.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (st == Stage.EGG) {
                StatTile(Icons.EGG, C.gold, "${((p.growth / Pets.HATCH) * 100).toInt().coerceAtMost(99)}%", tr("до вылупления", "to hatching"), Modifier.weight(1f))
                StatTile(Icons.CAL, C.ok, fmtDayShort(p.found), tr("найдено", "found"), Modifier.weight(1f))
            } else {
                StatTile(Icons.SPARK, col, if (st == Stage.MASTER) "★" else "${(Stage.progress(p) * 100).toInt()}%",
                    if (st == Stage.MASTER) tr("мастер", "master") else tr("до «${Stage.entries[st.ordinal + 1].label.lowercase()}»", "to ${Stage.entries[st.ordinal + 1].label.lowercase()}"), Modifier.weight(1f))
                StatTile(Icons.HEART, C.ok, shapeWord(p.shape), tr("форма", "shape"), Modifier.weight(1f))
                StatTile(Icons.GYM, C.acc, "${p.workouts}", tr("тренировок вместе", "workouts together"), Modifier.weight(1f))
            }
        }
        if (st != Stage.EGG) {
            Column(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).padding(14.dp)) {
                Row { Text(tr("Рост", "Growth"), style = T.small, modifier = Modifier.weight(1f)); Text(fmt1(p.growth), style = T.smallB) }
                Meter(Stage.progress(p), Modifier.padding(top = 5.dp), color = col)
                if (p.belly > 1) Text(tr("В животике ${fmt1(p.belly)} – станет ростом в дни отдыха", "${fmt1(p.belly)} in the belly – it turns into growth on rest days"), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
            }
        }
        // what makes it itself
        Section(icon = Icons.STAR, text = tr("Особенности", "What it's like"))
        Trait(Icons.BOLT, col, tr("Способность", "Ability"), sp.ability)
        Trait(Icons.HEART, C.acc, tr("Характер: ", "Nature: ") + p.nat().label.lowercase(), p.nat().about)
        Form.of(p.form)?.let { Trait(Icons.ARM, C.ok, tr("Телосложение: ", "Build: ") + it.label.lowercase(), it.about) }
        Trait(Icons.CAL, C.mute, tr("История", "Story"), tr("Найден ${fmtDayYear(p.found)}", "Found ${fmtDayYear(p.found)}") + (if (p.hatched > 0) tr(", вылупился ${fmtDayYear(p.hatched)}", ", hatched ${fmtDayYear(p.hatched)}") else ""))
        // what it ate, as one stripe
        val cats = p.catMap().filterValues { it > 0 }
        if (cats.isNotEmpty()) {
            Section(icon = Icons.HEART, text = tr("Чем кормили", "What it ate"))
            Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
                val total = cats.values.sum(); val sorted = cats.entries.sortedByDescending { it.value }
                app.pumpling.ui.components.Stripe(sorted.map { Color(Cat.of(it.key).color) to (it.value * 100).toInt() }, h = 12.dp)
                androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sorted.filter { it.value / total >= .01 }.forEach { (k, v) -> val cat = Cat.of(k)
                        Row(verticalAlignment = Alignment.CenterVertically) { app.pumpling.ui.components.Dot(Color(cat.color), 9.dp)
                            Text(" ${cat.label.lowercase()} ${(v / total * 100).toInt()}%", style = T.small.copy(color = C.ink)) } }
                }
                if (st < Stage.ADULT) Text(tr("Когда вырастет, телосложение выберется по этой еде – сейчас это «${Form.from(cats).label.lowercase()}».", "When grown, its build follows this food – right now «${Form.from(cats).label.lowercase()}»."),
                    style = T.tiny, modifier = Modifier.padding(top = 8.dp))
            }
        }
        // where it stands
        Section(icon = Icons.TARGET, text = tr("Место в команде", "Place on the team"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple(tr("Ведущий", "Leader"), 0, Icons.STAR), Triple(tr("Рядом", "Beside"), 1, Icons.PAW), Triple(tr("Рядом", "Beside"), 2, Icons.PAW), Triple(tr("Дома", "At home"), -1, Icons.HOME)).forEach { (l, sl, ic) ->
                val on = p.slot == sl
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) col else C.card).clickable { c.write { setSlot(p, sl) } }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(ic, if (on) Color.White else C.mute, 18.dp)
                    Text(l, style = T.tiny.copy(color = if (on) Color.White else C.ink), modifier = Modifier.padding(top = 3.dp))
                } }
        }
        Text(when (p.slot) { 0 -> tr("Ведущий ест всё, что вы натренируете", "The leader eats everything you train"); -1 -> tr("Дома питомец отдыхает и не ест", "At home the pet rests and doesn't eat")
            else -> tr("Рядом с ведущим – половина еды", "Beside the leader – half the food") }, style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        if (st != Stage.EGG) Btn(tr("Поделиться открыткой", "Share a card"), Modifier.fillMaxWidth(), color = col) { nav.share = ShareReq(CardKind.PET, p.id) }
    }
    if (rename) {
        var name by remember { mutableStateOf(p.name) }
        app.pumpling.ui.components.Popup({ rename = false }) {
            Text(tr("Как зовут питомца?", "What's its name?"), style = T.h2)
            Field(name, { name = it }, tr("Имя", "Name"), Modifier.fillMaxWidth().padding(top = 12.dp))
            Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = name.isNotBlank()) { c.write { dao.update(p.copy(name = name.trim())) }; rename = false }
        }
    }
}

@Composable
private fun StatTile(icon: Icons, tint: Color, value: String, label: String, modifier: Modifier) {
    Column(modifier.fillMaxHeight().card(18.dp).padding(12.dp)) {
        app.pumpling.ui.components.Badge(icon, tint, 32.dp)
        Text(value, style = T.h3, modifier = Modifier.padding(top = 8.dp), maxLines = 1)
        Text(label, style = T.tiny, maxLines = 2)
    }
}

@Composable
private fun Trait(icon: Icons, tint: Color, title: String, text: String) {
    Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().card(18.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        app.pumpling.ui.components.Badge(icon, tint, 38.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); Text(text, style = T.small.copy(color = C.ink)) }
    }
}

private fun lerpP(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)

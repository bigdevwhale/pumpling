package app.pumpling.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pumpling.container
import app.pumpling.domain.AchData
import app.pumpling.domain.Achievement
import app.pumpling.domain.Achievements
import app.pumpling.domain.Tally
import app.pumpling.domain.tr
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.components.Badge
import app.pumpling.ui.components.Hint
import app.pumpling.ui.components.Icons
import app.pumpling.ui.components.Meter
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.card
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T

private fun icon(a: Achievement) = when (a.id) {
    "workouts" -> Icons.GYM; "streak" -> Icons.FLAME; "hours", "long" -> Icons.CLOCK; "sets" -> Icons.CHECK; "tonnage" -> Icons.PLATE
    "records" -> Icons.TROPHY; "own", "pullups" -> Icons.ARM; "distance" -> Icons.ROUTE; "cardio" -> Icons.HEART; "holds" -> Icons.TIMER
    "flex" -> Icons.SPARK; "exercises" -> Icons.LIST; "families" -> Icons.TARGET; "early" -> Icons.SUN; "late", "masters" -> Icons.STAR
    "classes" -> Icons.CAL; "supersets" -> Icons.BOLT; "pets" -> Icons.PAW; "gym" -> Icons.HOME; "weigh" -> Icons.SCALE
    else -> Icons.TROPHY
}

/** Every long-term achievement: where the user stands, the tiers reached and what the next one takes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsPage(d: Data, nav: Nav) {
    val rows = remember(d.finished, d.doneSets, d.pets, d.s.items, d.bodies.size) {
        val data = AchData(Tally(d.finished, d.doneSets, d.s), d.pets, d.s, d.bodies.size)
        Achievements.all.map { a -> val v = a.value(data); Triple(a, v, a.level(v)) }
    }
    val got = rows.sumOf { it.third }
    // a tier reached since the last look (a thing bought, a pet grown) is paid now, so every screen shows the same count
    val c = LocalContext.current.container
    LaunchedEffect(got) { if (got > d.s.achieved.size) c.tick() }
    Page(tr("Достижения", "Achievements"), { nav.pop() }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(C.gold, C.acc))).padding(18.dp)) {
            Text(tr("СТУПЕНЕЙ ПРОЙДЕНО", "TIERS REACHED"), style = T.sec.copy(color = Color(0xCCFFFFFF)))
            Text("$got / ${Achievements.total}", style = T.title.copy(color = Color.White), modifier = Modifier.padding(top = 4.dp))
            Meter(got.toFloat() / Achievements.total, Modifier.padding(top = 10.dp), color = Color.White, h = 6.dp, track = Color(0x40FFFFFF))
        }
        Hint("achievements", d.s.seen, Icons.TROPHY, tr("Достижения", "Achievements"), tr("У каждого достижения несколько ступеней. За каждую ступень дают блины – чем она выше, тем больше. Ничего не сгорает: всё считается за всё время.",
            "Every achievement has several tiers. Each tier pays plates – the higher, the more. Nothing is ever lost: it all counts over all time."), Modifier.padding(top = 12.dp))
        rows.forEach { (a, v, lvl) ->
            val full = lvl >= a.tiers.size
            Column(Modifier.padding(top = 10.dp).fillMaxWidth().card(20.dp).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(icon(a), if (lvl > 0) C.gold else C.mute, 40.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(a.label, style = T.bodyB); Text(a.about, style = T.small) }
                    Text("$lvl/${a.tiers.size}", style = T.smallB.copy(color = if (lvl > 0) C.gold else C.mute))
                }
                if (full) Text(tr("Все ступени пройдены · сейчас ${a.show(v)}", "Every tier reached · now ${a.show(v)}"), style = T.small.copy(color = C.gold), modifier = Modifier.padding(top = 10.dp))
                else {
                    val next = a.tiers[lvl]
                    Meter((v / next).toFloat().coerceIn(0f, 1f), Modifier.padding(top = 10.dp), color = C.gold, h = 6.dp)
                    Text(tr("${a.show(v)} из ${a.show(next)} · за ступень +${Achievements.plates(lvl + 1)} блинов", "${a.show(v)} of ${a.show(next)} · +${Achievements.plates(lvl + 1)} plates for the tier"),
                        style = T.tiny, modifier = Modifier.padding(top = 5.dp))
                }
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    a.tiers.forEachIndexed { i, t -> val on = i < lvl
                        Text((if (on) "★ " else "") + a.show(t), style = T.tiny.copy(color = if (on) C.gold else C.mute),
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) C.goldSoft else C.card2).padding(horizontal = 9.dp, vertical = 4.dp)) }
                }
            }
        }
    }
}

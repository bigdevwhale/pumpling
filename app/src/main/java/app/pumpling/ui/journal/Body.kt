package app.pumpling.ui.journal

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import app.pumpling.ui.components.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.pumpling.container
import app.pumpling.data.Body
import app.pumpling.domain.Span
import app.pumpling.domain.Stats
import app.pumpling.domain.fmt1
import app.pumpling.domain.fmtDayYear
import app.pumpling.domain.today
import app.pumpling.domain.toKg
import app.pumpling.domain.tr
import app.pumpling.domain.unit
import app.pumpling.domain.w
import app.pumpling.ui.Data
import app.pumpling.ui.Nav
import app.pumpling.ui.components.Btn
import app.pumpling.ui.components.Card
import app.pumpling.ui.components.Field
import app.pumpling.ui.components.Ghost
import app.pumpling.ui.components.Line
import app.pumpling.ui.components.Page
import app.pumpling.ui.components.Row2
import app.pumpling.ui.components.Section
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T
import java.io.File

@Composable
fun BodyPage(d: Data, nav: Nav) {
    val ctx = LocalContext.current; val c = ctx.container
    val todayB = d.bodies.firstOrNull { it.day == today() }
    var weight by remember { mutableStateOf(todayB?.weight?.takeIf { it > 0 }?.let { w(it) } ?: "") }
    var waist by remember { mutableStateOf(todayB?.waist?.takeIf { it > 0 }?.let { fmt1(it) } ?: "") }
    var chest by remember { mutableStateOf(todayB?.chest?.takeIf { it > 0 }?.let { fmt1(it) } ?: "") }
    var hips by remember { mutableStateOf(todayB?.hips?.takeIf { it > 0 }?.let { fmt1(it) } ?: "") }
    var arm by remember { mutableStateOf(todayB?.arm?.takeIf { it > 0 }?.let { fmt1(it) } ?: "") }
    var fat by remember { mutableStateOf(todayB?.fat?.takeIf { it > 0 }?.let { fmt1(it) } ?: "") }
    var photo by remember { mutableStateOf(todayB?.photo ?: "") }
    var pending by remember { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pending?.let { photo = it }; pending = null }
    fun p(s: String) = s.replace(',', '.').toDoubleOrNull() ?: 0.0
    val st = remember(d.bodies) { Stats(d.workouts, d.sets, d.bodies, d.s, Span.of(Span.ALL)) }
    Page(tr("Вес и замеры", "Weight and measurements"), { nav.pop() }) {
        Card {
            Text(tr("Сегодня", "Today"), style = T.h3)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Text(tr("Вес, ${unit()}", "Weight, ${unit()}"), style = T.tiny); Field(weight, { weight = it }, "–", number = true) }
                Column(Modifier.weight(1f)) { Text(tr("Жир, %", "Fat, %"), style = T.tiny); Field(fat, { fat = it }, "–", number = true) }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Text(tr("Талия, см", "Waist, cm"), style = T.tiny); Field(waist, { waist = it }, "–", number = true) }
                Column(Modifier.weight(1f)) { Text(tr("Грудь, см", "Chest, cm"), style = T.tiny); Field(chest, { chest = it }, "–", number = true) }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Text(tr("Бёдра, см", "Hips, cm"), style = T.tiny); Field(hips, { hips = it }, "–", number = true) }
                Column(Modifier.weight(1f)) { Text(tr("Бицепс, см", "Arm, cm"), style = T.tiny); Field(arm, { arm = it }, "–", number = true) }
            }
            Spacer(Modifier.height(10.dp))
            Ghost(if (photo.isEmpty()) tr("Фото прогресса", "Progress photo") else tr("Переснять фото", "Retake photo"), Modifier.fillMaxWidth()) {
                val name = "body-${today()}-${System.currentTimeMillis() % 100000}.jpg"; val f = File(c.photos, name); pending = name
                camera.launch(FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)) }
            if (photo.isNotEmpty()) Thumb(File(c.photos, photo), Modifier.padding(top = 8.dp).fillMaxWidth().aspectRatio(.75f))
            Spacer(Modifier.height(10.dp))
            Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth()) {
                c.write { addBody(Body(day = today(), weight = toKg(p(weight)), waist = p(waist), chest = p(chest), hips = p(hips), arm = p(arm), fat = p(fat), photo = photo)); say(tr("Записано", "Saved")) } }
        }
        val trend = st.weightTrend()
        if (trend.size >= 2) {
            Section(icon = Icons.SCALE, text = tr("Вес", "Weight"))
            Card {
                val first = trend.first(); val last = trend.last()
                Text("${w(last.third)} ${unit()}", style = T.num)
                val diff = last.third - first.third
                Text(tr("Сглаженный тренд, ${if (diff >= 0) "+" else "−"}${w(kotlin.math.abs(diff))} ${unit()} с ${fmtDayYear(first.first)}", "Smoothed trend, ${if (diff >= 0) "+" else "−"}${w(kotlin.math.abs(diff))} ${unit()} since ${fmtDayYear(first.first)}"), style = T.small)
                Line(trend.map { it.first.toDouble() to it.second }, Modifier.padding(top = 10.dp), trend = trend.map { it.first.toDouble() to it.third })
            }
        }
        val waists = d.bodies.filter { it.waist > 0 }.sortedBy { it.day }
        if (waists.size >= 2) { Section(icon = Icons.SCALE, text = tr("Талия", "Waist")); Card { Line(waists.map { it.day.toDouble() to it.waist }) } }
        val photos = d.bodies.filter { it.photo.isNotEmpty() }.sortedBy { it.day }
        if (photos.size >= 2) {
            Section(icon = Icons.CAMERA, text = tr("До и после", "Before and after"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(photos.first(), photos.last()).forEach { b -> Column(Modifier.weight(1f)) {
                    Thumb(File(c.photos, b.photo), Modifier.fillMaxWidth().aspectRatio(.75f)); Text(fmtDayYear(b.day) + if (b.weight > 0) " · ${w(b.weight)} ${unit()}" else "", style = T.tiny) } }
            }
        }
        if (d.bodies.isNotEmpty()) { Section(icon = Icons.LOG, text = tr("Записи", "Entries"))
            Card(pad = 8.dp) { d.bodies.take(30).forEach { b -> Row2(fmtDayYear(b.day), listOfNotNull(b.waist.takeIf { it > 0 }?.let { tr("талия ${fmt1(it)}", "waist ${fmt1(it)}") },
                b.fat.takeIf { it > 0 }?.let { tr("жир ${fmt1(it)}%", "fat ${fmt1(it)}%") }).joinToString(" · ").ifEmpty { null }, if (b.weight > 0) "${w(b.weight)} ${unit()}" else null,
                Modifier.padding(horizontal = 8.dp)) } } }
    }
}

@Composable
fun Thumb(f: File, modifier: Modifier) {
    val bmp = remember(f.path, f.lastModified()) { runCatching { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }.getOrNull() }
    if (bmp != null) Image(bmp, null, modifier.clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
}


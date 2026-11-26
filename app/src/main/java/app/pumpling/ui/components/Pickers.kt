package app.pumpling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.pumpling.domain.fmtDayYear
import app.pumpling.domain.today
import app.pumpling.domain.tr
import app.pumpling.ui.theme.C
import app.pumpling.ui.theme.T

/** «18:30»: tapping it opens a clock to pick any time (#12). [mins] counts minutes from midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePick(mins: Int, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(C.card2).clickable { open = true }.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text("%d:%02d".format(mins / 60, mins % 60), style = T.bodyB)
    }
    if (open) {
        val ctx = LocalContext.current
        val st = rememberTimePickerState(mins / 60, mins % 60, android.text.format.DateFormat.is24HourFormat(ctx))
        Dialog({ open = false }) {
            Column(Modifier.clip(RoundedCornerShape(26.dp)).background(C.bg).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Во сколько началась?", "Started at"), style = T.h3, modifier = Modifier.padding(bottom = 14.dp))
                TimePicker(st, colors = androidx.compose.material3.TimePickerDefaults.colors(
                    clockDialColor = C.card2, clockDialSelectedContentColor = Color.White, clockDialUnselectedContentColor = C.ink, selectorColor = C.acc,
                    containerColor = C.bg, periodSelectorBorderColor = C.line,
                    periodSelectorSelectedContainerColor = C.accSoft, periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = C.ink, periodSelectorUnselectedContentColor = C.mute,
                    timeSelectorSelectedContainerColor = C.acc, timeSelectorUnselectedContainerColor = C.card2,
                    timeSelectorSelectedContentColor = Color.White, timeSelectorUnselectedContentColor = C.ink))
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Ghost(tr("Отмена", "Cancel")) { open = false }
                    Btn(tr("Готово", "Done")) { onPick(st.hour * 60 + st.minute); open = false }
                }
            }
        }
    }
}

/** A day up to today – or, with [future], from today on (#31): tapping it opens a calendar (#12). [day] is an epoch day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPick(day: Long, modifier: Modifier = Modifier, future: Boolean = false, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(C.card2).clickable { open = true }.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(fmtDayYear(day), style = T.bodyB)
    }
    if (open) {
        val dayMs = 86_400_000L
        val last = (today() + 1) * dayMs
        val st = rememberDatePickerState(initialSelectedDateMillis = day * dayMs, selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = if (future) utcTimeMillis >= today() * dayMs else utcTimeMillis < last
            override fun isSelectableYear(year: Int) = if (future) year >= java.time.LocalDate.ofEpochDay(today()).year else year <= java.time.LocalDate.ofEpochDay(today()).year
        })
        val colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = C.bg, titleContentColor = C.mute, headlineContentColor = C.ink,
            weekdayContentColor = C.mute, navigationContentColor = C.ink, dayContentColor = C.ink, disabledDayContentColor = C.mute.copy(alpha = .5f),
            selectedDayContainerColor = C.acc, selectedDayContentColor = Color.White, todayContentColor = C.acc, todayDateBorderColor = C.acc,
            yearContentColor = C.ink, currentYearContentColor = C.acc, selectedYearContainerColor = C.acc, selectedYearContentColor = Color.White, dividerColor = C.line)
        DatePickerDialog({ open = false }, colors = colors,
            confirmButton = { Btn(tr("Готово", "Done"), Modifier.padding(end = 12.dp, bottom = 8.dp)) { st.selectedDateMillis?.let { onPick(Math.floorDiv(it, dayMs)) }; open = false } },
            dismissButton = { Ghost(tr("Отмена", "Cancel"), Modifier.padding(end = 8.dp, bottom = 8.dp)) { open = false } }) {
            DatePicker(st, showModeToggle = false, colors = colors)
        }
    }
}

/** How long it took: quick choices and fields for any other time, in hours and minutes (#12, #31). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinutesPick(mins: Int, choices: List<Int> = listOf(15, 30, 45, 60, 90), onChange: (Int) -> Unit) {
    var h by remember { mutableStateOf(if (mins >= 60) "${mins / 60}" else "") }
    var m by remember { mutableStateOf("${mins % 60}") }
    fun send() { val t = (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0); if (t in 1..720) onChange(t) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        choices.forEach { c -> Chip(app.pumpling.domain.span(c * 60_000L).replace(" 00 мин", "").replace(" 00 min", ""), mins == c) {
            h = if (c >= 60) "${c / 60}" else ""; m = "${c % 60}"; onChange(c) } }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(h, { t -> h = t.filter { it.isDigit() }.take(2); send() }, "0", Modifier.width(52.dp), number = true, center = true)
            Text(" " + tr("ч", "h") + "  ", style = T.small)
            Field(m, { t -> m = t.filter { it.isDigit() }.take(3); send() }, "0", Modifier.width(60.dp), number = true, center = true)
            Text(" " + tr("мин", "min"), style = T.small)
        }
    }
    Spacer(Modifier.height(2.dp))
}

/** Where the workout is: home, the gym or outside; tapping the chosen one again clears it (#13). */
@Composable
fun PlacePick(place: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        app.pumpling.domain.Place.entries.forEach { p -> Chip(p.label, place == p.code) { onChange(if (place == p.code) "" else p.code) } }
    }
}

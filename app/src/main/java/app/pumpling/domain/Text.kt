package app.pumpling.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** The language the app speaks right now (set from the activity's locale). */
object Lang { @Volatile var ru = true }

/** Kilograms or pounds; everything is stored in kilograms. */
object Units { @Volatile var lb = false }

fun tr(ru: String, en: String) = if (Lang.ru) ru else en

fun plural(n: Long, one: String, few: String, many: String): String {
    val a = abs(n) % 100; val b = a % 10
    return if (a in 11..14) many else when (b) { 1L -> one; 2L, 3L, 4L -> few; else -> many }
}

/** «3 тренировки» / «3 workouts». */
fun count(n: Int, ru1: String, ru2: String, ru5: String, en1: String, enN: String) =
    if (Lang.ru) "${fmt(n.toLong())} ${plural(n.toLong(), ru1, ru2, ru5)}" else "${fmt(n.toLong())} ${if (n == 1) en1 else enN}"

/** 12 345 with a thin space in Russian, 12,345 in English. */
fun fmt(n: Long): String {
    val s = abs(n).toString().reversed().chunked(3).joinToString(if (Lang.ru) " " else ",").reversed()
    return if (n < 0) "−$s" else s
}
fun fmt(n: Int) = fmt(n.toLong())

/** One decimal at most, the decimal comma in Russian. */
fun fmt1(v: Double): String {
    val r = (v * 10).roundToLong()
    val s = if (r % 10 == 0L) fmt(r / 10) else fmt(r / 10) + (if (Lang.ru) "," else ".") + abs(r % 10)
    return if (v < 0 && r / 10 == 0L && r != 0L) "−$s" else s
}
fun fmt2(v: Double): String { val r = (v * 100).roundToLong(); return if (r % 100 == 0L) fmt(r / 100) else String.format(if (Lang.ru) Locale.forLanguageTag("ru") else Locale.UK, "%.2f", v).trimEnd('0') }

const val LB = 2.20462

/** A weight in the chosen unit, without the unit: 62,5 · 137.5 */
fun w(kg: Double): String { val v = if (Units.lb) kg * LB else kg; return if (Units.lb) fmt1((v * 2).roundToInt() / 2.0) else fmt2((v * 4).roundToInt() / 4.0) }
/** An estimate, rounded to whole kilograms (or pounds). */
fun wuA(kg: Double): String = if (Units.lb) "${fmt(Math.round(kg * LB))} ${unit()}" else "${fmt(Math.round(kg))} ${unit()}"
fun unit() = if (Units.lb) tr("фнт", "lb") else tr("кг", "kg")
fun wu(kg: Double) = "${w(kg)} ${unit()}"
/** Tonnage: big numbers in tonnes. */
fun tonnage(kg: Double): String = when {
    Units.lb -> if (kg * LB >= 100_000) "${fmt1(kg * LB / 1000)}k lb" else "${fmt((kg * LB).roundToLong())} lb"
    kg >= 10_000 -> "${fmt1(kg / 1000)} ${tr("т", "t")}"
    else -> "${fmt(kg.roundToLong())} ${tr("кг", "kg")}"
}
/** Input in the chosen unit back to kilograms. */
fun toKg(v: Double) = if (Units.lb) v / LB else v

fun km(m: Double): String = if (m >= 1000 || m == 0.0) "${fmt2(m / 1000)} ${tr("км", "km")}" else "${m.roundToInt()} ${tr("м", "m")}"

fun dur(sec: Int): String {
    val h = sec / 3600; val m = sec % 3600 / 60; val s = sec % 60
    return when {
        h > 0 -> tr("$h ч ${m} мин", "$h h ${m} min")
        m > 0 && s > 0 && m < 10 -> "$m:${s.toString().padStart(2, '0')}"
        m > 0 -> tr("$m мин", "$m min")
        else -> tr("$s с", "$s s")
    }
}
/** How long a workout took, in whole minutes: «47 мин», «1 ч 05 мин». */
fun span(ms: Long): String { val m = maxOf(1L, ms / 60000); return if (m < 60) tr("$m мин", "$m min") else tr("${m / 60} ч ${(m % 60).toString().padStart(2, '0')} мин", "${m / 60} h ${(m % 60).toString().padStart(2, '0')} min") }
fun clock(sec: Int): String { val m = sec / 60; val s = sec % 60; return if (m >= 60) "${m / 60}:${(m % 60).toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}" else "$m:${s.toString().padStart(2, '0')}" }
fun hours(sec: Long): String = if (sec >= 3600 * 10) tr("${fmt(sec / 3600)} ч", "${fmt(sec / 3600)} h") else tr("${fmt1(sec / 3600.0)} ч", "${fmt1(sec / 3600.0)} h")
/** min/km */
fun pace(secPerKm: Double): String = if (secPerKm.isNaN() || secPerKm <= 0) "–" else "${clock(secPerKm.roundToInt())} /${tr("км", "km")}"

private val ruLoc = Locale.forLanguageTag("ru")
val loc: Locale get() = if (Lang.ru) ruLoc else Locale.UK
val zone: ZoneId get() = ZoneId.systemDefault()
fun dayOf(ms: Long): Long = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()
fun hourOf(ms: Long): Int = Instant.ofEpochMilli(ms).atZone(zone).hour
fun today(): Long = LocalDate.now().toEpochDay()
fun fmtDay(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMMM", loc))
fun fmtDayShort(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern(if (Lang.ru) "d MMM" else "d MMM", loc)).trimEnd('.')
fun fmtDayYear(day: Long): String {
    val d = LocalDate.ofEpochDay(day)
    return if (d.year == LocalDate.now().year) fmtDay(day) else d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", loc))
}
fun weekday(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEEE", loc))
fun relDay(day: Long): String = when (today() - day) { 0L -> tr("сегодня", "today"); 1L -> tr("вчера", "yesterday"); else -> fmtDayYear(day) }
fun monthName(m: Int, nominative: Boolean = true): String =
    if (Lang.ru) (if (nominative) MONTHS_RU else MONTHS_RU_GEN)[m - 1] else MONTHS_EN[m - 1]
fun monthShort(m: Int): String = if (Lang.ru) MONTHS_RU_SHORT[m - 1] else MONTHS_EN[m - 1].take(3)
fun dayShort(i: Int): String = if (Lang.ru) listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")[i] else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")[i]

private val MONTHS_RU = listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
private val MONTHS_RU_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
private val MONTHS_RU_SHORT = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private val MONTHS_EN = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

/** Monday of the week [day] falls in. */
fun weekStart(day: Long): Long = day - LocalDate.ofEpochDay(day).dayOfWeek.value + 1

/** FNV-1a: stable hashes for colours and the order of things. */
fun hash(s: String): Int { var x = -0x7ee3623b; for (c in s) { x = x xor c.code; x *= 16777619 }; return x }

/** A small seeded random (mulberry32), so made-up things look the same every time. */
class Rand(seed: Int) {
    private var a = seed
    fun next(): Float { a += 0x6D2B79F5; var t = (a xor (a ushr 15)) * (1 or a); t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t; return ((t xor (t ushr 14)).toLong() and 0xffffffffL).toFloat() / 4294967296f }
    fun range(a: Float, b: Float) = a + (b - a) * next()
    fun int(n: Int) = (next() * n).toInt().coerceAtMost(n - 1)
}

fun String.cap() = replaceFirstChar { it.uppercase() }

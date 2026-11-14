package app.pumpling.domain

import app.pumpling.data.Pet
import app.pumpling.data.Settings

/** Everything the achievements are counted from. */
class AchData(val t: Tally, val pets: List<Pet>, val s: Settings, val weighIns: Int)

/**
 * A long-term achievement with several tiers: [value] is where the user stands, [tiers] the marks to reach, rising.
 * [show] writes a mark or the value the way the user reads it («50 т», «10 км»).
 */
class Achievement(val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String,
                  val tiers: List<Double>, val show: (Double) -> String = { fmt(it.toLong()) }, val value: (AchData) -> Double) {
    val label get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    /** How many tiers [v] has reached. */
    fun level(v: Double) = tiers.count { v >= it - 1e-9 }
}

/** Supersets, group by group: the blocks of a workout that share a number greater than 0 and stand next to each other. */
object Supersets {
    /** Is block [i] done together with the next one? [g] are the blocks' group numbers in order. */
    fun linked(g: List<Int>, i: Int) = i >= 0 && i < g.size - 1 && g[i] > 0 && g[i] == g[i + 1]
    fun inSet(g: List<Int>, i: Int) = linked(g, i) || linked(g, i - 1)
    /** Ties block [i] to the next one or cuts them apart; returns the group numbers anew (0 for a block on its own). */
    fun link(g: List<Int>, i: Int, on: Boolean): List<Int> {
        if (i < 0 || i >= g.size - 1) return g
        val links = BooleanArray(g.size - 1) { linked(g, it) }; links[i] = on
        val out = IntArray(g.size); var num = 0; var k = 0
        while (k < g.size) {
            var e = k; while (e < g.size - 1 && links[e]) e++
            if (e > k) { num++; for (j in k..e) out[j] = num }
            k = e + 1
        }
        return out.toList()
    }
}

object Achievements {
    private fun h(v: Double) = tr("${fmt(v.toLong())} ч", "${fmt(v.toLong())} h")
    private fun kmS(v: Double) = "${fmt1(v)} ${tr("км", "km")}"
    private fun min(v: Double) = tr("${fmt(v.toLong())} мин", "${fmt(v.toLong())} min")
    private fun x(v: Double) = "×${fmt1(v)}"

    val all = listOf(
        Achievement("workouts", "Тренировки", "Workouts", "Сколько тренировок в журнале", "Workouts in the log",
            listOf(1.0, 10.0, 25.0, 50.0, 100.0, 200.0, 365.0, 500.0)) { it.t.workouts.size.toDouble() },
        Achievement("streak", "Недели по плану", "Weeks to plan", "Самая длинная серия недель с выполненной целью", "The longest run of weeks with the goal met",
            listOf(2.0, 4.0, 8.0, 12.0, 26.0, 52.0)) { it.t.weeks.best.toDouble() },
        Achievement("hours", "Время в зале", "Time training", "Часы всех тренировок вместе", "Hours of all the workouts together",
            listOf(10.0, 50.0, 100.0, 250.0, 500.0), ::h) { d -> d.t.workouts.sumOf { it.end - it.start } / 3600_000.0 },
        Achievement("sets", "Подходы", "Sets", "Рабочие подходы за всё время", "Working sets of all time",
            listOf(100.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)) { it.t.work.size.toDouble() },
        Achievement("tonnage", "Поднято", "Lifted", "Вес всех подходов вместе", "The weight of every set together",
            listOf(10_000.0, 50_000.0, 100_000.0, 250_000.0, 500_000.0, 1_000_000.0), { tonnage(it) }) { it.t.tonnage },
        Achievement("records", "Рекорды", "Records", "Сколько раз вы превзошли себя", "Times you beat yourself",
            listOf(1.0, 10.0, 25.0, 50.0, 100.0, 250.0)) { it.t.prs.toDouble() },
        Achievement("own", "Свой вес", "Your own weight", "Самый тяжёлый подход относительно веса тела", "The heaviest set against your body weight",
            listOf(.5, 1.0, 1.5, 2.0), ::x) { d -> (d.t.work.filter { Catalog.of(it.exId).kind == Kind.WEIGHT && it.reps >= 1 }.maxOfOrNull { it.weight } ?: 0.0) / d.t.bw },
        Achievement("pullups", "Подтягивания", "Pull-ups", "Повторы подтягиваний за всё время", "Pull-up reps of all time",
            listOf(50.0, 200.0, 500.0, 1000.0, 2500.0)) { it.t.pullupReps.toDouble() },
        Achievement("distance", "Дистанция", "Distance", "Километры бега, ходьбы, велосипеда и другого кардио", "Kilometres run, walked, cycled and so on",
            listOf(10.0, 42.2, 100.0, 250.0, 500.0, 1000.0), ::kmS) { d -> d.t.work.sumOf { it.meters } / 1000 },
        Achievement("cardio", "Кардио", "Cardio", "Часы кардио", "Hours of cardio",
            listOf(1.0, 5.0, 10.0, 25.0, 50.0, 100.0), ::h) { it.t.cardioSecs / 3600.0 },
        Achievement("holds", "Удержания", "Holds", "Минуты планок и других удержаний", "Minutes of planks and other holds",
            listOf(10.0, 30.0, 60.0, 180.0), ::min) { it.t.holdSecs / 60.0 },
        Achievement("flex", "Гибкость", "Flexibility", "Часы растяжки, йоги и мобилити", "Hours of stretching, yoga and mobility",
            listOf(1.0, 3.0, 10.0, 25.0), ::h) { (it.t.catSecs[Cat.FLEX] ?: 0) / 3600.0 },
        Achievement("exercises", "Упражнения", "Exercises", "Сколько разных упражнений вы пробовали", "Different exercises you've tried",
            listOf(10.0, 25.0, 50.0, 100.0, 150.0)) { d -> d.t.work.map { it.exId }.distinct().size.toDouble() },
        Achievement("families", "Разносторонность", "All-round", "Семейства упражнений: ноги, жимы, тяги, кор, кардио и другие", "Exercise families: legs, push, pull, core, cardio and the rest",
            listOf(3.0, 6.0, 9.0)) { it.t.cats.size.toDouble() },
        Achievement("long", "Долгие тренировки", "Long workouts", "Тренировки от часа", "Workouts of an hour or more",
            listOf(1.0, 10.0, 50.0, 100.0)) { d -> d.t.workouts.count { it.end - it.start >= 3600_000 }.toDouble() },
        Achievement("early", "Жаворонок", "Early bird", "Тренировки до 7:30 утра", "Workouts before 7:30 am",
            listOf(1.0, 10.0, 30.0)) { it.t.early.toDouble() },
        Achievement("late", "Сова", "Night owl", "Тренировки после 21:00", "Workouts after 9 pm",
            listOf(1.0, 10.0, 30.0)) { it.t.late.toDouble() },
        Achievement("classes", "Групповые занятия", "Group classes", "Пилатес, йога, круговые и другие занятия с таймером", "Pilates, yoga, circuits and other classes with a timer",
            listOf(1.0, 10.0, 30.0)) { d -> d.t.workouts.count { it.classOf.isNotEmpty() }.toDouble() },
        Achievement("supersets", "Суперсеты", "Supersets", "Тренировки, где упражнения шли суперсетом", "Workouts with exercises done as a superset",
            listOf(1.0, 10.0, 30.0)) { d -> d.t.work.filter { it.superset > 0 }.map { it.workoutId }.distinct().size.toDouble() },
        Achievement("pets", "Питомцы", "Pets", "Сколько питомцев вылупилось", "Pets hatched",
            listOf(1.0, 3.0, 6.0, 10.0, 15.0, 20.0)) { d -> d.pets.count { it.hatched > 0 }.toDouble() },
        Achievement("masters", "Мастера", "Masters", "Питомцы, выросшие до мастера", "Pets grown into masters",
            listOf(1.0, 3.0, 5.0, 10.0)) { d -> d.pets.count { it.stage() == Stage.MASTER }.toDouble() },
        Achievement("gym", "Свой зал", "Your gym", "Вещи, купленные для зала", "Things bought for the gym",
            listOf(1.0, 5.0, 10.0, 16.0)) { it.s.items.size.toDouble() },
        Achievement("weigh", "Замеры", "Check-ins", "Сколько раз вы взвесились или измерились", "Times you weighed in or measured",
            listOf(1.0, 5.0, 20.0, 50.0)) { it.weighIns.toDouble() },
    )
    val byId = all.associateBy { it.id }
    val total = all.sumOf { it.tiers.size }

    /** Plates for reaching tier [level] (1 is the first). */
    fun plates(level: Int) = 10 + 10 * level
    fun key(a: Achievement, level: Int) = "${a.id}:$level"

    /** Tiers reached and not yet in [paid], lowest first within an achievement. */
    fun fresh(d: AchData, paid: Set<String>): List<Pair<Achievement, Int>> =
        all.flatMap { a -> (1..a.level(a.value(d))).filter { key(a, it) !in paid }.map { a to it } }
}

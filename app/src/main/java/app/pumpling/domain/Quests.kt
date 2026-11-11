package app.pumpling.domain

import app.pumpling.data.Body
import app.pumpling.data.WSet
import app.pumpling.data.Workout

/** A small task for the week, paid in plates. [progress] gives done/needed over this week's data. */
class Quest(val id: String, val ru: String, val en: String, val plates: Int, val need: Double, val progress: (Week) -> Double) {
    val label get() = tr(ru, en)
}

/** This week's workouts and sets, and everything done before it. */
class Week(val workouts: List<Workout>, val sets: List<WSet>, val beforeSets: List<WSet>, val bodies: List<Body>, val start: Long) {
    val work = sets.filter { it.done && !it.warmup }
    fun catSets(c: Cat) = work.count { Catalog.of(it.exId).cat == c }.toDouble()
}

object Quests {
    val all = listOf(
        Quest("new", "Попробовать новое упражнение", "Try a new exercise", 20, 1.0) { w ->
            val old = w.beforeSets.map { it.exId }.toSet(); w.work.map { it.exId }.distinct().count { it !in old }.toDouble() },
        Quest("pr", "Поставить рекорд", "Set a record", 25, 1.0) { w -> w.work.count { it.pr.isNotEmpty() }.toDouble() },
        Quest("cardio20", "20 минут кардио за раз", "20 minutes of cardio in one go", 20, 1.0) { w ->
            if (w.work.any { Catalog.of(it.exId).kind == Kind.CARDIO && it.secs >= 1200 }) 1.0 else 0.0 },
        Quest("legs", "10 подходов на ноги", "10 sets for legs", 20, 10.0) { w -> w.catSets(Cat.LEGS) },
        Quest("pull", "10 подходов тяг", "10 pulling sets", 20, 10.0) { w -> w.catSets(Cat.PULL) },
        Quest("push", "10 подходов жимов", "10 pressing sets", 20, 10.0) { w -> w.catSets(Cat.PUSH) },
        Quest("core", "6 подходов на кор", "6 core sets", 20, 6.0) { w -> w.catSets(Cat.CORE) },
        Quest("flex", "15 минут растяжки", "15 minutes of stretching", 20, 15.0) { w ->
            w.work.filter { Catalog.of(it.exId).cat == Cat.FLEX }.sumOf { it.secs } / 60.0 },
        Quest("early", "Тренировка до 9 утра", "A workout before 9 am", 20, 1.0) { w -> w.workouts.count { hourOf(it.start) < 9 }.toDouble() },
        Quest("rpe", "Отметить RPE в 10 подходах", "Note RPE on 10 sets", 15, 10.0) { w -> w.work.count { it.rpe > 0 }.toDouble() },
        Quest("long", "Тренировка дольше часа", "A workout over an hour", 20, 1.0) { w -> w.workouts.count { it.end - it.start >= 3600_000 }.toDouble() },
        Quest("volume", "Поднять 5 тонн за неделю", "Lift 5 tonnes this week", 25, 5000.0) { w ->
            w.work.filter { Catalog.of(it.exId).kind == Kind.WEIGHT }.sumOf { it.weight * it.reps } },
        Quest("weigh", "Взвеситься", "Weigh in", 10, 1.0) { w -> w.bodies.count { it.day >= w.start && it.weight > 0 }.toDouble() },
        Quest("routine", "Тренировка по программе", "A workout from a routine", 15, 1.0) { w -> w.workouts.count { it.routineId > 0 }.toDouble() },
        Quest("variety", "Три семейства упражнений за одну тренировку", "Three exercise families in one workout", 20, 1.0) { w ->
            if (w.work.groupBy { it.workoutId }.values.any { l -> l.map { Catalog.of(it.exId).cat }.toSet().size >= 3 }) 1.0 else 0.0 },
        Quest("holds", "3 минуты планки за неделю", "3 minutes of planks this week", 20, 180.0) { w ->
            w.work.filter { Catalog.of(it.exId).kind == Kind.HOLD }.sumOf { it.secs }.toDouble() },
    )
    val byId = all.associateBy { it.id }

    /** Three quests for the week starting [monday], the same for everyone on that week. */
    fun deal(monday: Long): List<String> {
        val r = Rand(hash("week$monday"))
        return all.map { it.id }.sortedBy { r.next() }.take(3)
    }
}

/** Something to buy for the gym with plates. [x],[y] place it in the room (0..1 of the scene); [w] its width. */
class Item(val id: String, val ru: String, val en: String, val price: Int, val cell: Int, val x: Float, val y: Float, val w: Float, val wall: Boolean = false) {
    val name get() = tr(ru, en)
}

object Shop {
    // cell = place on art/sheets/items.png (4×4). Floor things stand on y (their bottom), wall things hang with y their centre.
    val all = listOf(
        Item("mat", "Коврик и ролл", "Mat and roller", 40, 11, 0.62f, 0.75f, 0.1f),
        Item("plant", "Монстера", "Monstera", 60, 4, 0.05f, 0.83f, 0.1f),
        Item("poster", "Постер с горой", "Mountain poster", 60, 5, 0.24f, 0.4f, 0.13f, wall = true),
        Item("boombox", "Магнитофон", "Boombox", 80, 6, 0.53f, 0.7f, 0.09f),
        Item("lights", "Гирлянда", "Fairy lights", 80, 7, 0.5f, 0.12f, 0.42f, wall = true),
        Item("fan", "Вентилятор", "Fan", 90, 13, 0.29f, 0.74f, 0.08f),
        Item("cooler", "Кулер с водой", "Water cooler", 110, 12, 0.75f, 0.74f, 0.075f),
        Item("bench", "Скамья с гантелями", "Bench and dumbbells", 140, 15, 0.7f, 0.81f, 0.16f),
        Item("kettle", "Стойка с гирями", "Kettlebell rack", 150, 10, 0.4f, 0.78f, 0.12f),
        Item("mirror", "Зеркало", "Mirror", 160, 9, 0.09f, 0.47f, 0.09f, wall = true),
        Item("neon", "Неоновое сердце", "Neon heart", 180, 14, 0.64f, 0.42f, 0.09f, wall = true),
        Item("trophies", "Полка с кубками", "Trophy shelf", 220, 8, 0.8f, 0.33f, 0.13f, wall = true),
        Item("bag", "Боксёрский мешок", "Punching bag", 240, 3, 0.955f, 0.42f, 0.07f, wall = true),
        Item("rower", "Гребной тренажёр", "Rowing machine", 300, 2, 0.31f, 0.83f, 0.2f),
        Item("treadmill", "Беговая дорожка", "Treadmill", 360, 1, 0.88f, 0.81f, 0.2f),
        Item("rack", "Силовая рама", "Squat rack", 420, 0, 0.17f, 0.79f, 0.19f),
    )
    val byId = all.associateBy { it.id }
}

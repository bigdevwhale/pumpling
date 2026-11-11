package app.pumpling.ui.journal

import app.pumpling.domain.tr

/** Where a ready routine is done. */
enum class Place(val ru: String, val en: String) {
    GYM("Зал", "Gym"), HOME("Дом", "Home"), OUT("Улица", "Outdoors");
    val label get() = tr(ru, en)
}

/** A ready routine: name, one line about it, where, and its plans («exId|sets|reps|weight|secs» joined by «;»). */
class Template(val ru: String, val en: String, val aboutRu: String, val aboutEn: String, val place: Place, val items: String) {
    val level: Level get() = Level.entries[(LEVELS[ru] ?: 1) - 1]
}

/** How ready one needs to be. */
enum class Level(val ru: String, val en: String, val aboutRu: String, val aboutEn: String) {
    NEW("Новичок", "Beginner", "только начинаю или давно не занимался(-ась)", "just starting or back after a long break"),
    MID("Средний", "Intermediate", "занимаюсь от полугода", "training for half a year or more"),
    PRO("Опытный", "Advanced", "давно и регулярно", "for years, regularly");
    val label get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
}

private val LEVELS = mapOf("Фулбоди А" to 1, "Фулбоди Б" to 1, "5×5" to 2, "Тренажёры для новичка" to 1, "Жимы" to 2, "Тяги" to 2, "Ноги" to 2, "Верх тела" to 2, "Низ тела" to 2, "Грудь и трицепс" to 3, "Спина и бицепс" to 3, "Плечи" to 3, "Ягодицы" to 2, "Руки" to 3, "Пресс и кор" to 2, "Кардио в зале" to 1, "Дома без железа" to 1, "Всё тело за 20 минут" to 2, "Дома с гантелями" to 1, "Пресс дома" to 1, "Ноги и ягодицы дома" to 1, "Грудь и руки дома" to 2, "Табата" to 3, "Утренняя зарядка" to 1, "Растяжка на вечер" to 1, "Йога и баланс" to 1, "Турники во дворе" to 2, "Воркаут-круг" to 3, "Пробежка 5 км" to 2, "Интервалы на стадионе" to 3, "Прыжки и лестница" to 2, "Велопрогулка" to 1, "Ходьба" to 1)

/** What the user has to train with, for suggesting routines. */
enum class Kit(val ru: String, val en: String) {
    DB("Гантели", "Dumbbells"), KB("Гиря", "Kettlebell"), BAR("Турник", "Pull-up bar"), BARBELL("Штанга", "Barbell"), BAND("Резинки", "Bands"), MAT("Коврик", "Mat");
    val label get() = tr(ru, en)
}

/** What the user trains for. */
enum class Goal(val ru: String, val en: String) {
    STRENGTH("Сила", "Strength"), MUSCLE("Мышцы и рельеф", "Muscle and tone"), LOSE("Похудеть", "Lose weight"),
    ENDURE("Выносливость", "Endurance"), FLEX("Гибкость и здоровье", "Mobility and health");
    val label get() = tr(ru, en)
}

/** What a routine is good for, from its exercises: heavy low reps – strength, 8–15 reps – muscle, cardio and intervals – endurance and weight, stretching – mobility. */
val Template.goals: Set<Goal> get() {
    val out = HashSet<Goal>()
    for (p in app.pumpling.Plan.parse(items)) { val ex = app.pumpling.domain.Catalog.of(p.exId)
        when (ex.cat) {
            app.pumpling.domain.Cat.CARDIO -> { out += Goal.ENDURE; out += Goal.LOSE }
            app.pumpling.domain.Cat.HIIT, app.pumpling.domain.Cat.JUMP -> { out += Goal.LOSE; out += Goal.ENDURE }
            app.pumpling.domain.Cat.FLEX, app.pumpling.domain.Cat.BAL -> out += Goal.FLEX
            else -> if (ex.kind == app.pumpling.domain.Kind.WEIGHT && p.reps in 1..6) out += Goal.STRENGTH else if (!ex.kind.timed) out += Goal.MUSCLE
        } }
    if (Goal.MUSCLE in out && items.count { it == ';' } >= 4) out += Goal.LOSE
    return out
}

/** Routines that fit the place, the kit and the level, the ones serving the goals first, then the closest level. */
fun suggest(places: Set<Place>, kit: Set<Kit>, level: Level, goals: Set<Goal> = emptySet()): List<Template> {
    fun fits(t: Template): Boolean {
        if (t.place !in places) return false
        if (t.place == Place.GYM) return true
        return app.pumpling.Plan.parse(t.items).all { p -> val e = app.pumpling.domain.Catalog.of(p.exId).equip
            when (e) {
                app.pumpling.domain.Equip.DB -> Kit.DB in kit
                app.pumpling.domain.Equip.KB -> Kit.KB in kit
                app.pumpling.domain.Equip.BAR, app.pumpling.domain.Equip.EZ -> Kit.BARBELL in kit
                app.pumpling.domain.Equip.MACH, app.pumpling.domain.Equip.CABLE -> false
                else -> p.exId !in setOf("pullup", "chinup", "dead_hang", "leg_raise", "dip") || Kit.BAR in kit || t.place == Place.OUT
            } }
    }
    val ranked = TEMPLATES.filter { fits(it) && it.level <= level }.sortedWith(compareBy({ -(it.goals intersect goals).size }, { -it.level.ordinal }))
    // places take turns, so a gym-and-home person sees both
    val byPlace = ranked.groupBy { it.place }.values.map { it.toMutableList() }
    val out = ArrayList<Template>()
    while (byPlace.any { it.isNotEmpty() }) for (l in byPlace) if (l.isNotEmpty()) out += l.removeAt(0)
    return out
}


val TEMPLATES = listOf(
    // ---- the gym: whole body ----
    Template("Фулбоди А", "Full body A", "Всё тело за раз, чередуйте с «Б»", "The whole body; alternate with «B»", Place.GYM, "squat|3|8|0|0;bench|3|8|0|0;row|3|8|0|0;plank|3|0|0|45"),
    Template("Фулбоди Б", "Full body B", "Пара к «А»: тяга, жим стоя, подтягивания", "The pair to «A»: deadlift, press, pull-ups", Place.GYM, "deadlift|3|5|0|0;ohp|3|8|0|0;pullup|3|6|0|0;lunge|3|10|0|0"),
    Template("5×5", "5×5", "Классика для силы: три базовых по 5×5", "The strength classic: three lifts, 5×5", Place.GYM, "squat|5|5|0|0;bench|5|5|0|0;row|5|5|0|0"),
    Template("Тренажёры для новичка", "Machines for a beginner", "Безопасно и понятно: всё тело на тренажёрах", "Safe and simple: the whole body on machines", Place.GYM,
        "leg_press|3|12|0|0;chest_press|3|12|0|0;lat_pulldown|3|12|0|0;cable_row|3|12|0|0;leg_curl|3|12|0|0;plank|3|0|0|30"),
    // ---- the gym: splits ----
    Template("Жимы", "Push", "Грудь, плечи, трицепс", "Chest, shoulders, triceps", Place.GYM, "bench|4|8|0|0;incline_db|3|10|0|0;ohp|3|8|0|0;lateral|3|15|0|0;pushdown|3|12|0|0"),
    Template("Тяги", "Pull", "Спина и бицепс", "Back and biceps", Place.GYM, "deadlift|3|5|0|0;pullup|4|6|0|0;cable_row|3|10|0|0;face_pull|3|15|0|0;db_curl|3|12|0|0"),
    Template("Ноги", "Legs", "Присед, тяги, икры", "Squats, hinges, calves", Place.GYM, "squat|4|6|0|0;rdl|3|8|0|0;leg_press|3|12|0|0;leg_curl|3|12|0|0;calf_raise|4|15|0|0"),
    Template("Верх тела", "Upper body", "Грудь, спина, плечи и руки за раз", "Chest, back, shoulders and arms in one go", Place.GYM,
        "bench|4|8|0|0;row|4|8|0|0;ohp|3|10|0|0;lat_pulldown|3|10|0|0;db_curl|3|12|0|0;pushdown|3|12|0|0"),
    Template("Низ тела", "Lower body", "Ноги, ягодицы и пресс", "Legs, glutes and abs", Place.GYM,
        "squat|4|8|0|0;rdl|3|10|0|0;bulgarian|3|10|0|0;leg_curl|3|12|0|0;calf_raise|3|15|0|0;leg_raise|3|12|0|0"),
    // ---- the gym: muscle groups ----
    Template("Грудь и трицепс", "Chest and triceps", "Жимы под разными углами и разгибания", "Presses at different angles and extensions", Place.GYM,
        "bench|4|8|0|0;incline_db|3|10|0|0;cable_fly|3|12|0|0;dip|3|10|0|0;skullcrusher|3|12|0|0;pushdown|3|15|0|0"),
    Template("Спина и бицепс", "Back and biceps", "Ширина, толщина и руки", "Width, thickness and arms", Place.GYM,
        "pullup|4|8|0|0;row|4|8|0|0;lat_pulldown|3|10|0|0;cable_row|3|12|0|0;curl|3|10|0|0;hammer|3|12|0|0"),
    Template("Плечи", "Shoulders", "Все три пучка и трапеции", "All three heads and the traps", Place.GYM,
        "ohp|4|8|0|0;arnold|3|10|0|0;lateral|4|15|0|0;rear_delt|3|15|0|0;face_pull|3|15|0|0;shrug|3|12|0|0"),
    Template("Ягодицы", "Glutes", "Мост, тяги и отведения", "Thrusts, hinges and abductions", Place.GYM,
        "hip_thrust|4|10|0|0;rdl|3|10|0|0;bulgarian|3|10|0|0;abductor|3|15|0|0;kb_swing|3|15|0|0"),
    Template("Руки", "Arms", "Бицепс и трицепс по очереди", "Biceps and triceps in turn", Place.GYM,
        "curl|3|10|0|0;skullcrusher|3|10|0|0;hammer|3|12|0|0;pushdown|3|12|0|0;preacher|3|12|0|0;oh_ext|3|12|0|0"),
    Template("Пресс и кор", "Abs and core", "Блок, вис и ролик", "Cable, hanging and the wheel", Place.GYM,
        "cable_crunch|3|15|0|0;leg_raise|3|12|0|0;ab_wheel|3|10|0|0;pallof|3|12|0|0;plank|3|0|0|60"),
    Template("Кардио в зале", "Gym cardio", "Дорожка, гребля и велосипед", "Treadmill, rower and bike", Place.GYM, "treadmill|1|0|0|1200;rower|1|0|0|600;spin|1|0|0|900"),
    // ---- home ----
    Template("Дома без железа", "Home, no gear", "Только свой вес, полчаса", "Bodyweight only, half an hour", Place.HOME,
        "pushup|4|12|0|0;air_squat|4|20|0|0;inverted_row|3|10|0|0;glute_bridge|3|15|0|0;plank|3|0|0|45;mountain|3|20|0|0"),
    Template("Всё тело за 20 минут", "Whole body in 20 minutes", "Быстро и жарко, без инвентаря", "Quick and hot, no gear", Place.HOME,
        "burpee|3|10|0|0;jump_squat|3|15|0|0;pushup|3|12|0|0;mountain|3|30|0|0;plank|3|0|0|40"),
    Template("Дома с гантелями", "Home with dumbbells", "Пара гантелей – и всё тело", "A pair of dumbbells, the whole body", Place.HOME,
        "lunge|3|10|0|0;db_row|3|10|0|0;db_ohp|3|10|0|0;db_rdl|3|10|0|0;db_curl|3|12|0|0;oh_ext|3|12|0|0"),
    Template("Пресс дома", "Abs at home", "Шесть упражнений на коврике", "Six moves on a mat", Place.HOME,
        "crunch|3|20|0|0;lying_leg_raise|3|15|0|0;russian_twist|3|20|0|0;dead_bug|3|12|0|0;hollow|3|0|0|30;side_plank|2|0|0|30"),
    Template("Ноги и ягодицы дома", "Legs and glutes at home", "Мостики, приседы и прыжки", "Bridges, squats and jumps", Place.HOME,
        "glute_bridge|4|20|0|0;air_squat|4|20|0|0;skater|3|20|0|0;wall_sit|3|0|0|45;jump_squat|3|12|0|0"),
    Template("Грудь и руки дома", "Chest and arms at home", "Отжимания всех видов", "Push-ups of every kind", Place.HOME,
        "pushup|4|12|0|0;diamond_pushup|3|10|0|0;pike_pushup|3|8|0|0;bench_dip|3|15|0|0;knee_pushup|2|15|0|0"),
    Template("Табата", "Tabata", "20 секунд работы, 10 отдыха – 20 минут", "20 s on, 10 s off – 20 minutes", Place.HOME, "tabata|1|0|0|1200;stretch|1|0|0|300"),
    Template("Утренняя зарядка", "Morning routine", "Десять минут, чтобы проснуться", "Ten minutes to wake up", Place.HOME,
        "jumping_jack|2|30|0|0;air_squat|2|15|0|0;pushup|2|10|0|0;bird_dog|2|10|0|0;stretch|1|0|0|300"),
    Template("Растяжка на вечер", "Evening stretch", "Спокойно, перед сном", "Calm, before bed", Place.HOME, "foam_roll|1|0|0|300;stretch|1|0|0|900;bridge_hold|2|0|0|30"),
    Template("Йога и баланс", "Yoga and balance", "Йога, стойки на одной ноге", "Yoga and one-leg stands", Place.HOME, "yoga|1|0|0|1800;tree_pose|3|0|0|45;bird_dog|2|10|0|0"),
    // ---- outdoors ----
    Template("Турники во дворе", "Bars in the yard", "Подтягивания, брусья, вис", "Pull-ups, dips, hangs", Place.OUT,
        "pullup|4|6|0|0;dip|4|10|0|0;inverted_row|3|10|0|0;leg_raise|3|10|0|0;pushup|3|15|0|0;dead_hang|2|0|0|40"),
    Template("Воркаут-круг", "Street workout circuit", "Круг из шести упражнений, 3 раза", "Six moves in a circuit, three rounds", Place.OUT,
        "pullup|3|8|0|0;dip|3|10|0|0;pushup|3|15|0|0;jump_squat|3|15|0|0;burpee|3|10|0|0;plank|3|0|0|45"),
    Template("Пробежка 5 км", "5 km run", "Спокойный темп, чтобы можно было говорить", "An easy pace you can talk at", Place.OUT, "run|1|0|0|1800;stretch|1|0|0|300"),
    Template("Интервалы на стадионе", "Track intervals", "Разминка, ускорения, заминка", "Warm-up, sprints, cool-down", Place.OUT, "run|1|0|0|600;hiit|1|0|0|900;walk|1|0|0|600"),
    Template("Прыжки и лестница", "Jumps and stairs", "Ступеньки, прыжки, конькобежец", "Stairs, jumps, skaters", Place.OUT,
        "stairs|1|0|0|900;broad_jump|3|8|0|0;jump_squat|3|12|0|0;skater|3|20|0|0"),
    Template("Велопрогулка", "Bike ride", "Час в спокойном темпе", "An easy hour", Place.OUT, "bike|1|0|0|3600"),
    Template("Ходьба", "Walk", "45 минут быстрым шагом", "45 minutes at a brisk pace", Place.OUT, "walk|1|0|0|2700"),
)

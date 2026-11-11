package app.pumpling.domain

import app.pumpling.data.Pet

/**
 * A kind of pet. [likes] is the exercise family it eats one and a half times as much of (and shares with the team).
 * [find] says how its egg is found; [starter] eggs can be picked on the first day.
 */
data class Species(
    val id: String, val ru: String, val en: String, val likes: Cat?,
    val abilityRu: String, val abilityEn: String, val findRu: String, val findEn: String,
    val starter: Boolean = false, val legendary: Boolean = false, val color: Long,
    val names: List<String>, val namesEn: List<String>,
) {
    val name get() = tr(ru, en)
    val ability get() = tr(abilityRu, abilityEn)
    val find get() = tr(findRu, findEn)
}

object Pets {
    val all = listOf(
        Species("bull", "Бычок", "Bullock", Cat.LEGS, "×1,5 за ноги: приседы, становую, выпады", "×1.5 for legs: squats, deadlifts, lunges",
            "Стартовое яйцо. Или 80 подходов на ноги", "A starter egg. Or 80 sets for legs", true, color = 0xFFC9783A,
            names = listOf("Бодя", "Буба", "Тотошка", "Мука"), namesEn = listOf("Bodie", "Moo", "Toto", "Buck")),
        Species("otter", "Выдра", "Otter", Cat.CARDIO, "×1,5 за кардио и плавание", "×1.5 for cardio and swimming",
            "Стартовое яйцо. Или 6 часов кардио", "A starter egg. Or 6 hours of cardio", true, color = 0xFF7A5236,
            names = listOf("Плюх", "Ласка", "Нырок", "Бульк"), namesEn = listOf("Splash", "Dipper", "Ripple", "Pebble")),
        Species("lynx", "Рысь", "Lynx", Cat.FLEX, "×1,5 за растяжку, йогу и мобилити", "×1.5 for stretching, yoga and mobility",
            "Стартовое яйцо. Или 3 часа растяжки и йоги", "A starter egg. Or 3 hours of stretching and yoga", true, color = 0xFFB7A08A,
            names = listOf("Кисточка", "Тиша", "Мята", "Рыся"), namesEn = listOf("Tuft", "Willow", "Mint", "Pounce")),
        Species("octopus", "Осьминог", "Octopus", Cat.PULL, "×1,5 за тяги, подтягивания и бицепс", "×1.5 for rows, pull-ups and curls",
            "200 подтягиваний в сумме или 120 подходов тяг", "200 pull-ups in all or 120 pulling sets", color = 0xFF9A6BD0,
            names = listOf("Окто", "Чернилка", "Восьмёрка", "Присоска"), namesEn = listOf("Octo", "Inky", "Eight", "Squish")),
        Species("crab", "Краб", "Crab", Cat.PUSH, "×1,5 за жимы, грудь и трицепс", "×1.5 for presses, chest and triceps",
            "120 подходов жимов", "120 pressing sets", color = 0xFFD9493B,
            names = listOf("Клешня", "Боком", "Рачок", "Кракс"), namesEn = listOf("Pinch", "Sideways", "Snap", "Clack")),
        Species("hedgehog", "Ёж", "Hedgehog", Cat.CORE, "×1,5 за пресс, планки и удержания", "×1.5 for abs, planks and holds",
            "Час планок и удержаний в сумме", "An hour of planks and holds in all", color = 0xFF8C6A4E,
            names = listOf("Колючка", "Фырка", "Ёшка", "Шуршун"), namesEn = listOf("Prickle", "Huff", "Bramble", "Snuff")),
        Species("kangaroo", "Кенгуру", "Kangaroo", Cat.JUMP, "×1,5 за прыжки, бокс и скакалку", "×1.5 for jumps, boxing and rope",
            "15 тренировок с прыжками или ударами", "15 workouts with jumps or strikes", color = 0xFFC79A64,
            names = listOf("Прыг", "Джо", "Хук", "Сумочка"), namesEn = listOf("Hop", "Joey", "Hook", "Pouch")),
        Species("hummingbird", "Колибри", "Hummingbird", Cat.HIIT, "×1,5 за интервалы, табату и круговые", "×1.5 for intervals, tabata and circuits",
            "12 интервальных тренировок", "12 interval sessions", color = 0xFF2FAF7A,
            names = listOf("Жужа", "Искра", "Фьють", "Пчёлка"), namesEn = listOf("Buzz", "Spark", "Zip", "Flick")),
        Species("turtle", "Черепаха", "Turtle", null, "×2 за кардио от 45 минут", "×2 for cardio of 45 minutes and longer",
            "Кардио больше часа за раз", "Cardio for over an hour at a time", color = 0xFF3F9A8A,
            names = listOf("Тортилла", "Панцирь", "Тихоня", "Марафон"), namesEn = listOf("Shelly", "Steady", "Myrtle", "Miles")),
        Species("sloth", "Ленивец", "Sloth", null, "×1,5 за подходы после честного отдыха от 2,5 минуты; дни отдыха кормят команду лучше", "×1.5 for sets after an honest rest of 2.5 min; rest days feed the team better",
            "10 тренировок со средним отдыхом от 2,5 минуты", "10 workouts with an average rest of 2.5 min", color = 0xFF9B8B72,
            names = listOf("Тихон", "Соня", "Ленни", "Обнимашка"), namesEn = listOf("Lenny", "Snooze", "Mellow", "Hugs")),
        Species("squirrel", "Белка", "Squirrel", null, "×1,3 за всю тренировку, если в ней 5 упражнений и больше", "×1.3 for the whole workout with 5 or more exercises",
            "Тренировка из 6 разных упражнений", "A workout of 6 different exercises", color = 0xFFD06A2C,
            names = listOf("Рыжик", "Орешек", "Шустрик", "Пушинка"), namesEn = listOf("Rusty", "Nutmeg", "Zippy", "Fluff")),
        Species("gorilla", "Горилла", "Gorilla", null, "Рекорды кормят втрое", "Records feed three times as much",
            "25 рекордов", "25 records", color = 0xFF7D8590,
            names = listOf("Кинг", "Бабах", "Громила", "Лапа"), namesEn = listOf("King", "Boom", "Tank", "Paws")),
        Species("ant", "Муравей", "Ant", null, "×2 за подходы с весом не меньше своего", "×2 for sets with at least your own body weight",
            "Поднять вес своего тела", "Lift your own body weight", color = 0xFFC2412D,
            names = listOf("Мураш", "Атлант", "Крошка", "Тянучка"), namesEn = listOf("Atlas", "Tiny", "Crumb", "Haul")),
        Species("crane", "Журавль", "Crane", Cat.BAL, "×1,5 за баланс и стойки", "×1.5 for balance and handstands",
            "12 тренировок с балансом", "12 workouts with balance work", color = 0xFFE9E4DA,
            names = listOf("Цапля", "Стойка", "Тянь", "Журка"), namesEn = listOf("Poise", "Stilt", "Tian", "Grace")),
        Species("mole", "Крот", "Mole", null, "×1,5 за тренировку, начатую до 8 утра", "×1.5 for a workout started before 8 am",
            "10 тренировок до 7:30 утра", "10 workouts before 7:30 am", color = 0xFF4B4350,
            names = listOf("Рассвет", "Кротя", "Копуша", "Фонарик"), namesEn = listOf("Dawn", "Digby", "Burrow", "Lamp")),
        Species("owl", "Сова", "Owl", null, "×1,5 за тренировку, начатую после 20:00", "×1.5 for a workout started after 8 pm",
            "10 тренировок после 21:00", "10 workouts after 9 pm", color = 0xFF8A6440,
            names = listOf("Ухтыш", "Луна", "Полночь", "Филя"), namesEn = listOf("Hoot", "Luna", "Midnight", "Moss")),
        Species("hamster", "Хомяк", "Hamster", null, "+10% команде за каждую неделю цели подряд, до +50%", "+10% for the team per week of the goal in a row, up to +50%",
            "Недельная цель 6 недель подряд", "The weekly goal 6 weeks in a row", color = 0xFFE3A64C,
            names = listOf("Пухляш", "Колесо", "Щёчкин", "Зёрнышко"), namesEn = listOf("Chubs", "Wheel", "Cheeks", "Seedy")),
        Species("phoenix", "Феникс", "Phoenix", null, "×3 за первую тренировку после перерыва от 10 дней", "×3 for the first workout after a break of 10 days",
            "Вернуться после перерыва от 3 недель", "Come back after a break of 3 weeks", color = 0xFFEF7B2A,
            names = listOf("Уголёк", "Заря", "Искорка", "Пепел"), namesEn = listOf("Ember", "Dawn", "Sparky", "Ash")),
        Species("mammoth", "Мамонт", "Mammoth", null, "Ест тоннаж: +1 порция за каждые 500 кг", "Eats tonnage: +1 portion for every 500 kg",
            "250 тонн за всё время", "250 tonnes in all", color = 0xFF9A5A3A,
            names = listOf("Шерстяк", "Бивень", "Ледок", "Топ"), namesEn = listOf("Woolly", "Tusk", "Frost", "Stomp")),
        Species("dragon", "Дракончик", "Dragon", null, "×1,3 за всё", "×1.3 for everything",
            "Легенда: 120 тренировок и все девять семейств упражнений", "Legend: 120 workouts and all nine exercise families", legendary = true, color = 0xFF2E9C9C,
            names = listOf("Искр", "Дымок", "Чешуйка", "Шквал"), namesEn = listOf("Smoky", "Scales", "Gale", "Cinder")),
    )
    val byId = all.associateBy { it.id }
    fun of(id: String) = byId[id] ?: all[0]
    val starters = all.filter { it.starter }

    /** Growth needed to hatch, then for each stage. */
    const val HATCH = 30.0
    val STAGES = doubleArrayOf(HATCH, 220.0, 750.0, 2200.0)
}

enum class Stage(val ru: String, val en: String) {
    EGG("Яйцо", "Egg"), BABY("Малыш", "Baby"), TEEN("Подросток", "Youngster"), ADULT("Взрослый", "Grown"), MASTER("Мастер", "Master");
    val label get() = tr(ru, en)
    companion object {
        fun of(p: Pet): Stage = if (p.hatched == 0L) EGG else when {
            p.growth >= Pets.STAGES[3] -> MASTER; p.growth >= Pets.STAGES[2] -> ADULT; p.growth >= Pets.STAGES[1] -> TEEN; else -> BABY }
        /** 0..1 progress to the next stage. */
        fun progress(p: Pet): Float {
            val s = of(p); if (s == MASTER) return 1f
            val lo = if (s == EGG) 0.0 else Pets.STAGES[s.ordinal - 1]; val hi = Pets.STAGES[s.ordinal]
            return ((p.growth - lo) / (hi - lo)).toFloat().coerceIn(0f, 1f)
        }
    }
}

/** A grown pet's build, fixed by what it was raised on. */
enum class Form(val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String) {
    STRONG("strong", "Силач", "Powerhouse", "вырос на железе", "raised on iron"),
    ENDURE("endure", "Марафонец", "Marathoner", "вырос на кардио", "raised on cardio"),
    ATHLETE("athlete", "Атлет", "Athlete", "вырос на прыжках и интервалах", "raised on jumps and intervals"),
    GYMNAST("gymnast", "Гимнаст", "Gymnast", "вырос на гибкости, балансе и коре", "raised on flexibility, balance and core"),
    ALL("all", "Универсал", "All-rounder", "вырос на всём понемногу", "raised on a bit of everything");
    val label get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id }
        fun from(cats: Map<String, Double>): Form {
            val g = mapOf(
                STRONG to (cats["legs"] ?: 0.0) + (cats["push"] ?: 0.0) + (cats["pull"] ?: 0.0),
                ENDURE to (cats["cardio"] ?: 0.0),
                ATHLETE to (cats["jump"] ?: 0.0) + (cats["hiit"] ?: 0.0),
                GYMNAST to (cats["flex"] ?: 0.0) + (cats["bal"] ?: 0.0) + (cats["core"] ?: 0.0))
            val total = g.values.sum(); if (total <= 0) return ALL
            val top = g.maxBy { it.value }
            return if (top.value / total < .45) ALL else top.key
        }
    }
}

/** A pet's temper: one small rule of its own. */
enum class Nature(val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String) {
    STUBBORN("stubborn", "Упрямый", "Stubborn", "×1,5 за тяжёлые подходы (RPE 9–10)", "×1.5 for hard sets (RPE 9–10)"),
    GREEDY("greedy", "Обжора", "Greedy", "ест на 20% больше, но быстрее теряет форму", "eats 20% more but loses shape faster"),
    CURIOUS("curious", "Любопытный", "Curious", "+3 блина за каждое новое упражнение", "+3 plates for every new exercise"),
    SOCIAL("social", "Компанейский", "Social", "в команде ест три четверти, а не половину", "as a companion eats three quarters, not half"),
    SLEEPY("sleepy", "Соня", "Sleepy", "в дни отдыха переваривает больше", "digests more on rest days"),
    STEADY("steady", "Ровный", "Steady", "+15%, когда неделя идёт по плану", "+15% when the week goes to plan");
    val label get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: STEADY }
}

fun Pet.sp() = Pets.of(species)
fun Pet.stage() = Stage.of(this)
fun Pet.nat() = Nature.of(nature)
fun Pet.catMap(): Map<String, Double> = cats.split(',').filter { ':' in it }.associate { it.substringBefore(':') to (it.substringAfter(':').toDoubleOrNull() ?: 0.0) }
fun catString(m: Map<String, Double>) = m.entries.joinToString(",") { "${it.key}:${fmtRaw(it.value)}" }
private fun fmtRaw(v: Double) = ((v * 10).toLong() / 10.0).toString()

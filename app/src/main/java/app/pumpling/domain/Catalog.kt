package app.pumpling.domain

import android.content.Context

/** How an exercise is written down. */
enum class Kind(val code: Char) {
    WEIGHT('W'),   // weight × reps
    BODY('B'),     // own weight, reps; a belt or vest adds weight
    ASSIST('A'),   // a machine helps: the weight is taken off
    HOLD('T'),     // seconds (plus weight for a farmer's walk)
    CARDIO('C'),   // minutes and kilometres
    MINUTES('M');  // just minutes: yoga, a match, a class
    val name1: String get() = when (this) {
        WEIGHT -> tr("Вес × повторы", "Weight × reps"); BODY -> tr("Свой вес, повторы", "Bodyweight reps"); ASSIST -> tr("С помощью (гравитрон)", "Assisted")
        HOLD -> tr("Удержание, время", "Hold, time"); CARDIO -> tr("Кардио: время и дистанция", "Cardio: time and distance"); MINUTES -> tr("Только минуты", "Minutes only")
    }
    val timed get() = this == HOLD || this == CARDIO || this == MINUTES
    /** How a set of this kind is written, with an example. */
    val help: String get() = when (this) {
        WEIGHT -> tr("Пишете вес снаряда и сколько раз подняли: 60 кг × 8. Для штанги, гантелей, гирь и тренажёров.",
            "Write the weight and how many times you lifted it: 60 kg × 8. For barbells, dumbbells, kettlebells and machines.")
        BODY -> tr("Только повторы: подтягивания × 10. Поле веса оставьте пустым. Если вешаете блин на пояс – впишите его вес, он прибавится к вашему.",
            "Just the reps: pull-ups × 10. Leave the weight empty. With a plate on a belt, write its weight – it adds to yours.")
        ASSIST -> tr("Гравитрон или резинка помогают подтянуться. Пишете, сколько килограммов помощи выставили, и повторы: помощь 20 кг × 8. Чем меньше помощь, тем вы сильнее – когда она уменьшится, будет рекорд.",
            "An assisted machine or band helps you up. Write how many kilos of help you set and the reps: help 20 kg × 8. Less help means stronger – when it drops, that's a record.")
        HOLD -> tr("Сколько секунд держали: планка 60 с. Если держите груз (прогулка фермера) – впишите и вес.",
            "How many seconds you held: a plank, 60 s. If you hold a load (a farmer's walk), add the weight too.")
        CARDIO -> tr("Километры и минуты: бег 5 км за 30 мин. Темп и лучшее время посчитаются сами. Если дистанции нет – только минуты.",
            "Kilometres and minutes: a 5 km run in 30 min. Pace and best times are worked out for you. No distance? Just minutes.")
        MINUTES -> tr("Только время: йога 30 мин, футбол 60 мин. Подходит для занятий, где подходов нет.",
            "Just the time: yoga 30 min, football 60 min. For activities without sets.")
    }
    companion object { fun of(c: Char) = entries.firstOrNull { it.code == c } ?: WEIGHT }
}

/** What a pet likes: the family an exercise belongs to. */
enum class Cat(val id: String, val ru: String, val en: String, val color: Long) {
    LEGS("legs", "Ноги", "Legs", 0xFFE0703C), PUSH("push", "Жимы", "Push", 0xFFD2484E), PULL("pull", "Тяги", "Pull", 0xFF6B5BD2),
    CORE("core", "Кор", "Core", 0xFF3E9C6A), CARDIO("cardio", "Кардио", "Cardio", 0xFF2F8FD0), FLEX("flex", "Гибкость", "Flexibility", 0xFFB461C7),
    JUMP("jump", "Прыжки и удары", "Jumps & strikes", 0xFFE5A21E), HIIT("hiit", "Интервалы", "Intervals", 0xFFE04C8A), BAL("bal", "Баланс", "Balance", 0xFF2AA8A0);
    val label get() = tr(ru, en)
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: PUSH }
}

enum class Muscle(val id: String, val ru: String, val en: String, val upper: Boolean, val front: Boolean) {
    CHEST("chest", "Грудь", "Chest", true, true), DELTS("delts", "Плечи", "Shoulders", true, true), TRI("tri", "Трицепс", "Triceps", true, false),
    BI("bi", "Бицепс", "Biceps", true, true), FORE("fore", "Предплечья", "Forearms", true, true), LATS("lats", "Широчайшие", "Lats", true, false),
    UPPER("upper", "Верх спины", "Upper back", true, false), LOWER("lower", "Поясница", "Lower back", false, false), ABS("abs", "Пресс", "Abs", false, true),
    GLUTES("glutes", "Ягодицы", "Glutes", false, false), QUADS("quads", "Квадрицепсы", "Quads", false, true), HAMS("hams", "Бицепс бедра", "Hamstrings", false, false),
    CALVES("calves", "Икры", "Calves", false, false);
    val label get() = tr(ru, en)
    /** Push muscles against pull muscles, for the balance chart; null for neither. */
    val push: Boolean? get() = when (this) { CHEST, DELTS, TRI -> true; LATS, UPPER, BI -> false; else -> null }
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
}

enum class Equip(val id: String, val ru: String, val en: String) {
    BAR("bar", "Штанга", "Barbell"), DB("db", "Гантели", "Dumbbells"), KB("kb", "Гиря", "Kettlebell"), MACH("mach", "Тренажёр", "Machine"),
    CABLE("cable", "Блок", "Cable"), EZ("ez", "EZ-гриф", "EZ bar"), BW("bw", "Свой вес", "Bodyweight"), BAND("band", "Резинка", "Band"),
    CARDIO("cardio", "Кардио", "Cardio"), OTHER("other", "Другое", "Other");
    val label get() = tr(ru, en)
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: OTHER }
}

/** One exercise from the catalogue or made by the user (id starts with «u»). */
data class Ex(
    val id: String, val ru: String, val en: String, val kind: Kind, val primary: List<Muscle>, val secondary: List<Muscle>,
    val equip: Equip, val cat: Cat, val own: Boolean = false,
    val gear: String = "",   // the user's own equipment by name; empty for the built-in kinds
    val aliases: String = "", // other names people search by
) {
    val name get() = if (own) ru else tr(ru, en)
    /** The equipment as shown: the user's own name or the built-in kind. */
    val equipLabel get() = gear.ifEmpty { equip.label }
    val muscles get() = primary + secondary
    /** Does a search query match: name in either language, synonyms, muscles or equipment; «ё» counts as «е». */
    fun matches(q: String): Boolean {
        val n = norm(q); if (n.isEmpty()) return true
        val hay = norm(listOf(ru, en, aliases, gear, equip.ru, equip.en, cat.ru, cat.en).joinToString(" ") + " " + muscles.joinToString(" ") { it.ru + " " + it.en })
        return n.split(' ').filter { it.isNotBlank() }.all { w -> hay.contains(w) }
    }
    /** «Подтягивания», «отжимания» and the like count your own weight into the tonnage. */
    val bodyweight get() = kind == Kind.BODY || kind == Kind.ASSIST
}

object Catalog {
    @Volatile private var builtin: List<Ex>? = null
    @Volatile var own: List<Ex> = emptyList()

    fun builtin(ctx: Context): List<Ex> = builtin ?: synchronized(this) {
        builtin ?: ctx.assets.open("exercises.tsv").bufferedReader().readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
            val c = line.split('\t')
            fun ms(s: String) = s.split(',').mapNotNull { Muscle.of(it.trim()) }
            Ex(c[0], c[1], c[2], Kind.of(c[3][0]), ms(c[4]), ms(c.getOrElse(5) { "" }), Equip.of(c.getOrElse(6) { "" }), Cat.of(c.getOrElse(7) { "" }), aliases = c.getOrElse(8) { "" })
        }.also { builtin = it }
    }

    /** For tests and renders: a catalogue read from a file. */
    fun load(lines: List<String>) {
        builtin = lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
            val c = line.split('\t')
            fun ms(s: String) = s.split(',').mapNotNull { Muscle.of(it.trim()) }
            Ex(c[0], c[1], c[2], Kind.of(c[3][0]), ms(c[4]), ms(c.getOrElse(5) { "" }), Equip.of(c.getOrElse(6) { "" }), Cat.of(c.getOrElse(7) { "" }), aliases = c.getOrElse(8) { "" })
        }
    }

    fun all(): List<Ex> = (builtin ?: emptyList()) + own
    private var index: Map<String, Ex> = emptyMap(); private var indexed: Pair<List<Ex>?, List<Ex>>? = null
    fun byId(id: String): Ex? {
        val key = builtin to own
        if (indexed?.first !== key.first || indexed?.second !== key.second) { index = all().associateBy { it.id }; indexed = key }
        return index[id]
    }
    fun of(id: String): Ex = byId(id) ?: Ex(id, id, id, Kind.WEIGHT, emptyList(), emptyList(), Equip.OTHER, Cat.PUSH)
}

fun norm(s: String) = s.lowercase().replace('ё', 'е').replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex(" +"), " ").trim()

package app.pumpling.domain

/**
 * Where a workout was (#13). The pets train there too: each place has its own scene.
 * [code] is what a workout keeps; an empty or unknown code means it wasn't said.
 * [color] paints its days in the calendar (#24); 0 keeps the usual tomato of the heat map.
 */
enum class Place(val code: String, private val ru: String, private val en: String, val color: Long) {
    HOME("home", "Дом", "Home", 0xFF3B7DD8),
    GYM("gym", "Зал", "Gym", 0),
    STREET("street", "Улица", "Outside", 0xFF3E9C6A),
    /** A group class: aerobics, a dance class, functional training with a coach (#21). */
    GROUP("group", "Группа", "Class", 0xFF9B59D0);

    val label get() = tr(ru, en)

    companion object {
        fun of(code: String): Place? = entries.firstOrNull { it.code == code }
    }
}

package app.pumpling.domain

/**
 * Where a workout was (#13). The pets train there too: each place has its own scene.
 * [code] is what a workout keeps; an empty or unknown code means it wasn't said.
 */
enum class Place(val code: String, private val ru: String, private val en: String) {
    HOME("home", "Дом", "Home"),
    GYM("gym", "Зал", "Gym"),
    STREET("street", "Улица", "Outside");

    val label get() = tr(ru, en)

    companion object {
        fun of(code: String): Place? = entries.firstOrNull { it.code == code }
    }
}

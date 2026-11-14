package app.pumpling.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Everything that isn't a workout or a pet: plates, the gym, goals, units, quests. */
data class Settings(
    val onboarded: Boolean = false,
    val plates: Int = 0,                        // the money
    val items: Set<String> = emptySet(),        // gym things bought
    val hidden: Set<String> = emptySet(),       // bought things put away
    val weekGoal: Int = 3,                      // workouts a week
    val lb: Boolean = false,
    val theme: String = "system",               // light · dark · system
    val bodyweight: Double = 0.0,               // kg, for strength standards and the ant
    val sex: String = "",                       // m · f · "" (for strength standards and DOTS only)
    val digested: Long = 0,                     // last epoch day digestion ran for
    val questWeek: Long = 0,                    // Monday of the week quests were dealt
    val quests: List<String> = emptyList(),     // quest ids this week
    val questsDone: Set<String> = emptySet(),
    val rest: Int = 90,                         // default rest, seconds; 0 = no rest timer at all
    val restSound: Boolean = true,
    val sick: Long = 0,                         // «on sick leave» since this day; 0 = not
    val sickDays: Set<Long> = emptySet(),       // days spent on sick leave (not counted against anything)
    val inbox: List<String> = emptyList(),      // news to show once: «egg:otter», «hatch:12», «stage:12:2»
    val seen: Set<String> = emptySet(),         // hints read
    val yearGoal: Int = 0,                      // workouts this year, 0 = no goal
    val bar: Double = 20.0,                     // the bar for the plate calculator, kg
    val plateSet: String = "25,20,15,10,5,2.5,1.25",
    val cardBg: Int = 0,                        // last background chosen for cards
    val hideWeights: Boolean = false,           // on shared cards
    val town: String = "", val lat: Double = Double.NaN, val lon: Double = Double.NaN,  // for the window: weather and the sun
    val guessed: Boolean = false,               // the town was guessed from the time zone once
    val outside: String = "",                   // last weather, as Outside.json()
    val gear: List<String> = emptyList(),       // the user's own equipment, by name
    val goals: Set<String> = emptySet(),        // Goal names
    val checkIn: Int = 0,                       // weigh and measure every this many days; 0 = never ask
    val weather: Boolean = true,                // the real weather in the window; off = nothing goes online
    val remindDays: Set<Int> = emptySet(),      // days to remind about a workout, 1 Monday .. 7 Sunday; empty = never
    val remindAt: Int = 18 * 60,                // minutes after midnight the reminder comes at
    val health: Boolean = false,                // finished workouts are written to Health Connect
    val achieved: Set<String> = emptySet(),     // achievement tiers paid: «workouts:2»
) {
    fun json(): JSONObject = JSONObject().apply {
        put("onboarded", onboarded); put("plates", plates); put("items", JSONArray(items.toList())); put("hidden", JSONArray(hidden.toList()))
        put("weekGoal", weekGoal); put("lb", lb); put("theme", theme); put("bodyweight", bodyweight); put("sex", sex); put("digested", digested)
        put("questWeek", questWeek); put("quests", JSONArray(quests)); put("questsDone", JSONArray(questsDone.toList())); put("rest", rest); put("restSound", restSound)
        put("sick", sick); put("sickDays", JSONArray(sickDays.toList())); put("inbox", JSONArray(inbox)); put("seen", JSONArray(seen.toList()))
        put("yearGoal", yearGoal); put("bar", bar); put("plateSet", plateSet); put("cardBg", cardBg); put("hideWeights", hideWeights)
        put("town", town); if (!lat.isNaN()) { put("lat", lat); put("lon", lon) }; put("guessed", guessed); put("outside", outside); put("gear", JSONArray(gear)); put("goals", JSONArray(goals.toList())); put("checkIn", checkIn)
        put("weather", weather); put("remindDays", JSONArray(remindDays.toList())); put("remindAt", remindAt); put("health", health); put("achieved", JSONArray(achieved.toList()))
    }

    companion object {
        fun of(j: JSONObject): Settings {
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            fun set(k: String) = list(k).toSet()
            return Settings(j.optBoolean("onboarded"), j.optInt("plates"), set("items"), set("hidden"), j.optInt("weekGoal", 3), j.optBoolean("lb"),
                j.optString("theme", "system"), j.optDouble("bodyweight", 0.0), j.optString("sex"), j.optLong("digested"),
                j.optLong("questWeek"), list("quests"), set("questsDone"), j.optInt("rest", 90), j.optBoolean("restSound", true),
                j.optLong("sick"), list("sickDays").mapNotNull { it.toLongOrNull() }.toSet(), list("inbox"), set("seen"), j.optInt("yearGoal"),
                j.optDouble("bar", 20.0), j.optString("plateSet", "25,20,15,10,5,2.5,1.25"), j.optInt("cardBg"), j.optBoolean("hideWeights"),
                j.optString("town"), j.optDouble("lat", Double.NaN), j.optDouble("lon", Double.NaN), j.optBoolean("guessed"), j.optString("outside"), list("gear"), set("goals"), j.optInt("checkIn"),
                j.optBoolean("weather", true), (j.optJSONArray("remindDays")?.let { a -> (0 until a.length()).map { a.getInt(it) } } ?: emptyList()).toSet(),
                j.optInt("remindAt", 18 * 60), j.optBoolean("health"), set("achieved"))
        }
    }
}

private val Context.store by preferencesDataStore("pumpling")
private val KEY = stringPreferencesKey("settings")

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.store.data.map { p -> p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings() }
    suspend fun now(): Settings = settings.first()
    suspend fun update(f: (Settings) -> Settings) { ctx.store.edit { p ->
        val cur = p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings()
        p[KEY] = f(cur).json().toString() } }
}

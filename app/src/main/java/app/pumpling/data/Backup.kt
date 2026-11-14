package app.pumpling.data

import androidx.room.withTransaction
import app.pumpling.AppContainer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Everything the app keeps, as one zip the user owns: «pumpling.json» with the workouts, sets, routines,
 * own exercises, measurements, pets and settings, and the progress photos beside it.
 * Reading a backup replaces what is on the phone.
 */
object Backup {
    /** Raised when the file's layout changes in a way older versions can't read. */
    const val FORMAT = 1
    private const val DATA = "pumpling.json"
    private const val PHOTOS = "photos/"

    class Everything(val workouts: List<Workout>, val sets: List<WSet>, val own: List<OwnEx>, val routines: List<Routine>,
                     val bodies: List<Body>, val pets: List<Pet>, val settings: Settings)

    /** The file is not a Pumpling backup, or was made by a newer version. */
    class Unreadable(val newer: Boolean) : Exception()

    // ---------- json ----------
    private fun <T> arr(l: List<T>, f: (T) -> JSONObject) = JSONArray().apply { l.forEach { put(f(it)) } }
    private fun <T> list(j: JSONObject, k: String, f: (JSONObject) -> T): List<T> = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { f(a.getJSONObject(it)) } } ?: emptyList()

    private fun Workout.json() = JSONObject().put("id", id).put("start", start).put("end", end).put("title", title).put("note", note).put("routineId", routineId)
        .put("food", food).put("plates", plates).put("prs", prs).put("leader", leader).put("feel", feel).put("manual", manual).put("classOf", classOf).put("muscles", muscles)
    private fun workout(j: JSONObject) = Workout(j.getLong("id"), j.getLong("start"), j.optLong("end"), j.optString("title"), j.optString("note"), j.optLong("routineId"),
        j.optDouble("food", 0.0), j.optInt("plates"), j.optInt("prs"), j.optString("leader"), j.optInt("feel"), j.optBoolean("manual"), j.optString("classOf"), j.optString("muscles"))

    private fun WSet.json() = JSONObject().put("id", id).put("workoutId", workoutId).put("exId", exId).put("block", block).put("idx", idx).put("weight", weight).put("reps", reps)
        .put("secs", secs).put("meters", meters).put("rpe", rpe).put("warmup", warmup).put("done", done).put("doneAt", doneAt).put("pr", pr).put("superset", superset).put("side", side)
    private fun set(j: JSONObject) = WSet(j.getLong("id"), j.getLong("workoutId"), j.getString("exId"), j.optInt("block"), j.optInt("idx"), j.optDouble("weight", 0.0), j.optInt("reps"),
        j.optInt("secs"), j.optDouble("meters", 0.0), j.optInt("rpe"), j.optBoolean("warmup"), j.optBoolean("done"), j.optLong("doneAt"), j.optString("pr"), j.optInt("superset"), j.optInt("side"))

    private fun OwnEx.json() = JSONObject().put("id", id).put("name", name).put("kind", kind).put("primary", primary).put("secondary", secondary).put("equip", equip).put("cat", cat).put("archived", archived)
    private fun own(j: JSONObject) = OwnEx(j.getLong("id"), j.getString("name"), j.optString("kind", "W"), j.optString("primary"), j.optString("secondary"), j.optString("equip", "other"), j.optString("cat", "push"), j.optBoolean("archived"))

    private fun Routine.json() = JSONObject().put("id", id).put("name", name).put("items", items).put("note", note).put("created", created).put("order", order)
    private fun routine(j: JSONObject) = Routine(j.getLong("id"), j.getString("name"), j.optString("items"), j.optString("note"), j.optLong("created"), j.optInt("order"))

    private fun Body.json() = JSONObject().put("id", id).put("day", day).put("weight", weight).put("waist", waist).put("chest", chest).put("hips", hips).put("arm", arm).put("thigh", thigh).put("fat", fat).put("photo", photo)
    private fun body(j: JSONObject) = Body(j.getLong("id"), j.getLong("day"), j.optDouble("weight", 0.0), j.optDouble("waist", 0.0), j.optDouble("chest", 0.0), j.optDouble("hips", 0.0),
        j.optDouble("arm", 0.0), j.optDouble("thigh", 0.0), j.optDouble("fat", 0.0), j.optString("photo"))

    private fun Pet.json() = JSONObject().put("id", id).put("species", species).put("name", name).put("nature", nature).put("found", found).put("hatched", hatched).put("growth", growth)
        .put("belly", belly).put("shape", shape).put("form", form).put("cats", cats).put("slot", slot).put("workouts", workouts).put("shiny", shiny)
    private fun pet(j: JSONObject) = Pet(j.getLong("id"), j.getString("species"), j.optString("name"), j.optString("nature"), j.optLong("found"), j.optLong("hatched"), j.optDouble("growth", 0.0),
        j.optDouble("belly", 0.0), j.optDouble("shape", 60.0), j.optString("form"), j.optString("cats"), j.optInt("slot", -1), j.optInt("workouts"), j.optBoolean("shiny"))

    fun encode(e: Everything, version: String = ""): String = JSONObject().put("app", "pumpling").put("format", FORMAT).put("version", version).put("at", System.currentTimeMillis())
        .put("workouts", arr(e.workouts) { it.json() }).put("sets", arr(e.sets) { it.json() }).put("own", arr(e.own) { it.json() }).put("routines", arr(e.routines) { it.json() })
        .put("bodies", arr(e.bodies) { it.json() }).put("pets", arr(e.pets) { it.json() }).put("settings", e.settings.json()).toString()

    fun decode(text: String): Everything {
        val j = runCatching { JSONObject(text) }.getOrNull() ?: throw Unreadable(false)
        if (j.optString("app") != "pumpling") throw Unreadable(false)
        if (j.optInt("format", 0) > FORMAT) throw Unreadable(true)
        return runCatching {
            Everything(list(j, "workouts", ::workout), list(j, "sets", ::set), list(j, "own", ::own), list(j, "routines", ::routine), list(j, "bodies", ::body), list(j, "pets", ::pet),
                Settings.of(j.optJSONObject("settings") ?: JSONObject()))
        }.getOrElse { throw Unreadable(false) }
    }

    // ---------- the file ----------
    /** Writes everything to [out] and closes it. A workout going on right now is written too, as it stands. */
    suspend fun write(c: AppContainer, out: OutputStream, version: String) {
        val e = Everything(c.dao.workoutList(), c.dao.setList(), c.dao.ownExList(), c.dao.routineList(), c.dao.bodyList(), c.dao.petList(), c.settings.now())
        ZipOutputStream(out.buffered()).use { z ->
            z.putNextEntry(ZipEntry(DATA)); z.write(encode(e, version).toByteArray()); z.closeEntry()
            for (b in e.bodies) { if (b.photo.isEmpty()) continue
                val f = File(c.photos, b.photo); if (!f.isFile) continue
                z.putNextEntry(ZipEntry(PHOTOS + f.name)); f.inputStream().use { it.copyTo(z) }; z.closeEntry() }
        }
    }

    /**
     * Reads a backup from [input] and puts it in place of everything on the phone. Nothing is touched
     * until the whole file is read and understood. Returns what was read; throws [Unreadable] for a wrong file.
     */
    suspend fun read(c: AppContainer, input: InputStream): Everything {
        val tmp = File(c.app.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            var text: String? = null
            ZipInputStream(input.buffered()).use { z ->
                while (true) { val en = z.nextEntry ?: break
                    if (en.name == DATA) text = z.readBytes().toString(Charsets.UTF_8)
                    // only the bare name is taken, so an entry can't write outside the folder
                    else if (!en.isDirectory && en.name.startsWith(PHOTOS)) File(en.name).name.takeIf { it.isNotEmpty() && it != "." && it != ".." }?.let { n -> File(tmp, n).outputStream().use { o -> z.copyTo(o) } }
                }
            }
            val e = decode(text ?: throw Unreadable(false))
            c.db.withTransaction {
                c.dao.clearSets(); c.dao.clearWorkouts(); c.dao.clearOwnEx(); c.dao.clearRoutines(); c.dao.clearBodies(); c.dao.clearPets()
                c.dao.insertWorkouts(e.workouts); e.sets.chunked(500).forEach { c.dao.insertSets(it) }; c.dao.insertOwn(e.own); c.dao.insertRoutines(e.routines)
                c.dao.insertBodies(e.bodies); c.dao.insertPets(e.pets)
            }
            // Health Connect is asked for again on this phone; the weather is fetched anew
            c.settings.update { e.settings.copy(onboarded = true, health = false, outside = "") }
            c.photos.listFiles()?.forEach { it.delete() }
            tmp.listFiles()?.forEach { it.copyTo(File(c.photos, it.name), overwrite = true) }
            return e
        } finally { tmp.deleteRecursively() }
    }
}

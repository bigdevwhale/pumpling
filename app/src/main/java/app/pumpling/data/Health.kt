package app.pumpling.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import app.pumpling.domain.Cat
import app.pumpling.domain.Catalog
import app.pumpling.domain.Equip
import app.pumpling.domain.Kind
import java.time.Instant
import java.time.ZoneId

/**
 * Finished workouts written to Health Connect as exercise sessions – when it began, when it ended,
 * what kind it was, its name and note – so other health apps on the phone see them. Nothing is read.
 * A workout is written under its own id, so editing it replaces the session and deleting it removes it.
 */
object Health {
    val PERMS = setOf(HealthPermission.getWritePermission(ExerciseSessionRecord::class))

    /** [HealthConnectClient.SDK_AVAILABLE], or why not: no Health Connect on this phone, or it wants an update. */
    fun status(ctx: Context): Int = runCatching { HealthConnectClient.getSdkStatus(ctx) }.getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)
    fun available(ctx: Context) = status(ctx) == HealthConnectClient.SDK_AVAILABLE
    fun needsUpdate(ctx: Context) = status(ctx) == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    fun contract() = PermissionController.createRequestPermissionResultContract()

    suspend fun granted(ctx: Context): Boolean = runCatching {
        available(ctx) && HealthConnectClient.getOrCreate(ctx).permissionController.getGrantedPermissions().containsAll(PERMS) }.getOrDefault(false)

    private fun key(id: Long) = "workout-$id"

    /** The kind of session a workout is: what most of its sets were. */
    fun type(w: Workout, sets: List<WSet>): Int {
        fun byName(id: String): Int? = when (id) {
            "pilates" -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
            "yoga" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "stretching_class" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "hiit", "tabata", "circuit" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "dance" -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            "kickboxing", "martial" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "run", "trail_run", "sprint" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "treadmill" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
            "walk", "incline_walk", "nordic" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            "hike" -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            "bike", "cycling_outdoor" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            "spin", "air_bike" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY
            "rower" -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE
            "elliptical" -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
            "stairs", "stair_walk" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
            "swim", "swimming_laps" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "ski", "ski_erg" -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            "skate", "rollerblading" -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            else -> null
        }
        if (w.classOf.isNotEmpty()) return byName(w.classOf) ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        val work = sets.filter { it.done && !it.warmup }
        val top = work.groupingBy { it.exId }.eachCount().maxByOrNull { e -> val ex = Catalog.of(e.key)
            // a run is one «set» however long it was: timed work is weighed by its minutes
            if (ex.kind == Kind.CARDIO || ex.kind == Kind.MINUTES) work.filter { it.exId == e.key }.sumOf { it.secs } / 150.0 else e.value.toDouble() }?.key
            ?: return ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        val ex = Catalog.of(top)
        byName(top)?.let { return it }
        return when {
            ex.cat == Cat.FLEX -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            ex.cat == Cat.HIIT -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            ex.cat == Cat.CARDIO -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            work.count { Catalog.of(it.exId).equip == Equip.BW } * 2 > work.size -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            else -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        }
    }

    private fun record(w: Workout, sets: List<WSet>): ExerciseSessionRecord {
        val zone = ZoneId.systemDefault()
        val a = Instant.ofEpochMilli(w.start); val b = Instant.ofEpochMilli(maxOf(w.end, w.start + 60_000))
        return ExerciseSessionRecord(
            startTime = a, startZoneOffset = zone.rules.getOffset(a), endTime = b, endZoneOffset = zone.rules.getOffset(b),
            metadata = Metadata.manualEntry(clientRecordId = key(w.id), clientRecordVersion = System.currentTimeMillis()),
            exerciseType = type(w, sets), title = w.title.ifBlank { null }, notes = w.note.ifBlank { null })
    }

    /** Writes finished workouts with their sets; one already there is replaced. Returns how many went through. */
    suspend fun write(ctx: Context, items: List<Pair<Workout, List<WSet>>>): Int = runCatching {
        if (!granted(ctx)) return 0
        val client = HealthConnectClient.getOrCreate(ctx)
        var n = 0
        items.filter { it.first.end > 0 }.chunked(200).forEach { part -> client.insertRecords(part.map { (w, s) -> record(w, s) }); n += part.size }
        n
    }.getOrDefault(0)

    suspend fun delete(ctx: Context, workoutId: Long) { runCatching {
        if (!granted(ctx)) return
        HealthConnectClient.getOrCreate(ctx).deleteRecords(ExerciseSessionRecord::class, recordIdsList = emptyList(), clientRecordIdsList = listOf(key(workoutId)))
    } }
}

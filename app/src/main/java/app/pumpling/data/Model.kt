package app.pumpling.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One workout. [end] is 0 while it is going on. What it gave the pets is written in when it ends. */
@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val start: Long,
    val end: Long = 0,
    val title: String = "",
    val note: String = "",
    val routineId: Long = 0,
    val food: Double = 0.0,        // portions the team ate
    val plates: Int = 0,           // plates earned
    val prs: Int = 0,              // records set
    val leader: String = "",       // species that led the team
    val feel: Int = 0,             // 1..5 how it went, 0 not said
    val manual: Boolean = false,   // written afterwards, not followed live
)

/**
 * One set. [block] is the exercise's place in the workout, [idx] the set's place in it.
 * [weight] is kilograms (help for an assisted exercise), [meters] distance, [secs] time.
 * [pr] lists records the set broke («e1rm,weight,reps,hold,dist,time,pace»).
 */
@Entity(indices = [Index("workoutId"), Index("exId")])
data class WSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exId: String,
    val block: Int,
    val idx: Int,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val secs: Int = 0,
    val meters: Double = 0.0,
    val rpe: Int = 0,
    val warmup: Boolean = false,
    val done: Boolean = false,
    val doneAt: Long = 0,
    val pr: String = "",
    val superset: Int = 0,         // blocks with the same number > 0 are done as a superset
)

/** An exercise the user made. Its id in sets is «u<id>». */
@Entity
data class OwnEx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,              // Kind.code
    val primary: String,           // muscle ids, comma-separated
    val secondary: String = "",
    val equip: String = "other",
    val cat: String = "push",
    val archived: Boolean = false,
)

/** A saved plan: exercises with their sets. [items] is «exId|sets|reps|weight|secs» joined by «;». */
@Entity
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val items: String,
    val note: String = "",
    val created: Long = 0,
    val order: Int = 0,
)

/** Body weight and measurements for a day; 0 means not measured. [photo] is a file in files/photos. */
@Entity
data class Body(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val weight: Double = 0.0,
    val waist: Double = 0.0,
    val chest: Double = 0.0,
    val hips: Double = 0.0,
    val arm: Double = 0.0,
    val thigh: Double = 0.0,
    val fat: Double = 0.0,
    val photo: String = "",
)

/**
 * A pet. Until [hatched] it is an egg and [growth] counts towards hatching.
 * [belly] is food eaten and not yet digested: it turns into growth on rest days.
 * [shape] 0..100 is how fit it looks (low = round as a dumpling). [cats] counts food by family, «legs:120,cardio:40».
 * [slot]: 0 leader, 1 and 2 companions, -1 resting at home.
 */
@Entity
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val name: String,
    val nature: String,
    val found: Long,               // epoch day
    val hatched: Long = 0,         // epoch day, 0 = still an egg
    val growth: Double = 0.0,
    val belly: Double = 0.0,
    val shape: Double = 60.0,
    val form: String = "",
    val cats: String = "",
    val slot: Int = -1,
    val workouts: Int = 0,
    val shiny: Boolean = false,
)

@Dao
interface Dao0 {
    @Query("SELECT * FROM Workout ORDER BY start DESC") fun workouts(): Flow<List<Workout>>
    @Query("SELECT * FROM Workout ORDER BY start DESC") suspend fun workoutList(): List<Workout>
    @Query("SELECT * FROM Workout WHERE id = :id") suspend fun workout(id: Long): Workout?
    @Query("SELECT * FROM Workout WHERE `end` = 0 ORDER BY start DESC LIMIT 1") fun live(): Flow<Workout?>
    @Query("SELECT * FROM Workout WHERE `end` = 0 ORDER BY start DESC LIMIT 1") suspend fun liveNow(): Workout?
    @Insert suspend fun insert(w: Workout): Long
    @Update suspend fun update(w: Workout)
    @Query("DELETE FROM Workout WHERE id = :id") suspend fun deleteWorkout(id: Long)

    @Query("SELECT * FROM WSet ORDER BY workoutId, block, idx") fun sets(): Flow<List<WSet>>
    @Query("SELECT * FROM WSet ORDER BY workoutId, block, idx") suspend fun setList(): List<WSet>
    @Query("SELECT * FROM WSet WHERE workoutId = :w ORDER BY block, idx") fun setsOf(w: Long): Flow<List<WSet>>
    @Query("SELECT * FROM WSet WHERE workoutId = :w ORDER BY block, idx") suspend fun setsOfNow(w: Long): List<WSet>
    @Insert suspend fun insert(s: WSet): Long
    @Insert suspend fun insertSets(s: List<WSet>)
    @Update suspend fun update(s: WSet)
    @Update suspend fun updateSets(s: List<WSet>)
    @Query("DELETE FROM WSet WHERE id = :id") suspend fun deleteSet(id: Long)
    @Query("DELETE FROM WSet WHERE workoutId = :w") suspend fun deleteSetsOf(w: Long)
    @Query("DELETE FROM WSet WHERE workoutId = :w AND block = :b") suspend fun deleteBlock(w: Long, b: Int)

    @Query("SELECT * FROM OwnEx ORDER BY name") fun ownEx(): Flow<List<OwnEx>>
    @Query("SELECT * FROM OwnEx ORDER BY name") suspend fun ownExList(): List<OwnEx>
    @Insert suspend fun insert(e: OwnEx): Long
    @Update suspend fun update(e: OwnEx)

    @Query("SELECT * FROM Routine ORDER BY `order`, created") fun routines(): Flow<List<Routine>>
    @Query("SELECT * FROM Routine ORDER BY `order`, created") suspend fun routineList(): List<Routine>
    @Insert suspend fun insert(r: Routine): Long
    @Update suspend fun update(r: Routine)
    @Query("DELETE FROM Routine WHERE id = :id") suspend fun deleteRoutine(id: Long)

    @Query("SELECT * FROM Body ORDER BY day DESC") fun bodies(): Flow<List<Body>>
    @Query("SELECT * FROM Body ORDER BY day DESC") suspend fun bodyList(): List<Body>
    @Insert suspend fun insert(b: Body): Long
    @Update suspend fun update(b: Body)
    @Query("DELETE FROM Body WHERE id = :id") suspend fun deleteBody(id: Long)

    @Query("SELECT * FROM Pet ORDER BY found, id") fun pets(): Flow<List<Pet>>
    @Query("SELECT * FROM Pet ORDER BY found, id") suspend fun petList(): List<Pet>
    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)
    @Update suspend fun updatePets(p: List<Pet>)

    @Query("DELETE FROM Workout") suspend fun clearWorkouts()
    @Query("DELETE FROM WSet") suspend fun clearSets()
    @Query("DELETE FROM Pet") suspend fun clearPets()
    @Query("DELETE FROM Body") suspend fun clearBodies()
    @Query("DELETE FROM Routine") suspend fun clearRoutines()
    @Query("DELETE FROM OwnEx") suspend fun clearOwnEx()
}

@Database(entities = [Workout::class, WSet::class, OwnEx::class, Routine::class, Body::class, Pet::class], version = 1, exportSchema = true)
abstract class Db : RoomDatabase() {
    abstract fun dao(): Dao0

    companion object {
        fun create(ctx: Context) = Room.databaseBuilder(ctx, Db::class.java, "pumpling.db").build()
    }
}

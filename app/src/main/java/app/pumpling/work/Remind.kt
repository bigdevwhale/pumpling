package app.pumpling.work

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.pumpling.MainActivity
import app.pumpling.R
import app.pumpling.container
import app.pumpling.data.Settings
import app.pumpling.domain.dayOf
import app.pumpling.domain.today
import app.pumpling.domain.tr
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * A quiet reminder on the days the user chose: one notification at the chosen time, and only if there
 * was no workout that day, none is going on and the user isn't on sick leave. Nothing nags twice.
 */
object Remind {
    private const val CH = "remind"; private const val ID = 9

    /** The next moment after [now] that falls on one of [days] (1 Monday .. 7 Sunday) at [at] minutes past midnight. */
    fun next(days: Set<Int>, at: Int, now: ZonedDateTime): ZonedDateTime? {
        if (days.none { it in 1..7 }) return null
        for (i in 0..7) {
            val t = now.toLocalDate().plusDays(i.toLong()).atTime(at.coerceIn(0, 1439) / 60, at.coerceIn(0, 1439) % 60).atZone(now.zone)
            if (t.dayOfWeek.value in days && t.isAfter(now)) return t
        }
        return null
    }

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, 2, Intent(ctx, RemindAlarm::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Sets the alarm for the next reminder, or clears it when there are no days. Safe to call any number of times. */
    fun schedule(ctx: Context, s: Settings) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(ctx))
        val t = next(s.remindDays, s.remindAt, ZonedDateTime.now()) ?: return
        // a few minutes either way don't matter here, so no exact alarm is asked for
        runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.toInstant().toEpochMilli(), pending(ctx)) }
    }

    fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** [leader]: the leading pet's name, when there is one out of the egg. */
    fun show(ctx: Context, leader: String?) {
        if (!canPost(ctx)) return
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CH, tr("Напоминание о тренировке", "Workout reminder"), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_stat).setContentTitle(tr("Сегодня день тренировки", "It's a training day"))
            .setContentText(if (leader != null) tr("$leader уже разминается. Загляните в зал, когда будет время.", "$leader is warming up already. Drop by the gym when you can.")
                else tr("Команда ждёт в зале. Загляните, когда будет время.", "The team is waiting in the gym. Drop by when you can."))
            .setAutoCancel(true).setContentIntent(open).build()
        try { NotificationManagerCompat.from(ctx).notify(ID, n) } catch (_: SecurityException) { }
    }

    fun clear(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(ID)
}

/** The reminder's time has come: show it if today still needs one, and set the next. */
class RemindAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wait = goAsync(); val c = context.container
        c.scope.launch {
            try {
                val s = c.settings.now()
                Remind.schedule(context, s)
                val t = today()
                val due = java.time.LocalDate.ofEpochDay(t).dayOfWeek.value in s.remindDays && s.sick == 0L
                if (due && c.dao.liveNow() == null && c.dao.workoutList().none { it.end > 0 && dayOf(it.start) == t })
                    Remind.show(context, c.dao.petList().firstOrNull { it.slot == 0 && it.hatched > 0 }?.name)
            } finally { wait.finish() }
        }
    }
}

/** Alarms don't survive a restart, an update of the app or a change of the clock: the reminder is set again. */
class RemindBoot : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wait = goAsync(); val c = context.container
        c.scope.launch { try { Remind.schedule(context, c.settings.now()) } finally { wait.finish() } }
    }
}

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
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.pumpling.MainActivity
import app.pumpling.R
import app.pumpling.domain.tr

/**
 * The rest between sets. The count-down shows in the app and in a notification with a
 * chronometer; an alarm rings and buzzes the phone when the rest is over, even with the app closed.
 * The alarm is exact where the phone allows it (see [exact]); otherwise it may come late, and on some phones
 * only when the app is opened again.
 */
object Rest {
    /** When the rest ends, System.currentTimeMillis; 0 when not resting. */
    var until by mutableLongStateOf(0L)
    var length by mutableLongStateOf(0L)
    /** The app is on the screen: the end of a rest sounds by itself, without a notification. */
    @Volatile var shown = false
    private const val CH = "rest"; private const val CH_OVER = "rest_over"; private const val ID = 7; private const val ID_OVER = 8
    private const val PREFS = "rest"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** [sound]: ring when the rest is over, not only buzz. */
    fun start(ctx: Context, secs: Int, sound: Boolean = true) {
        if (secs <= 0) return
        length = secs * 1000L; until = System.currentTimeMillis() + length
        prefs(ctx).edit().putLong("until", until).putLong("length", length).putBoolean("sound", sound).apply()
        NotificationManagerCompat.from(ctx).cancel(ID_OVER)
        schedule(ctx); notify(ctx)
    }
    fun add(ctx: Context, secs: Int) {
        if (until == 0L) return
        until = maxOf(System.currentTimeMillis() + 1000, until + secs * 1000L); length += secs * 1000L
        prefs(ctx).edit().putLong("until", until).putLong("length", length).apply()
        schedule(ctx); notify(ctx)
    }
    fun stop(ctx: Context) { until = 0; prefs(ctx).edit().remove("until").apply(); cancelAlarm(ctx); NotificationManagerCompat.from(ctx).apply { cancel(ID); cancel(ID_OVER) } }

    /** After the process was restarted in the middle of a rest: the count-down goes on from where it was. */
    fun restore(ctx: Context) {
        val p = prefs(ctx); val u = p.getLong("until", 0)
        if (u > System.currentTimeMillis()) { until = u; length = p.getLong("length", 0) }
    }

    /** The app came to the screen: the «rest is over» notification has done its job. */
    fun seen(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(ID_OVER)

    /** Whether the end of a rest comes to the second. Android 12+ may hold the permission back; see [exactSettings]. */
    fun exact(ctx: Context) = Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    /** The system page where «Alarms & reminders» is allowed for the app (Android 12+). */
    fun exactSettings(ctx: Context): Intent? = if (Build.VERSION.SDK_INT < 31) null else
        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, RestAlarm::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val at = SystemClock.elapsedRealtime() + (until - System.currentTimeMillis())
        // An alarm-clock alarm, not just an exact one: Xiaomi's HyperOS freezes an app seconds after it leaves the screen
        // and holds its other alarms back until it is opened again («GreezeManager: cached alarm»). This kind wakes it.
        // The phone shows its alarm icon in the status bar while the rest runs.
        runCatching { if (exact(ctx)) am.setAlarmClock(AlarmManager.AlarmClockInfo(until, open(ctx)), pending(ctx))
            else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(ctx)) }
            .onFailure { runCatching { am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(ctx)) } }
    }
    private fun cancelAlarm(ctx: Context) = ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx))

    fun channel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, tr("Отдых между подходами", "Rest between sets"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        // the phone buzzes from [done] itself, so the channel only rings
        nm.createNotificationChannel(NotificationChannel(CH_OVER, tr("Отдых окончен", "Rest is over"), NotificationManager.IMPORTANCE_HIGH).apply { setShowBadge(false); enableVibration(false) })
    }
    private fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    private fun open(ctx: Context) = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)

    private fun notify(ctx: Context) {
        if (!canPost(ctx)) return
        channel(ctx)
        val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_stat).setContentTitle(tr("Отдых", "Resting"))
            .setContentText(tr("Питомец пьёт воду", "Your pet sips some water")).setUsesChronometer(true).setChronometerCountDown(true).setWhen(until)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open(ctx)).setSilent(true).build()
        try { NotificationManagerCompat.from(ctx).notify(ID, n) } catch (_: SecurityException) { }
    }

    /**
     * The rest is over: called by the alarm, or by the workout screen when it sees the time first.
     * Whoever comes first cancels the other, so the phone buzzes once.
     */
    fun done(ctx: Context) {
        val p = prefs(ctx)
        // the alarm of a rest already ended (or stopped) by the screen
        if (until == 0L && p.getLong("until", 0) == 0L) return
        until = 0; p.edit().remove("until").apply()
        cancelAlarm(ctx)
        NotificationManagerCompat.from(ctx).cancel(ID)
        val v = ctx.getSystemService(Vibrator::class.java)
        runCatching { v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1)) }
        if (!p.getBoolean("sound", true)) return
        if (!shown && canPost(ctx)) {
            channel(ctx)
            val n = NotificationCompat.Builder(ctx, CH_OVER).setSmallIcon(R.drawable.ic_stat).setContentTitle(tr("Отдых окончен", "Rest is over"))
                .setContentText(tr("Питомец ждёт следующий подход", "Your pet is waiting for the next set")).setAutoCancel(true).setContentIntent(open(ctx))
                .setCategory(NotificationCompat.CATEGORY_ALARM).setTimeoutAfter(120_000).build()
            try { NotificationManagerCompat.from(ctx).notify(ID_OVER, n); return } catch (_: SecurityException) { }
        }
        // on the screen (or with notifications off): the notification sound, played directly
        runCatching {
            RingtoneManager.getRingtone(ctx.applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.apply {
                audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                play()
            }
        }
    }
}

class RestAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Rest.done(context)
}

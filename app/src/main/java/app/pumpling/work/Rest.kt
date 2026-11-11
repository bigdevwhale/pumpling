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
 * chronometer; an alarm buzzes the phone when the rest is over, even with the app closed.
 */
object Rest {
    /** When the rest ends, System.currentTimeMillis; 0 when not resting. */
    var until by mutableLongStateOf(0L)
    var length by mutableLongStateOf(0L)
    private const val CH = "rest"; private const val ID = 7

    fun start(ctx: Context, secs: Int) {
        length = secs * 1000L; until = System.currentTimeMillis() + length
        schedule(ctx); notify(ctx)
    }
    fun add(ctx: Context, secs: Int) { if (until == 0L) return; until = maxOf(System.currentTimeMillis() + 1000, until + secs * 1000L); length += secs * 1000L; schedule(ctx); notify(ctx) }
    fun stop(ctx: Context) { until = 0; cancelAlarm(ctx); NotificationManagerCompat.from(ctx).cancel(ID) }

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, RestAlarm::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val at = SystemClock.elapsedRealtime() + (until - System.currentTimeMillis())
        runCatching { if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(ctx))
            else am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(ctx)) }
    }
    private fun cancelAlarm(ctx: Context) = ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx))

    fun channel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, tr("Отдых между подходами", "Rest between sets"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
    }
    private fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notify(ctx: Context) {
        if (!canPost(ctx)) return
        channel(ctx)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_stat).setContentTitle(tr("Отдых", "Resting"))
            .setContentText(tr("Питомец пьёт воду", "Your pet sips some water")).setUsesChronometer(true).setChronometerCountDown(true).setWhen(until)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open).setSilent(true).build()
        try { NotificationManagerCompat.from(ctx).notify(ID, n) } catch (_: SecurityException) { }
    }

    fun done(ctx: Context) {
        until = 0
        NotificationManagerCompat.from(ctx).cancel(ID)
        val v = ctx.getSystemService(Vibrator::class.java)
        runCatching { v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1)) }
    }
}

class RestAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Rest.done(context)
}

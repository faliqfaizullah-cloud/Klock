package com.klock.app
import android.app.*
import android.content.*
import android.media.RingtoneManager
import java.util.Calendar
data class AlarmItem(val id: Int, val h: Int, val m: Int, val label: String, val on: Boolean)
object Alarms {
    fun load(c: Context) = c.getSharedPreferences("k", 0).getString("a", "")!!.split(";").filter { it.isNotBlank() }.map {
        val p = it.split("|"); AlarmItem(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3], p[4] == "1") }
    fun save(c: Context, l: List<AlarmItem>) {
        c.getSharedPreferences("k", 0).edit().putString("a", l.joinToString(";") { "${it.id}|${it.h}|${it.m}|${it.label}|${if (it.on) 1 else 0}" }).apply()
        l.forEach { set(c, it) }
    }
    private fun pi(c: Context, id: Int, label: String = "") = PendingIntent.getBroadcast(c, id,
        Intent(c, AlarmReceiver::class.java).putExtra("l", label), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun cancel(c: Context, id: Int) = c.getSystemService(AlarmManager::class.java).cancel(pi(c, id))
    fun set(c: Context, a: AlarmItem) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (!a.on) return cancel(c, a.id)
        val t = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, a.h); set(Calendar.MINUTE, a.m); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
        val p = pi(c, a.id, a.label)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(t.timeInMillis, p), p)
    }
}
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("al", "Alarms", NotificationManager.IMPORTANCE_HIGH))
        nm.notify(1, Notification.Builder(c, "al").setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(i.getStringExtra("l").orEmpty().ifBlank { "Alarm" }).setCategory(Notification.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)).build())
    }
}

package com.klock.app
import android.app.*
import android.content.*
import java.util.Calendar
data class AlarmItem(val id: Int, val h: Int, val m: Int, val label: String, val on: Boolean, val city: String = "", val days: Int = 127)
object Alarms {
    fun load(c: Context) = c.getSharedPreferences("k", 0).getString("a", "")!!.split(";").filter { it.isNotBlank() }.map {
        val p = it.split("|"); AlarmItem(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3], p[4] == "1", p.getOrElse(5) { "" }, p.getOrElse(6) { "127" }.toInt()) }
    fun save(c: Context, l: List<AlarmItem>) {
        c.getSharedPreferences("k", 0).edit().putString("a", l.joinToString(";") { "${it.id}|${it.h}|${it.m}|${it.label.replace("|", " ").replace(";", " ")}|${if (it.on) 1 else 0}|${it.city}|${it.days}" }).apply()
        l.forEach { set(c, it) }
    }
    private fun pi(c: Context, id: Int, label: String = "") = PendingIntent.getBroadcast(c, id,
        Intent(c, AlarmReceiver::class.java).putExtra("l", label).putExtra("id", id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun cancel(c: Context, id: Int) = c.getSystemService(AlarmManager::class.java).cancel(pi(c, id))
    private fun fire(c: Context, at: Long, id: Int, label: String) {
        val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        try { c.getSystemService(AlarmManager::class.java).setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi(c, id, label)) }
        catch (e: SecurityException) { android.widget.Toast.makeText(c, "Allow exact alarms for Klock in Settings", android.widget.Toast.LENGTH_LONG).show() }
    }
    fun set(c: Context, a: AlarmItem) {
        if (!a.on) return cancel(c, a.id)
        val t = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, a.h); set(Calendar.MINUTE, a.m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            for (k in 0..8) { if (timeInMillis > System.currentTimeMillis() && (a.days == 0 || ((a.days shr (get(Calendar.DAY_OF_WEEK) - 1)) and 1) == 1)) break; add(Calendar.DAY_OF_YEAR, 1) } }
        fire(c, t.timeInMillis, a.id, a.label)
    }
    fun snooze(c: Context, label: String) = fire(c, System.currentTimeMillis() + 10 * 60_000L, 888, label)
}
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", 0)
        val all = Alarms.load(c); val it0 = all.find { it.id == id }
        if (it0 != null) { if (it0.days == 0) Alarms.save(c, all.map { if (it.id == id) it.copy(on = false) else it }) else Alarms.set(c, it0) }
        c.startForegroundService(Intent(c, AlarmService::class.java).putExtra("l", i.getStringExtra("l").orEmpty()))
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { Alarms.load(c).forEach { Alarms.set(c, it) } }
}

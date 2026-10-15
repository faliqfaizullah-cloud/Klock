package com.klock.app
import android.app.*
import android.appwidget.*
import android.content.*
import android.graphics.*
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar

data class WCfg(val hue: Float = 8f, val style: Int = 0, val dial: Boolean = true, val moon: Boolean = true)
class Pal(val bg: Int, val ink: Int, val dim: Int, val acc: Int)
object WStore {
    fun get(c: Context) = c.getSharedPreferences("w", 0).let { WCfg(it.getFloat("h", 8f), it.getInt("s", 0), it.getBoolean("d", true), it.getBoolean("m", true)) }
    fun set(c: Context, w: WCfg) { c.getSharedPreferences("w", 0).edit().putFloat("h", w.hue).putInt("s", w.style).putBoolean("d", w.dial).putBoolean("m", w.moon).apply() }
}
fun palOf(w: WCfg): Pal { val a = Color.HSVToColor(floatArrayOf(w.hue, .85f, 1f))
    return when (w.style) { 2 -> Pal(0xFFFBFBFC.toInt(), 0xFF1C1C1E.toInt(), 0xFFC9C9D3.toInt(), a); 1 -> Pal(0xFF0E0E10.toInt(), a, 0xFF4A4A52.toInt(), a); else -> Pal(0xFF0E0E10.toInt(), 0xFFFFFFFF.toInt(), 0xFF4A4A52.toInt(), a) } }
fun moonFrac() = ((((System.currentTimeMillis() / 86400000.0) - 10957.76) % 29.530588 + 29.530588) % 29.530588) / 29.530588

fun faceBitmap(c: Context, wd: Int, hd: Int, cfg: WCfg): Bitmap {
    val d = c.resources.displayMetrics; val W = (wd * d.density).toInt(); val H = (hd * d.density).toInt(); val w = W.toFloat(); val h = H.toFloat()
    val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val cv = Canvas(bmp); val pl = palOf(cfg); cv.drawColor(pl.bg)
    val p = Paint(Paint.ANTI_ALIAS_FLAG); val h24 = DateFormat.is24HourFormat(c); val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); p.textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 32f, d); p.textAlign = Paint.Align.CENTER; p.color = pl.dim
    if (cfg.style != 1) { val base = 27 * d.density - p.fontMetrics.ascent
        for (x in listOf(-2, -1, 1, 2)) { val hh = (hr + x + 24) % 24; val cx = w / 2 + x * w * 0.36f; val cy = base + x * x * w * 0.03f
            cv.save(); cv.rotate(x * 14f, cx, cy); cv.drawText(two(if (h24) hh else (hh + 11) % 12 + 1), cx, cy, p); cv.restore() } }
    if (cfg.dial) {
        p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = w * 0.014f; val py = h + w * 0.25f
        for (k in -5..5) { val a = Math.toRadians(k * 11.0); val s = Math.sin(a).toFloat(); val co = Math.cos(a).toFloat(); p.color = if (k == 0) pl.acc else pl.dim
            cv.drawLine(w / 2 + s * w * 0.42f, py - co * w * 0.42f, w / 2 + s * w * 0.5f, py - co * w * 0.5f, p) }
        p.color = pl.acc; p.style = Paint.Style.FILL; cv.drawCircle(w / 2, h - w * 0.115f, w * 0.02f, p)
        cv.drawPath(Path().apply { moveTo(w / 2 - w * 0.015f, h); lineTo(w / 2 + w * 0.015f, h); lineTo(w / 2, h - w * 0.09f); close() }, p)
    }
    if (cfg.moon) {
        val r = w * 0.085f; val mx = w * 0.85f; val my = h * 0.16f; val f = moonFrac(); p.style = Paint.Style.FILL
        p.color = 0xFFD9D9DF.toInt(); cv.drawCircle(mx, my, r, p)
        val sh = if (f < .5) -2 * r * (f * 2).toFloat() else 2 * r * (1 - (f - .5) * 2).toFloat()
        p.color = pl.bg; cv.drawCircle(mx + sh, my, r, p)
    }
    return bmp
}

class ClockWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, o: android.os.Bundle) = onUpdate(c, m, intArrayOf(id))
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        val cfg = WStore.get(c); val pl = palOf(cfg)
        ids.forEach { id ->
            val o = m.getAppWidgetOptions(id)
            val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160).coerceAtLeast(60); val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 176).coerceAtLeast(60)
            val v = RemoteViews(c.packageName, R.layout.widget_clock)
            v.setImageViewBitmap(R.id.dial, faceBitmap(c, w, h, cfg))
            v.setViewVisibility(R.id.hourClk, if (cfg.style == 1) View.GONE else View.VISIBLE); v.setViewVisibility(R.id.timeClk, if (cfg.style == 1) View.VISIBLE else View.GONE)
            v.setTextColor(R.id.hourClk, pl.ink); v.setTextColor(R.id.timeClk, pl.ink)
            v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            m.updateAppWidget(id, v)
        }
        val t = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 5) }
        c.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, t.timeInMillis,
            PendingIntent.getBroadcast(c, 9, Intent(c, ClockWidget::class.java).setAction("com.klock.TICK"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    }
    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        if (i.action in listOf("com.klock.TICK", Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED, Intent.ACTION_BOOT_COMPLETED)) {
            val m = AppWidgetManager.getInstance(c); onUpdate(c, m, m.getAppWidgetIds(ComponentName(c, ClockWidget::class.java)))
        }
    }
}

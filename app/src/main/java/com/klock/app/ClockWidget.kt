package com.klock.app
import android.app.*
import android.appwidget.*
import android.content.*
import android.content.res.Configuration
import android.graphics.*
import android.text.format.DateFormat
import android.widget.RemoteViews
import java.util.Calendar
class ClockWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, o: android.os.Bundle) = onUpdate(c, m, intArrayOf(id))
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val o = m.getAppWidgetOptions(id)
            val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160).coerceAtLeast(60); val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 176).coerceAtLeast(60)
            val v = RemoteViews(c.packageName, R.layout.widget_clock)
            v.setImageViewBitmap(R.id.dial, dial(c, w, h))
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
    private fun dial(c: Context, wd: Int, hd: Int): Bitmap {
        val d = c.resources.displayMetrics; val W = (wd * d.density).toInt(); val H = (hd * d.density).toInt(); val w = W.toFloat(); val h = H.toFloat()
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val cv = Canvas(bmp)
        val night = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val ink = if (night) 0xFFFFFFFF.toInt() else 0xFF1C1C1E.toInt(); val dim = if (night) 0xFF5A5A64.toInt() else 0xFFC9C9D3.toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG); val h24 = DateFormat.is24HourFormat(c); val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); p.textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 32f, d)
        p.textAlign = Paint.Align.CENTER; p.color = dim
        val base = 27 * d.density - p.fontMetrics.ascent
        for (x in listOf(-2, -1, 1, 2)) {
            val hh = (hr + x + 24) % 24; val cx = w / 2 + x * w * 0.36f; val cy = base + x * x * w * 0.03f
            cv.save(); cv.rotate(x * 14f, cx, cy); cv.drawText(two(if (h24) hh else (hh + 11) % 12 + 1), cx, cy, p); cv.restore()
        }
        p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = w * 0.014f; val py = h + w * 0.25f
        for (k in -5..5) {
            val a = Math.toRadians(k * 11.0); val s = Math.sin(a).toFloat(); val co = Math.cos(a).toFloat(); p.color = if (k == 0) ink else dim
            cv.drawLine(w / 2 + s * w * 0.42f, py - co * w * 0.42f, w / 2 + s * w * 0.5f, py - co * w * 0.5f, p)
        }
        p.color = ink; p.style = Paint.Style.FILL; cv.drawCircle(w / 2, h - w * 0.115f, w * 0.02f, p)
        cv.drawPath(Path().apply { moveTo(w / 2 - w * 0.015f, h); lineTo(w / 2 + w * 0.015f, h); lineTo(w / 2, h - w * 0.09f); close() }, p)
        return bmp
    }
}

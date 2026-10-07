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
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        val v = RemoteViews(c.packageName, R.layout.widget_clock)
        v.setImageViewBitmap(R.id.dial, dial(c))
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        ids.forEach { m.updateAppWidget(it, v) }
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
    private fun dial(c: Context): Bitmap {
        val S = (150 * c.resources.displayMetrics.density).toInt(); val f = S.toFloat()
        val bmp = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888); val cv = Canvas(bmp)
        val night = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bg = if (night) 0xFF101012.toInt() else 0xFFFBFBFC.toInt(); val ink = if (night) 0xFFFFFFFF.toInt() else 0xFF1C1C1E.toInt(); val dim = if (night) 0xFF5A5A64.toInt() else 0xFFC9C9D3.toInt()
        val rr = f * 28f / 110f
        cv.clipPath(Path().apply { addRoundRect(0f, 0f, f, f, rr, rr, Path.Direction.CW) }); cv.drawColor(bg)
        val p = Paint(Paint.ANTI_ALIAS_FLAG); val h24 = DateFormat.is24HourFormat(c); val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); p.textSize = f * 0.29f; p.textAlign = Paint.Align.CENTER; p.color = dim
        for (x in listOf(-2, -1, 1, 2)) {
            val hh = (hr + x + 24) % 24; val cx = f / 2 + x * f * 0.36f; val cy = f * 0.52f + x * x * f * 0.025f
            cv.save(); cv.rotate(x * 14f, cx, cy); cv.drawText(two(if (h24) hh else (hh + 11) % 12 + 1), cx, cy, p); cv.restore()
        }
        p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = f * 0.014f
        for (k in -5..5) {
            val a = Math.toRadians(k * 11.0); val s = Math.sin(a).toFloat(); val co = Math.cos(a).toFloat(); p.color = if (k == 0) ink else dim
            cv.drawLine(f / 2 + s * f * 0.42f, f * 1.25f - co * f * 0.42f, f / 2 + s * f * 0.5f, f * 1.25f - co * f * 0.5f, p)
        }
        p.color = ink; p.style = Paint.Style.FILL; cv.drawCircle(f / 2, f * 0.885f, f * 0.02f, p)
        cv.drawPath(Path().apply { moveTo(f / 2 - f * 0.015f, f); lineTo(f / 2 + f * 0.015f, f); lineTo(f / 2, f * 0.91f); close() }, p)
        return bmp
    }
}

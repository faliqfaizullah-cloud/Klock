package com.klock.app
import android.app.*
import android.appwidget.*
import android.content.*
import android.graphics.*
import android.text.format.DateFormat
import android.widget.RemoteViews
import java.util.Calendar

data class WCfg(val hue: Float = 100f, val style: Int = 0, val dial: Boolean = true, val moon: Boolean = true, val sec: Boolean = true)
class Pal(val dark: Boolean, val bgTop: Int, val bgBot: Int, val tick: Int, val num: Int, val hand: Int, val ink: Int, val dim: Int, val pill: Int, val acc: Int)
object WStore {
    fun get(c: Context) = c.getSharedPreferences("w", 0).let { WCfg(it.getFloat("h2", 100f), it.getInt("s2", 0), it.getBoolean("d2", true), it.getBoolean("m2", true), it.getBoolean("x2", true)) }
    fun set(c: Context, w: WCfg) { c.getSharedPreferences("w", 0).edit().putFloat("h2", w.hue).putInt("s2", w.style).putBoolean("d2", w.dial).putBoolean("m2", w.moon).putBoolean("x2", w.sec).apply() }
}
fun palOf(w: WCfg): Pal {
    val acc = Color.HSVToColor(floatArrayOf(w.hue, .7f, .82f))
    val dark = when (w.style) { 1 -> false; 2 -> true; else -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 12 }
    return if (dark) Pal(true, 0xFF222224.toInt(), 0xFF111112.toInt(), 0xFF55555A.toInt(), 0xFF8A8A90.toInt(), 0xFFEDEDED.toInt(), 0xFFFFFFFF.toInt(), 0xFF8A8A90.toInt(), 0xFF2C2C2E.toInt(), acc)
    else Pal(false, 0xFFF9F9FB.toInt(), 0xFFE6E6EB.toInt(), 0xFFB9B9C0.toInt(), 0xFFA0A0A8.toInt(), 0xFFFFFFFF.toInt(), 0xFF1C1C1E.toInt(), 0xFFA0A0A8.toInt(), 0xFFEDEDF1.toInt(), acc)
}

fun faceBitmap(c: Context, wd: Int, hd: Int, cfg: WCfg): Bitmap {
    val d = c.resources.displayMetrics; val W = (wd * d.density).toInt(); val H = (hd * d.density).toInt(); val w = W.toFloat(); val h = H.toFloat(); val S = Math.min(w, h)
    val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val cv = Canvas(bmp); val pl = palOf(cfg); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.shader = LinearGradient(0f, 0f, 0f, h, pl.bgTop, pl.bgBot, Shader.TileMode.CLAMP); cv.drawRect(0f, 0f, w, h, p); p.shader = null
    val cal = Calendar.getInstance(); val hr = cal.get(Calendar.HOUR_OF_DAY) % 12 + cal.get(Calendar.MINUTE) / 60f
    val ha = Math.toRadians(hr * 30.0 - 90.0); val dx = Math.cos(ha).toFloat(); val dy = Math.sin(ha).toFloat()
    val px = w / 2 - dx * S * .25f; val py = h / 2 - dy * S * .25f
    fun pt(deg: Double, r: Float, ox: Float = px, oy: Float = py): PointF { val a = Math.toRadians(deg - 90.0); return PointF(ox + Math.cos(a).toFloat() * r, oy + Math.sin(a).toFloat() * r) }
    p.strokeCap = Paint.Cap.ROUND
    if (cfg.dial) {
        p.style = Paint.Style.STROKE; p.strokeWidth = S * .003f; p.color = if (pl.dark) 0x22FFFFFF else 0x44C9B98A
        for (k in 0 until 12) { val e = pt(k * 30.0, S * .6f); cv.drawLine(px, py, e.x, e.y, p) }
        p.strokeWidth = S * .008f; p.color = pl.tick
        for (k in 0 until 60) { if (k % 5 == 0) continue; val a = pt(k * 6.0, S * .5f); val b = pt(k * 6.0, S * .55f); cv.drawLine(a.x, a.y, b.x, b.y, p) }
        p.style = Paint.Style.FILL; p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); p.textSize = S * .055f; p.textAlign = Paint.Align.CENTER; p.color = pl.num
        for (n in 1..12) { val a = n * 30.0; val q = pt(a, S * .66f); val rot = if (n == 12 || n == 6) 0f else if (n < 6) a.toFloat() else a.toFloat() - 180f
            cv.save(); cv.rotate(rot, q.x, q.y); cv.drawText(n.toString(), q.x, q.y + p.textSize * .35f, p); cv.restore() }
    }
    if (cfg.moon) { p.style = Paint.Style.STROKE; p.strokeWidth = S * .016f; p.color = pl.acc
        for (k in 0 until 12) { val a = pt(k * 30.0, S * .42f); val b = pt(k * 30.0, S * .545f); cv.drawLine(a.x, a.y, b.x, b.y, p) } }
    val tip = PointF(px + dx * S * .33f, py + dy * S * .33f)
    p.style = Paint.Style.STROKE; p.strokeWidth = S * .03f; p.color = pl.hand; p.setShadowLayer(S * .025f, 0f, S * .012f, 0x44000000); cv.drawLine(px, py, tip.x, tip.y, p); p.clearShadowLayer()
    if (cfg.moon) { p.strokeWidth = S * .014f; p.color = pl.acc; cv.drawLine(px + dx * S * .12f, py + dy * S * .12f, px + dx * S * .27f, py + dy * S * .27f, p) }
    p.style = Paint.Style.FILL; p.color = pl.hand; p.setShadowLayer(S * .02f, 0f, S * .01f, 0x44000000); cv.drawCircle(px, py, S * .04f, p); p.clearShadowLayer()
    val bx = w - S * .167f; val by = S * .167f; p.color = if (pl.dark) 0xFF2C2C2E.toInt() else 0xFFF1F1F4.toInt(); cv.drawCircle(bx, by, S * .067f, p)
    val letter = if (DateFormat.is24HourFormat(c)) "SMTWTFS"[cal.get(Calendar.DAY_OF_WEEK) - 1].toString() else if (cal.get(Calendar.AM_PM) == Calendar.AM) "A" else "P"
    p.color = pl.num; p.textSize = S * .07f; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); cv.drawText(letter, bx, by + p.textSize * .35f, p)
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
            v.setInt(R.id.pill, "setBackgroundResource", if (pl.dark) R.drawable.pill_dark else R.drawable.pill_light)
            v.setTextColor(R.id.hourClk, pl.dim); v.setTextColor(R.id.minClk, pl.ink); v.setTextColor(R.id.secClk, pl.dim)
            v.setViewVisibility(R.id.secClk, if (cfg.sec) android.view.View.VISIBLE else android.view.View.GONE)
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

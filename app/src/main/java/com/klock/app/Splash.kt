package com.klock.app
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.*

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(x: Float) = x * x * (3 - 2 * x)
private val GOLD = Color(0xFFE6C77A)

@Composable fun Splash(onDone: () -> Unit) {
    val hp = LocalHapticFeedback.current; val prog = remember { Animatable(0f) }; val now = remember { LocalTime.now() }
    val blocks = remember { val r = java.util.Random(11); List(260) { floatArrayOf(r.nextFloat() * 1.4f - .7f, r.nextFloat() * 1.4f - .7f, .02f + r.nextFloat() * .05f, .02f + r.nextFloat() * .05f, .22f + r.nextFloat() * .3f) } }
    LaunchedEffect(Unit) {
        launch { delay(550); hp.performHapticFeedback(HapticFeedbackType.LongPress); delay(2500); hp.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        prog.animateTo(1f, tween(4800, easing = LinearEasing)); onDone()
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF0A1016)).clickable(remember { MutableInteractionSource() }, null) { onDone() }) {
        Canvas(Modifier.fillMaxSize()) {
            val t = prog.value; val w = size.width; val h = size.height; val c = Offset(w / 2, h * 0.46f); val R = min(w, h) * 0.36f; val S = max(w, h)
            val zoom = 1f + 1.4f * ease(seg(t, .42f, .8f)); val aer = 1f - ease(seg(t, .5f, .78f)); val hud = seg(t, .1f, .3f) * (1f - ease(seg(t, .58f, .76f)))
            val land = ease(seg(t, .3f, .72f)); val el = now.second + t * 4.8f
            val hAng = ((now.hour % 12) + now.minute / 60f) * 30f; val mAng = (now.minute + el / 60f) * 6f; val sAng = el * 6f
            fun hand(a: Float, len: Float, wd: Float, col: Color, tail: Float = 0f) { val r = Math.toRadians(a - 90.0); val dx = cos(r).toFloat(); val dy = sin(r).toFloat()
                drawLine(col, Offset(c.x - dx * tail, c.y - dy * tail), Offset(c.x + dx * len, c.y + dy * len), wd, StrokeCap.Round) }
            fun txt(s: String, x: Float, y: Float, size: Float, col: Color) = drawIntoCanvas { cv ->
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col.toArgb(); textSize = size; textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
                cv.nativeCanvas.drawText(s, x, y + size * 0.35f, p) }
            withTransform({ scale(zoom, zoom, c) }) {
                if (aer > 0f) {
                    drawRect(Color(0xFF2A2E32).copy(alpha = aer))
                    for (b in blocks) drawRect(Color(b[4], b[4], b[4] + .03f, aer), Offset(c.x + b[0] * S, c.y + b[1] * S), Size(b[2] * S, b[3] * S))
                    for (k in 0 until 12) { val a = k * Math.PI.toFloat() / 6f; drawLine(Color(0xFF12161A).copy(alpha = aer), c, Offset(c.x + cos(a) * S, c.y + sin(a) * S), 7.dp.toPx()) }
                    drawCircle(Color(0xFF12161A).copy(alpha = aer), S * 0.11f, c, style = Stroke(5.dp.toPx())); drawCircle(Color(0xFF12161A).copy(alpha = aer), S * 0.24f, c, style = Stroke(5.dp.toPx()))
                    drawCircle(Color(0xFFD9D4C7).copy(alpha = aer), 10.dp.toPx(), c)
                }
                if (hud > 0f) {
                    for (k in 0 until 24) { val a = k * Math.PI.toFloat() / 12f; drawLine(GOLD.copy(alpha = .22f * hud), Offset(c.x + cos(a) * R * .55f, c.y + sin(a) * R * .55f), Offset(c.x + cos(a) * S * .55f, c.y + sin(a) * S * .55f), 1.5.dp.toPx()) }
                    for (i in 0..3) { val e = ease(seg(t, .1f + i * .04f, .55f)); val rad = R * (.4f + i * .3f) * (.35f + .65f * e); val sw = (1.5f + i).dp.toPx()
                        drawCircle(GOLD.copy(alpha = hud * .35f), rad, c, style = Stroke(1.dp.toPx()))
                        drawArc(GOLD.copy(alpha = hud * .9f), t * 300f * (if (i % 2 == 0) 1 else -1) + i * 70f, 110f + i * 30f, false, Offset(c.x - rad, c.y - rad), Size(rad * 2, rad * 2), style = Stroke(sw, cap = StrokeCap.Round)) }
                    val na = seg(t, .25f, .42f) * (1f - ease(seg(t, .6f, .76f))); val nc = Color.White.copy(alpha = na); val ts = 20.dp.toPx()
                    txt("12", c.x, c.y - R * .95f, ts, nc); txt("3", c.x + R * .95f, c.y, ts, nc); txt("6", c.x, c.y + R * .95f, ts, nc); txt("9", c.x - R * .95f, c.y, ts, nc)
                    hand(hAng - (1 - land) * 180f, R * .5f, 5.dp.toPx(), Color.White.copy(alpha = hud * .9f)); hand(mAng - (1 - land) * 720f, R * .8f, 3.dp.toPx(), Color.White.copy(alpha = hud * .9f))
                }
            }
            val nv = 1f - aer
            for (k in 0 until 12) { val a = k * Math.PI.toFloat() / 6f; drawLine(Color.White.copy(alpha = .06f * nv), c, Offset(c.x + cos(a) * S, c.y + sin(a) * S), 1.dp.toPx()) }
            for (i in 1..4) drawCircle(Color.White.copy(alpha = .06f * nv), S * .12f * i, c, style = Stroke(1.dp.toPx()))
            val ca = ease(seg(t, .7f, .9f))
            if (ca > 0f) {
                for (k in 0 until 60) { val r = Math.toRadians(k * 6.0); val long = k % 5 == 0; val i0 = R * (if (long) .9f else .94f); val s = sin(r).toFloat(); val co = cos(r).toFloat()
                    drawLine(Color.White.copy(alpha = ca * (if (long) .95f else .5f)), Offset(c.x + s * i0, c.y - co * i0), Offset(c.x + s * R * .98f, c.y - co * R * .98f), (if (long) 2.5f else 1f).dp.toPx(), StrokeCap.Round) }
                for (n in 1..12) { val r = Math.toRadians(n * 30.0); txt(n.toString(), c.x + sin(r).toFloat() * R * .76f, c.y - cos(r).toFloat() * R * .76f, 20.dp.toPx(), Color.White.copy(alpha = ca)) }
                hand(hAng, R * .5f, 6.dp.toPx(), Color.White.copy(alpha = ca)); hand(mAng, R * .74f, 4.dp.toPx(), Color.White.copy(alpha = ca))
                hand(sAng, R * .86f, 1.5f.dp.toPx(), GOLD.copy(alpha = ca), R * .14f); drawCircle(Color.White.copy(alpha = ca), 7.dp.toPx(), c)
                txt("Klock", c.x, c.y + R * 1.5f, 30.dp.toPx(), Color.White.copy(alpha = seg(t, .85f, .97f)))
            }
        }
    }
}

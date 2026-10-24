@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.klock.app
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

fun zid(t: String): ZoneId = try { ZoneId.of(t) } catch (e: Exception) { ZoneId.systemDefault() }
object Atlas {
    fun load(c: Context) { try {
        val l = c.assets.open("capitals.txt").bufferedReader().readLines().filter { it.isNotBlank() }.map { val p = it.split("|"); City(p[0], p[1], p[2], p[3].toDouble(), p[4].toDouble(), p[5].toFloat()) }
        if (l.isNotEmpty()) { CITIES = l; val id = ZoneId.systemDefault().id
            LOCAL = l.firstOrNull { it.tz == id } ?: City(id.substringAfter('/').replace('_', ' '), id.substringBefore('/', ""), id, Double.NaN, Double.NaN)
            ALL = if (LOCAL in l) l else listOf(LOCAL) + l }
    } catch (e: Exception) { } }
}
object World { var rows: List<String> = emptyList() }

@Composable fun GlobeTab2(add: () -> Unit, go: () -> Unit) {
    var map by remember { mutableStateOf(true) }; val d = dk()
    Box(Modifier.fillMaxSize()) {
        if (map) ClockMap(add) else GlobeTab(go)
        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(20.dp, 28.dp)) { Seg(listOf("List", "Map"), if (map) 1 else 0, d) { map = it == 1 } }
    }
}

@Composable fun ClockMap(add: () -> Unit) {
    val c = LocalContext.current; val d = dk(); val fg = fgC(d); val bgc = bgC(d); val hp = LocalHapticFeedback.current
    val rows = remember { if (World.rows.isEmpty()) World.rows = c.assets.open("land.txt").bufferedReader().readLines(); World.rows }
    val city = cityOf(S.cities[S.sel.coerceIn(0, S.cities.size - 1)])
    var now by remember { mutableStateOf(ZonedDateTime.now()) }; LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(); delay(1000) } }
    val t = now.withZoneSameInstant(zid(city.tz)); val off = t.offset.totalSeconds / 3600f
    val tLon = if (city.lon.isNaN()) off * 15f else city.lon.toFloat(); val tLat = if (city.lat.isNaN()) 20f else city.lat.toFloat()
    var baseLon by remember { mutableStateOf(tLon) }
    LaunchedEffect(city) { var x = tLon; while (x - baseLon > 180) x -= 360; while (baseLon - x > 180) x += 360; baseLon = x }
    val cx by animateFloatAsState(baseLon, spring(0.9f, 60f), label = "lon"); val cy by animateFloatAsState(tLat.coerceIn(-45f, 62f), spring(0.9f, 60f), label = "lat")
    val ax by animateFloatAsState(off, spring(0.9f, 80f), label = "ax"); var drag by remember { mutableStateOf(0f) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 92.dp).pointerInput(S.cities.size) {
        detectHorizontalDragGestures(onDragEnd = { drag = 0f }) { _, dx -> drag += dx
            if (abs(drag) > 90.dp.toPx()) { val n = S.cities.size; S.sel = (S.sel + (if (drag < 0) 1 else n - 1)) % n; drag = 0f; S.save(); hp.performHapticFeedback(HapticFeedbackType.TextHandleMove) } } }) {
        Text("World Time", fontSize = 36.sp, color = fg, modifier = Modifier.padding(20.dp, 24.dp, 20.dp, 12.dp))
        Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
            S.cities.take(6).forEachIndexed { i, n -> val on = i == S.sel
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (on) fg else cardC(d)).border(1.dp, Line, RoundedCornerShape(14.dp)).press { S.sel = i; S.save() }, Alignment.Center) { Text(n.take(1), color = if (on) bgc else fg) } }
            Spacer(Modifier.weight(1f)); Box(Modifier.size(44.dp).background(fg, CircleShape).press { add() }, Alignment.Center) { Text("+", color = bgc, fontSize = 22.sp) }
        }
        Box(Modifier.padding(top = 12.dp).weight(1f).fillMaxWidth().clipToBounds()) {
            Canvas(Modifier.fillMaxSize()) {
                val k = 4.6.dp.toPx(); val w = size.width; val h = size.height; val ox = w / 2; val oy = h * 0.5f; val st = 1.5f
                val i0 = floor((cx - ox / k + 180) / st).toInt() - 1; val i1 = ceil((cx + ox / k + 180) / st).toInt() + 1
                val j0 = floor((90 - (cy + oy / k)) / st).toInt() - 1; val j1 = ceil((90 - (cy - oy / k)) / st).toInt() + 1
                val land = if (d) Color(0xFF4A4A4C) else Color(0xFF8E8E8E); val rad = st * k * 0.34f; val rr = city.r * city.r
                for (j in max(j0, 0)..min(j1, rows.size - 1)) { val lat = 90 - (j + .5f) * st; val row = rows[j]; val cosL = cos(Math.toRadians(lat.toDouble())).toFloat()
                    for (i in i0..i1) { if (row[Math.floorMod(i, 240)] != '1') continue
                        val lon = -180 + (i + .5f) * st; val ddx = (lon - baseLon) * cosL; val ddy = lat - tLat; val hi = ddx * ddx + ddy * ddy < rr
                        drawCircle(if (hi) fg else land, if (hi) rad * 1.3f else rad, Offset(ox + (lon - cx) * k, oy + (cy - lat) * k)) } }
                drawLine(RED, Offset(ox, 0f), Offset(ox, h), 1.5.dp.toPx())
                drawCircle(RED, 6.dp.toPx(), Offset(ox, oy + (cy - tLat) * k), style = Stroke(2.dp.toPx())); drawCircle(bgc, 3.dp.toPx(), Offset(ox, oy + (cy - tLat) * k))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        BoxWithConstraints(Modifier.fillMaxWidth().height(34.dp)) {
            val wpx = constraints.maxWidth.toFloat(); val hourPx = wpx * 0.23f
            for (hr in floor(ax).toInt() - 3..ceil(ax).toInt() + 3) { val on = hr == Math.round(ax)
                Text("UTC " + (if (hr > 0) "+" else "") + hr, Modifier.offset { IntOffset((wpx / 2 + (hr - ax) * hourPx - 60.dp.toPx()).toInt(), 0) }.width(120.dp).padding(top = 8.dp),
                    textAlign = TextAlign.Center, fontSize = 12.sp, color = if (on) fg else dimC(d), fontWeight = if (on) FontWeight.Bold else FontWeight.Normal) }
        }
        Row(Modifier.padding(20.dp, 6.dp).fillMaxWidth().background(fg, RoundedCornerShape(28.dp)).padding(24.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text(utc(t), fontSize = 12.sp, color = bgc.copy(.6f)); Text(city.name, fontSize = 18.sp, color = bgc); Text(city.region, fontSize = 11.sp, color = bgc.copy(.5f)) }
            Text(hm(t.hour, t.minute), fontSize = 44.sp, letterSpacing = (-2).sp, color = bgc)
        }
    }
}

fun refreshWidgets(c: Context) { val m = AppWidgetManager.getInstance(c); val ids = m.getAppWidgetIds(ComponentName(c, ClockWidget::class.java)); if (ids.isNotEmpty()) ClockWidget().onUpdate(c, m, ids) }
val PRESETS = listOf(WCfg(100f, 0, true, true), WCfg(100f, 1, true, true), WCfg(100f, 2, true, true), WCfg(30f, 0, true, false), WCfg(200f, 2, false, true))

@Composable fun WidgetFace(cfg: WCfg, mod: Modifier) {
    val c = LocalContext.current; val pl = palOf(cfg); val now = LocalDateTime.now()
    val bmp = remember(cfg, now.hour) { faceBitmap(c, 160, 176, cfg).asImageBitmap() }
    Box(mod.clip(RoundedCornerShape(28.dp))) {
        Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Row(Modifier.align(Alignment.TopStart).padding(12.dp).background(Color(pl.pill), CircleShape).padding(10.dp, 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(hh(now.hour), color = Color(pl.dim), fontSize = 14.sp, fontFamily = FontFamily.Monospace); Text(" " + two(now.minute), color = Color(pl.ink), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Box(Modifier.padding(start = 6.dp).size(7.dp).background(Color(0xFFFF9F0A), CircleShape)) }
    }
}

@Composable fun WidgetLayer(close: () -> Unit) {
    val c = LocalContext.current; val d = dk(); val hp = LocalHapticFeedback.current; val fg = fgC(d); val card = cardC(d)
    var cfg by remember { mutableStateOf(WStore.get(c)) }; var wpx by remember { mutableStateOf(1f) }; var msg by remember { mutableStateOf("") }
    fun setHue(x: Float) { val nh = (x / wpx * 360f).coerceIn(0f, 360f); if ((nh / 12).toInt() != (cfg.hue / 12).toInt()) hp.performHapticFeedback(HapticFeedbackType.TextHandleMove); cfg = cfg.copy(hue = nh) }
    Column(Modifier.fillMaxSize().background(bgC(d)).statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(card, CircleShape).press { close() }, Alignment.Center) { Text("✕", color = fg) }
            Text("Widget", fontSize = 32.sp, color = fg, modifier = Modifier.padding(start = 16.dp)) }
        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), Alignment.Center) { WidgetFace(cfg, Modifier.size(160.dp, 176.dp)) }
        Text("Faces", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(10.dp)) { PRESETS.forEach { p ->
            Box(Modifier.size(80.dp, 88.dp).border(if (p == cfg) 2.dp else 0.dp, RED, RoundedCornerShape(16.dp)).press { cfg = p }, Alignment.Center) { WidgetFace(p, Modifier.requiredSize(160.dp, 176.dp).scale(.5f)) } } }
        Row(Modifier.padding(top = 16.dp), Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).background(card, RoundedCornerShape(24.dp)).padding(16.dp)) {
                Text("Colors", color = fg, fontWeight = FontWeight.Medium)
                Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(26.dp).onSizeChanged { wpx = it.width.toFloat() }.clip(RoundedCornerShape(13.dp))
                    .background(Brush.horizontalGradient(listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, .85f, 1f) }))
                    .pointerInput(Unit) { detectTapGestures { setHue(it.x) } }.pointerInput(Unit) { detectDragGestures { ch, _ -> setHue(ch.position.x) } }) {
                    Box(Modifier.offset { IntOffset((cfg.hue / 360f * wpx - 4.dp.toPx()).toInt(), 0) }.width(8.dp).fillMaxHeight().background(Color.White, RoundedCornerShape(4.dp))) } }
            Column(Modifier.weight(1f).background(card, RoundedCornerShape(24.dp)).padding(16.dp)) {
                Text("Style", color = fg, fontWeight = FontWeight.Medium)
                Row(Modifier.padding(top = 12.dp), Arrangement.spacedBy(8.dp)) { listOf(Color(0xFF9A9AA2), Color(0xFFF2F2F5), Color(0xFF1C1C1E)).forEachIndexed { i, col ->
                    Box(Modifier.size(38.dp).background(col, CircleShape).border(if (cfg.style == i) 2.dp else 1.dp, if (cfg.style == i) RED else Line, CircleShape).press { cfg = cfg.copy(style = i) }) } } }
        }
        Column(Modifier.padding(top = 12.dp).fillMaxWidth().background(card, RoundedCornerShape(24.dp)).padding(16.dp)) {
            Text("Elements", color = fg, fontWeight = FontWeight.Medium)
            Row(Modifier.padding(top = 12.dp), Arrangement.spacedBy(16.dp)) {
                for ((nm, on) in listOf("Dial" to cfg.dial, "Marks" to cfg.moon)) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).background(if (d) Color(0xFF2A2A2C) else Color(0xFFE4E1D6), CircleShape).press { cfg = if (nm == "Dial") cfg.copy(dial = !cfg.dial) else cfg.copy(moon = !cfg.moon) }, Alignment.Center) {
                        if (nm == "Dial") Ico2(1, fg) else Box(Modifier.width(6.dp).height(26.dp).background(Color.hsv(cfg.hue, .7f, .82f), RoundedCornerShape(3.dp)))
                        if (on) Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(10.dp).background(RED, CircleShape)) }
                    Text(nm, color = dimC(d), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) } }
        }
        Row(Modifier.padding(top = 20.dp).fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            Text("Reset", Modifier.background(card, CircleShape).press { cfg = WCfg() }.padding(22.dp, 14.dp), color = fg)
            Text("Add to home", Modifier.background(card, CircleShape).press { val m = AppWidgetManager.getInstance(c)
                if (m.isRequestPinAppWidgetSupported) m.requestPinAppWidget(ComponentName(c, ClockWidget::class.java), null, null) else { msg = "Long-press your home screen and add the Klock widget." } }.padding(22.dp, 14.dp), color = fg)
            Spacer(Modifier.weight(1f))
            Text("Save", Modifier.background(fg, CircleShape).press { WStore.set(c, cfg); refreshWidgets(c); msg = "Saved. Your widget is updated." }.padding(28.dp, 14.dp), color = bgC(d), fontWeight = FontWeight.Medium)
        }
        if (msg.isNotEmpty()) Text(msg, color = dimC(d), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 40.dp))
    }
}

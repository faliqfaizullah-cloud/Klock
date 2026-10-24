@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.klock.app
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.time.*
import java.util.Calendar
import kotlin.math.*

val GAMES = listOf("No game", "Remove all Points", "Quick Math")
val DAYN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
fun dowBit(i: Int) = (i + 1) % 7
fun AlarmItem.firesOn(bit: Int): Boolean = if (days == 0) (Calendar.getInstance().apply { timeInMillis = Alarms.nextAt(this@firesOn) }.get(Calendar.DAY_OF_WEEK) - 1) == bit else ((days shr bit) and 1) == 1
fun daysLabel(d: Int) = when (d) { 0 -> "No"; 127 -> "Daily"; else -> DAYN.filterIndexed { i, _ -> (d shr i) and 1 == 1 }.joinToString(" ") { it.take(2) } }
fun soundTitle(c: Context, s: String): String = if (s.isBlank()) "Default alarm" else try { RingtoneManager.getRingtone(c, Uri.parse(s))?.getTitle(c) ?: "Sound" } catch (e: Exception) { "Sound" }
fun loadSounds(c: Context): List<Pair<String, String>> { val l = mutableListOf("Default alarm" to "")
    try { val rm = RingtoneManager(c); rm.setType(RingtoneManager.TYPE_ALARM); val cur = rm.cursor; var n = 0; while (cur.moveToNext()) { l.add(cur.getString(1) to rm.getRingtoneUri(n).toString()); n++ } } catch (e: Exception) { }
    return l }
object Prev { var r: android.media.Ringtone? = null
    fun play(c: Context, s: String) { r?.stop(); r = RingtoneManager.getRingtone(c, if (s.isBlank()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else Uri.parse(s)); r?.play(); Handler(Looper.getMainLooper()).postDelayed({ r?.stop() }, 2500) } }

@Composable fun AlarmCard(a: AlarmItem, onEdit: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    val c = LocalContext.current; val d = dk(); val fg = fgC(d); val dim = dimC(d)
    val bg by animateColorAsState(if (a.on) (if (d) Color(0xFF1C1C1E) else Color.White) else (if (d) Color(0xFF0C0C0C) else Color(0xFFD0D0D0)), label = "cb")
    val st = remember(a.sound) { soundTitle(c, a.sound) }
    Column(Modifier.width(270.dp).height(150.dp).background(bg, R28).border(1.dp, if (d) Line else Color.Transparent, R28).press(onDelete, onEdit).padding(18.dp), Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (a.on) "On" else "Off", Modifier.background(fg.copy(.1f), CircleShape).press(null, onToggle).padding(14.dp, 5.dp), color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(daysLabel(a.days), color = dim, fontSize = 11.sp) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(hm(a.h, a.m), fontSize = 46.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp, color = if (a.on) fg else dim, maxLines = 1)
            if (!use24) Text(if (a.h < 12) "AM" else "PM", color = dim, fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp, bottom = 10.dp))
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(36.dp).background(fg, CircleShape).press(null) { Prev.play(c, a.sound) }, Alignment.Center) { Text("▶", color = bgC(d), fontSize = 13.sp) } }
        Text(st + if (a.game > 0) " · ${GAMES[a.game]}" else "", color = dim, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable fun AlarmHome(alarms: MutableList<AlarmItem>, onEdit: (AlarmItem?) -> Unit) {
    val c = LocalContext.current; val d = dk(); val fg = fgC(d); val dim = dimC(d); val hp = LocalHapticFeedback.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }; LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }
    LaunchedEffect(Unit) { alarms.clear(); alarms.addAll(Alarms.load(c)) }
    val today = now.dayOfWeek.value - 1; var sel by remember { mutableStateOf(today) }
    val snap = alarms.toList(); val mins = snap.filter { it.on }.minOfOrNull { Alarms.nextAt(it) }?.let { (it - System.currentTimeMillis()) / 60000 }
    val shown = snap.filter { it.firesOn(dowBit(sel)) }.sortedBy { it.h * 60 + it.m }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 110.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Alarm", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = fg)
            Text((if (use24) "24h" else "12h") + " ⌄", color = dim, fontSize = 14.sp, modifier = Modifier.press { use24 = !use24; S.save() }.padding(8.dp)) }
        Text(buildAnnotatedString { withStyle(SpanStyle(color = fg)) { append(hh(now.hour)) }; withStyle(SpanStyle(color = dim.copy(alpha = .55f))) { append(":" + two(now.minute)) } },
            Modifier.fillMaxWidth().padding(top = 28.dp), fontSize = 84.sp, fontWeight = FontWeight.Light, letterSpacing = (-4).sp, textAlign = TextAlign.Center)
        Text(buildAnnotatedString { append(if (mins == null) "No alarm set" else "The next alarm clock in "); if (mins != null) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = fg)) { append(if (mins < 60) "$mins min" else "${mins / 60}h ${mins % 60}m") } },
            Modifier.fillMaxWidth().padding(top = 6.dp), fontSize = 11.sp, color = dim, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().padding(top = 18.dp).height(96.dp).clip(RoundedCornerShape(18.dp)).background(cardC(d))) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height; val nowM = now.hour * 60 + now.minute + now.second / 60f; val start = nowM - 120f
                fun xOf(m: Float) = (m - start) / 480f * w
                var m = floor(start / 5f) * 5f
                while (m < start + 480f) { val x = xOf(m); val major = m.toInt() % 30 == 0
                    drawLine(Color(if (d) 0xFF333333 else 0xFFBDBDBD), Offset(x, h * (if (major) .18f else .3f)), Offset(x, h * .82f), 1.dp.toPx()); m += 5f }
                for (a in shown.filter { it.on }) { val x = xOf(a.h * 60f + a.m + ((sel - today + 7) % 7) * 1440f); if (x in 0f..w) drawLine(fg, Offset(x, h * .1f), Offset(x, h * .9f), 2.5.dp.toPx(), StrokeCap.Round) }
                drawLine(RED, Offset(xOf(nowM), h * .05f), Offset(xOf(nowM), h * .95f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), Arrangement.SpaceBetween) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { i, n -> val on = i == sel
                Box(Modifier.size(44.dp).background(if (on) (if (d) Color(0xFF2A2A2A) else Color.White) else Color.Transparent, CircleShape).press { sel = i }, Alignment.Center) {
                    Text(n, fontSize = 12.sp, color = if (on) fg else dim, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal) } } }
        LazyRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown, key = { it.id }) { a -> AlarmCard(a, { onEdit(a) }, { alarms[alarms.indexOfFirst { it.id == a.id }] = a.copy(on = !a.on); Alarms.save(c, alarms.toList()) },
                { hp.performHapticFeedback(HapticFeedbackType.LongPress); Alarms.cancel(c, a.id); alarms.removeAll { it.id == a.id }; Alarms.save(c, alarms.toList()) }) }
            if (shown.isEmpty()) item { Box(Modifier.width(270.dp).height(150.dp).background(cardC(d), R28).press { onEdit(null) }, Alignment.Center) { Text("No alarm on ${DAYN[dowBit(sel)]} · tap +", color = dim, fontSize = 13.sp) } }
        }
        Text("Tap a card to edit · long-press to delete", color = dim, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable fun Wheel(count: Int, value: Int, left: Boolean, onChange: (Int) -> Unit) {
    val hp = LocalHapticFeedback.current; val fg = fgC(dk()); val ih = 54.dp; val state = rememberLazyListState(count * 500 + value); var first by remember { mutableStateOf(true) }
    LaunchedEffect(state) { snapshotFlow { val li = state.layoutInfo; val mid = (li.viewportStartOffset + li.viewportEndOffset) / 2
        li.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: 0 }.collect { onChange(it % count); if (!first) hp.performHapticFeedback(HapticFeedbackType.TextHandleMove); first = false } }
    LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state), contentPadding = PaddingValues(vertical = ih * 3), modifier = Modifier.fillMaxHeight()) {
        items(count * 1000) { i ->
            Box(Modifier.fillMaxWidth().height(ih).padding(horizontal = 12.dp).graphicsLayer {
                val li = state.layoutInfo; val info = li.visibleItemsInfo.firstOrNull { it.index == i }
                if (info != null) { val mid = (li.viewportStartOffset + li.viewportEndOffset) / 2f; val a = abs((info.offset + info.size / 2f - mid) / info.size)
                    alpha = (1f - a * .28f).coerceIn(.12f, 1f); translationX = (if (left) -1 else 1) * a * a * 12.dp.toPx(); scaleX = 1f - .05f * min(a, 3f) } },
                contentAlignment = if (left) Alignment.CenterEnd else Alignment.CenterStart) { Text(two(i % count), fontSize = 34.sp, color = fg, fontWeight = FontWeight.Light) } }
    }
}

@Composable fun EditorBar(label: String, onX: () -> Unit, onOk: () -> Unit) { val d = dk()
    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(cardC(d), CircleShape).border(1.dp, Line, CircleShape).press(null, onX), Alignment.Center) { Text("✕", color = fgC(d)) }
        Text(label, color = fgC(d), fontSize = 13.sp)
        Box(Modifier.size(60.dp).background(fgC(d), CircleShape).press(null, onOk), Alignment.Center) { Text("✓", color = bgC(d), fontSize = 22.sp) } }
}
@Composable fun Opt(icon: String, title: String, value: String, f: () -> Unit) { val d = dk()
    Column(Modifier.press(null, f).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 18.sp, color = fgC(d)); Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fgC(d), modifier = Modifier.padding(top = 6.dp)); Text(value, fontSize = 10.sp, color = dimC(d), maxLines = 1) } }

@Composable fun Pad(page: Int, mod: Modifier) { val d = dk(); val fg = fgC(d)
    Box(mod.aspectRatio(1f).clip(R28).background(cardC(d)), Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) { val n = 12; val g = size.width / n; val rnd = java.util.Random(page * 7L + 3); val lit = if (page == 1) List(8) { rnd.nextInt(n * n) } else emptyList()
            for (r in 0 until n) for (cc in 0 until n) { val on = (r * n + cc) in lit; drawCircle(if (on) fg else Color(if (d) 0xFF2E2E2E else 0xFFCFCFCF), g * (if (on) .3f else .16f), Offset(g * (cc + .5f), g * (r + .5f))) } }
        if (page == 2) Text("7 × 8", fontSize = 40.sp, color = fg, fontWeight = FontWeight.Light)
        if (page == 0) Text("No game", fontSize = 18.sp, color = dimC(d)) }
}
@Composable fun Dial(h: Int, m: Int) { val d = dk()
    Box(Modifier.size(180.dp).background(cardC(d), CircleShape).border(1.dp, Line, CircleShape), Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) { val r = size.width / 2; for (k in 0 until 4) { val a = Math.toRadians(k * 90.0 + 45.0); val dx = cos(a).toFloat(); val dy = sin(a).toFloat()
            drawLine(fgC(d), Offset(center.x + dx * r * .84f, center.y + dy * r * .84f), Offset(center.x + dx * r * .96f, center.y + dy * r * .96f), 1.5.dp.toPx(), StrokeCap.Round) } }
        Text(buildAnnotatedString { withStyle(SpanStyle(color = fgC(d))) { append(hh(h)) }; withStyle(SpanStyle(color = dimC(d))) { append(":" + two(m)) } }, fontSize = 34.sp) } }

@Composable fun AlarmEditor(alarms: MutableList<AlarmItem>, editing: AlarmItem?, close: () -> Unit) {
    val c = LocalContext.current; val d = dk(); val fg = fgC(d); val hp = LocalHapticFeedback.current
    var step by remember { mutableStateOf(0) }; var h by remember { mutableStateOf(editing?.h ?: LocalTime.now().plusHours(1).hour) }; var m by remember { mutableStateOf(editing?.m ?: 0) }
    var sound by remember { mutableStateOf(editing?.sound ?: "") }; var snooze by remember { mutableStateOf(editing?.snooze ?: 10) }; var days by remember { mutableStateOf(editing?.days ?: 0) }; var game by remember { mutableStateOf(editing?.game ?: 0) }
    var dSound by remember { mutableStateOf(false) }; var dDays by remember { mutableStateOf(false) }
    fun save() { val item = (editing ?: AlarmItem((System.currentTimeMillis() % 1000000).toInt() + 1, 0, 0, "Alarm", true)).copy(h = h, m = m, on = true, sound = sound, snooze = snooze, days = days, game = game)
        val i = alarms.indexOfFirst { it.id == item.id }; if (i >= 0) alarms[i] = item else alarms.add(item); Alarms.save(c, alarms.toList()); close() }
    Column(Modifier.fillMaxSize().background(bgC(d)).statusBarsPadding().navigationBarsPadding()) {
        if (step == 0) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Canvas(Modifier.fillMaxSize()) { drawCircle(cardC(d), size.width * .62f, Offset(size.width * .1f, size.height * .5f)); drawCircle(cardC(d), size.width * .62f, Offset(size.width * .9f, size.height * .5f)) }
                Row(Modifier.align(Alignment.Center).fillMaxWidth().height(378.dp)) { Box(Modifier.weight(1f)) { Wheel(24, h, true) { h = it } }; Box(Modifier.weight(1f)) { Wheel(60, m, false) { m = it } } }
                Box(Modifier.align(Alignment.Center).width(150.dp).height(54.dp).border(1.dp, dimC(d).copy(.6f), RoundedCornerShape(12.dp)))
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.SpaceEvenly) {
                Opt("♪", "Sound", remember(sound) { soundTitle(c, sound) }.take(14)) { dSound = true }
                Opt("🔔", "Snooze", "Every $snooze min") { snooze = when (snooze) { 5 -> 10; 10 -> 15; 15 -> 20; else -> 5 } }
                Opt("⟳", "Repeat", daysLabel(days)) { dDays = true } }
            EditorBar("Choose time", close) { step = 1 }
        } else {
            val ps = rememberPagerState(initialPage = game) { 3 }
            LaunchedEffect(ps) { snapshotFlow { ps.currentPage }.collect { game = it; hp.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
            Column(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.padding(24.dp, 20.dp, 24.dp, 16.dp)) { Text("Choose", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = fg); Text("the Game", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = dimC(d)) }
                HorizontalPager(ps, contentPadding = PaddingValues(horizontal = 48.dp), pageSpacing = 12.dp) { p -> Pad(p, Modifier.fillMaxWidth()) }
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), Alignment.Center) { Dial(h, m) }
            }
            EditorBar(GAMES[game], { step = 0 }) { save() }
        }
    }
    if (dSound) { val sounds = remember { loadSounds(c) }
        AlertDialog({ dSound = false }, confirmButton = {}, title = { Text("Sound") }, text = { LazyColumn { items(sounds) { (t, u) -> Text(t, Modifier.fillMaxWidth().press { sound = u; Prev.play(c, u); dSound = false }.padding(14.dp)) } } }) }
    if (dDays) AlertDialog({ dDays = false }, confirmButton = { TextButton({ dDays = false }) { Text("Done") } }, title = { Text("Repeat") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { i, l -> val on = (days shr i) and 1 == 1
                Box(Modifier.size(34.dp).background(if (on) fg else cardC(d), CircleShape).border(1.dp, Line, CircleShape).press { days = days xor (1 shl i) }, Alignment.Center) { Text(l, fontSize = 12.sp, color = if (on) bgC(d) else fg) } } }
            Text("Once only", Modifier.press { days = 0 }.padding(6.dp), color = dimC(d)) } })
}

@Composable fun DotGame(onWin: () -> Unit) { val hp = LocalHapticFeedback.current
    val lit = remember { val r = java.util.Random(); val s = mutableStateListOf<Int>(); while (s.size < 7) { val k = r.nextInt(36); if (k !in s) s.add(k) }; s }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Remove all points", color = Dim, modifier = Modifier.padding(bottom = 10.dp))
        for (r in 0 until 6) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { for (cc in 0 until 6) { val k = r * 6 + cc; val on = k in lit
            Box(Modifier.size(38.dp).background(if (on) Color.White else Color(0xFF262626), CircleShape).clickable(enabled = on) { lit.remove(k); hp.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (lit.isEmpty()) onWin() }) } } } }
@Composable fun MathGame(onWin: () -> Unit) { val r = remember { java.util.Random() }; val a = remember { 3 + r.nextInt(7) }; val b = remember { 3 + r.nextInt(7) }
    val opts = remember { listOf(a * b, a * b + 1 + r.nextInt(6), a * b - 1 - r.nextInt(6)).distinct().shuffled() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$a × $b", fontSize = 56.sp, color = Color.White, fontWeight = FontWeight.Light, modifier = Modifier.padding(bottom = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { opts.forEach { o -> Pill("$o", false) { if (o == a * b) onWin() } } } } }

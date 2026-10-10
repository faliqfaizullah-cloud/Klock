@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.klock.app
import android.app.TimePickerDialog
import android.media.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

val Card = Color(0xFF0D0D0D); val Chip = Color(0xFF1C1C1C); val Line = Color(0xFF2A2A2A); val Ink = Color.White; val Dim = Color(0xFF8C8C8C)
val R28 = RoundedCornerShape(28.dp)
var use24 by mutableStateOf(true)
fun two(n: Int) = n.toString().padStart(2, '0')
fun hm(h: Int, m: Int) = if (use24) "${two(h)}:${two(m)}" else "${(h + 11) % 12 + 1}:${two(m)}"
fun pick(c: android.content.Context, h: Int, m: Int, done: (Int, Int) -> Unit) =
    TimePickerDialog(c, android.R.style.Theme_DeviceDefault_Dialog_Alert, { _, a, b -> done(a, b) }, h, m, use24).show()

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(SystemBarStyle.dark(t), SystemBarStyle.dark(t))
        requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
        setContent { App2() }
    }
}

@Composable fun Modifier.press(onLong: (() -> Unit)? = null, onClick: () -> Unit): Modifier {
    val h = LocalHapticFeedback.current; val src = remember { MutableInteractionSource() }
    val p by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (p) 0.95f else 1f, spring(0.5f, 600f), label = "s")
    return this.scale(s).combinedClickable(src, null, onLongClick = onLong?.let { f -> { h.performHapticFeedback(HapticFeedbackType.LongPress); f() } }) {
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
}
@Composable fun Modifier.enter(i: Int): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(i * 70L); a.animateTo(1f, spring(0.8f, 200f)) }
    return this.graphicsLayer { alpha = a.value; translationY = (1 - a.value) * 70f }
}

@Composable fun Ico(k: Int, c: Color, bg: Color) = Canvas(Modifier.size(22.dp)) {
    val w = size.width; val t = 2.dp.toPx(); val s = Stroke(t, cap = StrokeCap.Round); val o = center
    fun ln(a: Offset, b: Offset) = drawLine(c, a, b, t, StrokeCap.Round)
    when (k) {
        0 -> { drawCircle(c, w / 2 - t, style = s); ln(o, Offset(o.x, o.y - w * .25f)); ln(o, Offset(o.x + w * .2f, o.y)) }
        1 -> { drawCircle(c, w / 2 - t, style = s); drawOval(c, Offset(w * .3f, t), Size(w * .4f, w - 2 * t), style = s); ln(Offset(t, o.y), Offset(w - t, o.y)) }
        2 -> { drawCircle(c, w * .34f, o.copy(y = o.y + 1.5f.dp.toPx()), style = s); drawCircle(c, w * .1f, Offset(w * .14f, w * .16f)); drawCircle(c, w * .1f, Offset(w * .86f, w * .16f))
            ln(o.copy(y = o.y + 1.5f.dp.toPx()), Offset(o.x, o.y - w * .12f)) }
        3 -> { drawCircle(c, w * .36f, o.copy(y = o.y + 2.dp.toPx()), style = s); ln(Offset(o.x - w * .1f, t / 2), Offset(o.x + w * .1f, t / 2)); ln(o.copy(y = o.y + 2.dp.toPx()), Offset(o.x + w * .15f, o.y - w * .08f)) }
        else -> { drawCircle(c, w * .4f); drawCircle(bg, w * .34f, Offset(o.x + w * .2f, o.y - w * .14f)) }
    }
}

@Composable fun KlockApp() {
    var tab by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(tab, Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 84.dp), transitionSpec = {
            (fadeIn(spring(stiffness = 300f)) + slideInVertically(spring(0.8f, 300f)) { it / 14 }) togetherWith fadeOut(tween(120))
        }, label = "tab") { when (it) { 0 -> ClockScreen(); 1 -> WorldScreen(); 2 -> AlarmScreen(); 3 -> TimerScreen(); else -> BedScreen() } }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), Arrangement.spacedBy(8.dp)) {
            repeat(5) { i ->
                val on = i == tab; val bg by animateColorAsState(if (on) Ink else Chip, label = "n")
                val sc by animateFloatAsState(if (on) 1.1f else 1f, spring(0.5f, 500f), label = "z")
                Box(Modifier.size(52.dp).scale(sc).background(bg, CircleShape).border(1.dp, Line, CircleShape).press { tab = i }, Alignment.Center) { Ico(i, if (on) Color.Black else Ink, bg) }
            }
        }
    }
}

@Composable fun Toggle(a: String, b: String, first: Boolean, set: (Boolean) -> Unit) = Row(Modifier.background(Chip, CircleShape).padding(4.dp)) {
    listOf(a to true, b to false).forEach { (t, v) ->
        val on = first == v; val bg by animateColorAsState(if (on) Ink else Color.Transparent, label = "t")
        Text(t, Modifier.background(bg, CircleShape).press { set(v) }.padding(14.dp, 8.dp), color = if (on) Color.Black else Ink, fontSize = 14.sp)
    }
}
@Composable fun Plus(f: () -> Unit) = Box(Modifier.size(48.dp).background(Ink, CircleShape).press(null, f), Alignment.Center) { Text("+", color = Color.Black, fontSize = 26.sp) }
@Composable fun Head(t: String, sub: String? = null, end: @Composable () -> Unit = {}) = Column(Modifier.padding(24.dp, 16.dp, 24.dp, 12.dp)) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(t, fontSize = 40.sp, color = Ink); end() }
    if (sub != null) Text(sub, color = Dim, fontSize = 14.sp)
}
@Composable fun TCard(sub: String, name: String, time: String, sel: Boolean, emoji: String, mod: Modifier) {
    val bg by animateColorAsState(if (sel) Ink else Card, label = "b"); val fg = if (sel) Color.Black else Ink
    Box(mod.fillMaxWidth().background(bg, R28).border(1.dp, Line, R28).padding(24.dp, 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column { Text(sub, color = Dim, fontSize = 13.sp); Text(name, color = fg, fontSize = 18.sp) }
            Text(time, fontSize = 52.sp, letterSpacing = (-2).sp, color = fg)
        }
        Text(emoji, Modifier.align(Alignment.TopEnd), fontSize = 14.sp)
    }
}
@Composable fun Pill(t: String, main: Boolean = true, f: () -> Unit) = Text(t, Modifier.background(if (main) Ink else Chip, CircleShape).border(1.dp, Line, CircleShape).press(null, f).padding(28.dp, 16.dp),
    color = if (main) Color.Black else Ink, fontWeight = FontWeight.Medium)
fun utc(t: ZonedDateTime): String { val o = t.offset.totalSeconds / 3600f; return "UTC" + if (o % 1f == 0f) "%+d".format(o.toInt()) else "%+.1f".format(o) }

@Composable fun ClockScreen() {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(); delay(1000) } }
    val id = ZoneId.systemDefault().id; val city = id.substringAfter('/').replace('_', ' '); val region = id.substringBefore('/', "")
    val hh = now.format(DateTimeFormatter.ofPattern(if (use24) "HH" else "hh")); val mm = two(now.minute); val day = now.hour in 6..17
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Ink, CircleShape), Alignment.Center) { Ico(0, Color.Black, Ink) }
            Toggle("12h", "24h", !use24) { use24 = !it }
        }
        Row(Modifier.padding(top = 16.dp)) {
            Column { for (x in listOf(hh, mm)) Text(x, fontSize = 140.sp, lineHeight = 130.sp, letterSpacing = (-8).sp, fontWeight = FontWeight.Medium, color = Ink) }
            Column(Modifier.padding(start = 12.dp, top = 24.dp)) {
                Text(now.format(DateTimeFormatter.ofPattern("EEE,\nd MMM")), fontSize = 24.sp, color = Ink)
                Spacer(Modifier.height(48.dp))
                AnimatedContent(now.second, transitionSpec = { slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut() }, label = "s") { s ->
                    Column { Text(two((s + 59) % 60), fontSize = 30.sp, color = Dim.copy(alpha = .4f)); Text(two(s), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Ink); Text(two((s + 1) % 60), fontSize = 30.sp, color = Dim.copy(alpha = .4f)) }
                }
            }
        }
        Text("$city,\n$region", fontSize = 48.sp, lineHeight = 50.sp, color = Ink, modifier = Modifier.padding(top = 8.dp))
        Text((if (day) "Day ☀️" else "Night 🌙") + " · " + utc(now), color = Dim, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
    }
}

@Composable fun WorldScreen() {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(); delay(1000) } }
    val zones = remember { mutableStateListOf(ZoneId.systemDefault().id, "Asia/Tokyo", "Australia/Sydney", "America/New_York", "Europe/London") }
    var sel by remember { mutableStateOf(0) }; var add by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().padding(24.dp, 16.dp, 24.dp, 0.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) { zones.take(5).forEach { z ->
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFF4A4A4A), Color(0xFF111111)))), Alignment.Center) { Text(z.substringAfter('/').take(1), color = Ink, fontSize = 16.sp) } } }
            Plus { add = true }
        }
        Head("World Time")
        LazyColumn(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(zones, key = { _, z -> z }) { i, z ->
                val t = now.withZoneSameInstant(ZoneId.of(z))
                TCard(utc(t), z.substringAfter('/').replace('_', ' '), hm(t.hour, t.minute), sel == i, if (t.hour in 6..17) "☀️" else "🌙",
                    Modifier.animateItemPlacement().enter(i).press({ if (zones.size > 1) { zones.remove(z); sel = 0 } }) { sel = i })
            }
            item { Text("Tap to select · long-press to remove", color = Dim, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
    if (add) AlertDialog({ add = false }, confirmButton = {}, title = { Text("Add city") }, text = {
        LazyColumn { items(listOf("America/Los_Angeles", "Asia/Dubai", "Asia/Kolkata", "Asia/Jakarta", "Asia/Singapore", "Africa/Cairo", "America/Sao_Paulo", "Europe/Paris")) { z ->
            Text(z.substringAfter('/').replace('_', ' '), Modifier.fillMaxWidth().press { if (z !in zones) zones.add(z); add = false }.padding(14.dp)) } }
    })
}

@Composable fun AlarmTile(a: AlarmItem, i: Int, mod: Modifier, onLong: () -> Unit, onTap: () -> Unit) {
    val ang = listOf(-5f, 4f, -3f, 5f)[i % 4]
    val bg by animateColorAsState(if (a.on) Color(0xFFEDEDED) else Color(0xFF121212), label = "tb"); val fg = if (a.on) Color.Black else Ink
    val rot by animateFloatAsState(if (a.on) ang else ang / 2, spring(0.5f, 300f), label = "r")
    Box(mod.fillMaxWidth().aspectRatio(1f).rotate(rot).press(onLong, onTap).background(bg, R28).border(1.dp, Line, R28).padding(16.dp)) {
        Box(Modifier.size(44.dp).background(if (a.on) Color.Black else Chip, CircleShape), Alignment.Center) {
            Text(if (a.on) "ON" else "OFF", color = if (a.on) Ink else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(a.label + if (!use24) (if (a.h < 12) " · AM" else " · PM") else "", color = Dim, fontSize = 14.sp)
            Text(hm(a.h, a.m), fontSize = 40.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp, color = fg, maxLines = 1)
        }
    }
}

@Composable fun AlarmScreen() {
    val c = LocalContext.current
    val list = remember { mutableStateListOf<AlarmItem>().apply { addAll(Alarms.load(c)) } }
    fun commit() = Alarms.save(c, list.toList())
    fun add() = pick(c, 7, 0) { h, m -> list.add(AlarmItem((System.currentTimeMillis() % 100000).toInt(), h, m, "Alarm", true)); commit() }
    val now = LocalDateTime.now()
    val next = list.filter { it.on }.minOfOrNull { val t = now.toLocalDate().atTime(it.h, it.m); Duration.between(now, if (t.isAfter(now)) t else t.plusDays(1)).toMinutes() }
    Box(Modifier.fillMaxSize().drawBehind {
        val g = 11.dp.toPx(); var y = 0f
        while (y < size.height) { var x = 0f; while (x < size.width) { drawCircle(Color(0xFF2B2B2B), 1.dp.toPx(), Offset(x, y)); x += g }; y += g }
    }) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp, 16.dp, 24.dp, 8.dp)) {
                Text("Creating Alarm", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(if (next != null) "Next alarm in ${next / 60}h ${next % 60}m" else "No alarm set", color = Dim, fontSize = 14.sp)
            }
            val snap = list.toList()
            LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                items(snap.size, key = { snap[it].id }) { i ->
                    val a = snap[i]
                    AlarmTile(a, i, Modifier.animateItemPlacement().enter(i), { Alarms.cancel(c, a.id); list.remove(a); commit() }) { list[list.indexOf(a)] = a.copy(on = !a.on); commit() }
                }
            }
        }
        if (list.isEmpty()) Column(Modifier.align(Alignment.Center).padding(24.dp)) {
            Text("Set\nMultiple", fontSize = 44.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Alarms", fontSize = 44.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, color = Dim) }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(Color(0xFF3A1111), CircleShape).press { for (k in list.indices) list[k] = list[k].copy(on = false); commit() }, Alignment.Center) {
                Box(Modifier.size(18.dp).background(Color(0xFFFF2020), RoundedCornerShape(4.dp))) }
            Box(Modifier.width(150.dp).height(56.dp).background(Chip, CircleShape).border(1.dp, Line, CircleShape).press { add() }) {
                Canvas(Modifier.fillMaxSize().padding(16.dp)) { val g = 6.dp.toPx(); var y = g / 2
                    while (y < size.height) { var x = g / 2; while (x < size.width) { drawCircle(Dim, 1.2f.dp.toPx(), Offset(x, y)); x += g }; y += g } } }
            Box(Modifier.size(56.dp).press { use24 = !use24 }, Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawCircle(Line, size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))) }
                Box(Modifier.size(22.dp).background(Ink, RoundedCornerShape(6.dp)), Alignment.Center) { Box(Modifier.size(7.dp).background(Color.Black, CircleShape)) } }
        }
    }
}

@Composable fun TimerScreen() {
    var sw by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Head(if (sw) "Stopwatch" else "Timer") { Toggle("Timer", "Watch", !sw) { sw = !it } }
        Crossfade(sw, label = "sw") { if (it) WatchPane() else TimerPane() }
    }
}
@Composable fun TimerPane() {
    val c = LocalContext.current; val hp = LocalHapticFeedback.current
    var total by remember { mutableStateOf(300) }; var left by remember { mutableStateOf(300000L) }; var run by remember { mutableStateOf(false) }
    LaunchedEffect(run) { while (run && left > 0) { delay(50); left = (left - 50).coerceAtLeast(0) }
        if (run && left == 0L) { run = false; hp.performHapticFeedback(HapticFeedbackType.LongPress); c.getSystemService(android.os.Vibrator::class.java).vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)) } }
    val s = (left + 999) / 1000; val prog by animateFloatAsState(left / (total * 1000f), tween(100, easing = LinearEasing), label = "p")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(16.dp).size(260.dp), Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) { val st = Stroke(14.dp.toPx(), cap = StrokeCap.Round); drawArc(Chip, 0f, 360f, false, style = st); drawArc(Ink, -90f, 360f * prog, false, style = st) }
            Text("${two((s / 60).toInt())}:${two((s % 60).toInt())}", fontSize = 64.sp, letterSpacing = (-3).sp, color = Ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1, 5, 10, 15).forEach { m -> Pill("${m}m", false) { run = false; total = m * 60; left = total * 1000L } } }
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Pill(if (run) "Pause" else "Start") { run = !run }; Pill("Reset", false) { run = false; left = total * 1000L } }
    }
}
@Composable fun WatchPane() {
    var ms by remember { mutableStateOf(0L) }; var run by remember { mutableStateOf(false) }; val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(run) { if (run) { var last = System.nanoTime(); while (true) { withFrameNanos { n -> ms += (n - last) / 1_000_000; last = n } } } }
    fun f(l: Long) = "${two((l / 60000).toInt())}:${two((l / 1000 % 60).toInt())}.${two((l / 10 % 100).toInt())}"
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(f(ms), fontSize = 76.sp, letterSpacing = (-4).sp, fontWeight = FontWeight.Medium, color = Ink, maxLines = 1)
        Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(if (run) "Stop" else "Start") { run = !run }; Pill(if (run) "Lap" else "Reset", false) { if (run) laps.add(0, ms) else { ms = 0; laps.clear() } } }
        LazyColumn { itemsIndexed(laps) { i, l -> Text("Lap ${laps.size - i}    ${f(l)}", Modifier.padding(6.dp), color = Ink) } }
    }
}

@Composable fun BedScreen() {
    val c = LocalContext.current
    var bh by remember { mutableStateOf(23) }; var bm by remember { mutableStateOf(0) }; var wh by remember { mutableStateOf(7) }; var wm by remember { mutableStateOf(0) }
    var noise by remember { mutableStateOf<AudioTrack?>(null) }
    val mins = ((wh * 60 + wm) - (bh * 60 + bm) + 1440) % 1440
    DisposableEffect(Unit) { onDispose { noise?.release() } }
    Column(Modifier.fillMaxSize()) {
        Head("Bedtime", "Sleep: ${mins / 60}h ${two(mins % 60)}m")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TCard("Bedtime", "Tap to change", hm(bh, bm), false, "🌙", Modifier.press { pick(c, bh, bm) { h, m -> bh = h; bm = m } })
            TCard("Wake up", "Sets an alarm", hm(wh, wm), true, "☀️", Modifier.press { pick(c, wh, wm) { h, m -> wh = h; wm = m; Alarms.save(c, Alarms.load(c).filter { it.id != 777 } + AlarmItem(777, h, m, "Wake up", true)) } })
            Pill(if (noise == null) "Play sleep sound" else "Stop sound") {
                if (noise == null) { val buf = ShortArray(44100 * 2) { kotlin.random.Random.nextInt(-3500, 3500).toShort() }
                    noise = AudioTrack.Builder().setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(buf.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build().apply { write(buf, 0, buf.size); setLoopPoints(0, buf.size, -1); play() }
                } else { noise?.release(); noise = null } }
        }
    }
}

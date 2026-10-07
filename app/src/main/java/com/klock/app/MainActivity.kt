package com.klock.app
import android.app.TimePickerDialog
import android.media.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

val Bg = Color(0xFFF6F6F6); val Card = Color(0xFFE9E9E9); val Ink = Color(0xFF111111); val Dim = Color(0xFF9A9A9A)
val R28 = RoundedCornerShape(28.dp)

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
        setContent { MaterialTheme { KlockApp() } }
    }
}

@Composable fun Modifier.press(onClick: () -> Unit): Modifier {
    val h = LocalHapticFeedback.current; val src = remember { MutableInteractionSource() }
    val p by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (p) 0.93f else 1f, spring(0.5f, 600f), label = "s")
    return this.scale(s).clickable(src, null) { h.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
}

@Composable fun KlockApp() {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Alarm", "World", "Timer", "Watch", "Bed")
    Box(Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        AnimatedContent(tab, Modifier.fillMaxSize().padding(bottom = 80.dp), transitionSpec = {
            (fadeIn(spring(stiffness = 300f)) + slideInVertically(spring(0.8f, 300f)) { it / 14 }) togetherWith fadeOut(tween(120))
        }, label = "tab") {
            when (it) { 0 -> AlarmScreen(); 1 -> WorldScreen(); 2 -> TimerScreen(); 3 -> WatchScreen(); else -> BedScreen() }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color.White, CircleShape).padding(6.dp)) {
            tabs.forEachIndexed { i, t ->
                val bg by animateColorAsState(if (i == tab) Ink else Color.Transparent, label = "c")
                Text(t, Modifier.background(bg, CircleShape).press { tab = i }.padding(horizontal = 14.dp, vertical = 12.dp),
                    color = if (i == tab) Color.White else Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable fun Title(t: String, action: (() -> Unit)? = null) = Row(Modifier.fillMaxWidth().padding(24.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(t, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
    if (action != null) Box(Modifier.size(44.dp).background(Ink, CircleShape).press(action), Alignment.Center) { Text("+", color = Color.White, fontSize = 24.sp) }
}
@Composable fun Big(t: String, size: Int = 88) = Text(t, fontSize = size.sp, fontWeight = FontWeight.Medium, color = Ink, letterSpacing = (-3).sp, lineHeight = size.sp)
@Composable fun Pill(t: String, dark: Boolean = true, f: () -> Unit) = Text(t, Modifier.background(if (dark) Ink else Card, CircleShape).press(f).padding(28.dp, 16.dp),
    color = if (dark) Color.White else Ink, fontWeight = FontWeight.Medium)
fun pick(c: android.content.Context, h: Int, m: Int, done: (Int, Int) -> Unit) = TimePickerDialog(c, { _, a, b -> done(a, b) }, h, m, true).show()
fun two(n: Int) = n.toString().padStart(2, '0')

@OptIn(ExperimentalFoundationApi::class)
@Composable fun AlarmScreen() {
    val c = LocalContext.current; val hp = LocalHapticFeedback.current
    val list = remember { mutableStateListOf<AlarmItem>().apply { addAll(Alarms.load(c)) } }
    fun commit() = Alarms.save(c, list.toList())
    Column {
        Title("Alarms") { pick(c, 7, 0) { h, m -> list.add(AlarmItem((System.currentTimeMillis() % 100000).toInt(), h, m, "Alarm", true)); commit() } }
        LazyColumn(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { a ->
                Box(Modifier.animateItemPlacement().fillMaxWidth().background(if (a.on) Ink else Card, R28)
                    .combinedClickable(onLongClick = { hp.performHapticFeedback(HapticFeedbackType.LongPress); Alarms.cancel(c, a.id); list.remove(a); commit() }) {
                        hp.performHapticFeedback(HapticFeedbackType.TextHandleMove); list[list.indexOf(a)] = a.copy(on = !a.on); commit() }.padding(24.dp)) {
                    Column { Text(a.label, color = Dim); Text("${two(a.h)}:${two(a.m)}", fontSize = 56.sp, letterSpacing = (-2).sp, color = if (a.on) Color.White else Ink) }
                    Text(if (a.on) "ON" else "OFF", Modifier.align(Alignment.TopEnd).background(if (a.on) Color.White else Ink, CircleShape).padding(12.dp, 6.dp),
                        color = if (a.on) Ink else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { Text("Tap to toggle · long-press to delete", color = Dim, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun WorldScreen() {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(); delay(1000) } }
    val zones = remember { mutableStateListOf(ZoneId.systemDefault().id, "Europe/London", "Europe/Paris", "America/New_York", "Asia/Tokyo") }
    var add by remember { mutableStateOf(false) }
    Column {
        Title("World Clock") { add = true }
        LazyColumn(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(zones, key = { _, z -> z }) { i, z ->
                val t = now.withZoneSameInstant(ZoneId.of(z)); val day = t.hour in 6..17; val fg = if (i == 0) Color.White else Ink
                Column(Modifier.animateItemPlacement().fillMaxWidth().background(if (i == 0) Ink else Card, R28).press { if (i > 0) zones.remove(z) }.padding(24.dp)) {
                    Row { Text(z.substringAfter('/').replace('_', ' '), color = fg, modifier = Modifier.weight(1f)); Text("UTC " + t.offset.id, color = Dim, fontSize = 12.sp) }
                    Text(if (day) "Day ☀️" else "Night 🌙", color = Dim, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
                    Text(t.format(DateTimeFormatter.ofPattern("HH:mm:ss")), color = fg, fontSize = 44.sp, letterSpacing = (-2).sp)
                }
            }
        }
    }
    if (add) AlertDialog({ add = false }, confirmButton = {}, title = { Text("Add city") }, text = {
        LazyColumn { items(listOf("America/Los_Angeles", "Asia/Dubai", "Asia/Kolkata", "Asia/Jakarta", "Asia/Singapore", "Australia/Sydney", "Africa/Cairo", "America/Sao_Paulo")) { z ->
            Text(z.substringAfter('/').replace('_', ' '), Modifier.fillMaxWidth().press { if (z !in zones) zones.add(z); add = false }.padding(14.dp)) } }
    })
}

@Composable fun TimerScreen() {
    val c = LocalContext.current; val hp = LocalHapticFeedback.current
    var total by remember { mutableStateOf(300) }; var left by remember { mutableStateOf(300000L) }; var run by remember { mutableStateOf(false) }
    LaunchedEffect(run) { while (run && left > 0) { delay(50); left = (left - 50).coerceAtLeast(0) }
        if (run && left == 0L) { run = false; hp.performHapticFeedback(HapticFeedbackType.LongPress); c.getSystemService(android.os.Vibrator::class.java).vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)) } }
    val s = (left + 999) / 1000
    Column(Modifier.fillMaxSize()) {
        Title("Timer")
        Column(Modifier.padding(24.dp)) { Big(two((s / 60).toInt()), 120); Big(two((s % 60).toInt()), 120) }
        LinearProgressIndicator({ left / (total * 1000f) }, Modifier.padding(24.dp).fillMaxWidth().height(8.dp).clip(CircleShape), color = Ink, trackColor = Card)
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 5, 10, 15).forEach { m -> Pill("${m}m", false) { run = false; total = m * 60; left = total * 1000L } } }
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(if (run) "Pause" else "Start") { run = !run }; Pill("Reset", false) { run = false; left = total * 1000L } }
    }
}

@Composable fun WatchScreen() {
    var ms by remember { mutableStateOf(0L) }; var run by remember { mutableStateOf(false) }; val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(run) { if (run) { var last = System.nanoTime(); while (true) { withFrameNanos { n -> ms += (n - last) / 1_000_000; last = n } } } }
    Column(Modifier.fillMaxSize()) {
        Title("Stopwatch")
        Column(Modifier.padding(24.dp)) { Big(two((ms / 60000).toInt()) + ":" + two((ms / 1000 % 60).toInt()), 96); Text(".${two((ms / 10 % 100).toInt())}", fontSize = 40.sp, color = Dim) }
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(if (run) "Stop" else "Start") { run = !run }
            Pill(if (run) "Lap" else "Reset", false) { if (run) laps.add(0, ms) else { ms = 0; laps.clear() } } }
        LazyColumn(Modifier.padding(24.dp)) { itemsIndexed(laps) { i, l -> Text("Lap ${laps.size - i}   ${two((l / 60000).toInt())}:${two((l / 1000 % 60).toInt())}.${two((l / 10 % 100).toInt())}", Modifier.padding(6.dp), color = Ink) } }
    }
}

@Composable fun BedScreen() {
    val c = LocalContext.current
    var bh by remember { mutableStateOf(23) }; var bm by remember { mutableStateOf(0) }; var wh by remember { mutableStateOf(7) }; var wm by remember { mutableStateOf(0) }
    var noise by remember { mutableStateOf<AudioTrack?>(null) }
    val mins = ((wh * 60 + wm) - (bh * 60 + bm) + 1440) % 1440
    DisposableEffect(Unit) { onDispose { noise?.release() } }
    Column(Modifier.fillMaxSize()) {
        Title("Bedtime")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().background(Card, R28).press { pick(c, bh, bm) { h, m -> bh = h; bm = m } }.padding(24.dp)) { Text("Bedtime", color = Dim); Big("${two(bh)}:${two(bm)}", 56) }
            Column(Modifier.fillMaxWidth().background(Ink, R28).press { pick(c, wh, wm) { h, m -> wh = h; wm = m; Alarms.save(c, Alarms.load(c).filter { it.id != 777 } + AlarmItem(777, h, m, "Wake up", true)) } }.padding(24.dp)) {
                Text("Wake up · sets alarm", color = Dim); Text("${two(wh)}:${two(wm)}", fontSize = 56.sp, color = Color.White, letterSpacing = (-2).sp) }
            Text("Sleep: ${mins / 60}h ${two(mins % 60)}m", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
            Pill(if (noise == null) "Play sleep sound" else "Stop sound") {
                if (noise == null) { val buf = ShortArray(44100 * 2) { kotlin.random.Random.nextInt(-3500, 3500).toShort() }
                    noise = AudioTrack.Builder().setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(buf.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build().apply { write(buf, 0, buf.size); setLoopPoints(0, buf.size, -1); play() }
                } else { noise?.release(); noise = null } }
        }
    }
}

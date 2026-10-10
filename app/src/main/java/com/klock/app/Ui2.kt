@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.klock.app
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

data class City(val name: String, val region: String, val tz: String, val lat: Double, val lon: Double)
val CITIES = listOf(City("Toronto", "Ontario, Canada", "America/Toronto", 43.65, -79.38), City("New York", "New York, USA", "America/New_York", 40.71, -74.01),
    City("Los Angeles", "California, USA", "America/Los_Angeles", 34.05, -118.24), City("London", "England, UK", "Europe/London", 51.51, -0.13),
    City("Paris", "Île-de-France, France", "Europe/Paris", 48.86, 2.35), City("Cairo", "Cairo, Egypt", "Africa/Cairo", 30.04, 31.24),
    City("Dubai", "Dubai, UAE", "Asia/Dubai", 25.2, 55.27), City("Mumbai", "Maharashtra, India", "Asia/Kolkata", 19.08, 72.88),
    City("Jakarta", "Jakarta, Indonesia", "Asia/Jakarta", -6.2, 106.82), City("Singapore", "Singapore", "Asia/Singapore", 1.35, 103.82),
    City("Tokyo", "Tokyo, Japan", "Asia/Tokyo", 35.68, 139.69), City("Sydney", "New South Wales, Australia", "Australia/Sydney", -33.87, 151.21))
val LOCAL: City = ZoneId.systemDefault().id.let { id -> CITIES.firstOrNull { it.tz == id } ?: City(id.substringAfter('/').replace('_', ' '), id.substringBefore('/', ""), id, Double.NaN, Double.NaN) }
val ALL: List<City> = if (LOCAL in CITIES) CITIES else listOf(LOCAL) + CITIES
fun cityOf(n: String) = ALL.firstOrNull { it.name == n } ?: LOCAL

object S {
    private lateinit var sp: SharedPreferences
    var appearance by mutableStateOf(0); var ink by mutableStateOf(false); var glass by mutableStateOf(false); var temp by mutableStateOf(0); var sel by mutableStateOf(0)
    val cities = mutableStateListOf<String>()
    fun load(c: Context) {
        sp = c.getSharedPreferences("k2", 0); appearance = sp.getInt("ap", 0); ink = sp.getBoolean("ink", false); glass = sp.getBoolean("gl", false)
        temp = sp.getInt("tp", 0); sel = sp.getInt("sel", 0); use24 = sp.getBoolean("h24", true)
        cities.clear(); cities.addAll(sp.getString("c", "")!!.split(";").filter { n -> ALL.any { it.name == n } })
        if (cities.isEmpty()) cities.add(LOCAL.name); if (sel !in cities.indices) sel = 0
    }
    fun save() { sp.edit().putInt("ap", appearance).putBoolean("ink", ink).putBoolean("gl", glass).putInt("tp", temp).putInt("sel", sel).putBoolean("h24", use24).putString("c", cities.joinToString(";")).apply() }
}

data class Wx(val t: Double, val code: Int, val rise: String, val set: String)
suspend fun fetchWx(c: City): Wx? = withContext(Dispatchers.IO) {
    if (c.lat.isNaN()) return@withContext null
    try {
        val j = JSONObject(URL("https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lon}&current=temperature_2m,weather_code&daily=sunrise,sunset&timezone=auto&forecast_days=1").readText())
        val cur = j.getJSONObject("current"); val d = j.getJSONObject("daily")
        Wx(cur.getDouble("temperature_2m"), cur.getInt("weather_code"), d.getJSONArray("sunrise").getString(0).takeLast(5), d.getJSONArray("sunset").getString(0).takeLast(5))
    } catch (e: Exception) { null }
}
fun tempStr(t: Double): String { val f = S.temp == 2 || (S.temp == 0 && Locale.getDefault().country in listOf("US", "LR", "MM")); return "${Math.round(if (f) t * 9 / 5 + 32 else t)}°" }
fun wxIcon(c: Int, night: Boolean) = when (c) { 0 -> if (night) "🌙" else "☀️"; 1, 2 -> "⛅"; 3 -> "☁️"; 45, 48 -> "🌫"; in 51..67, in 80..82 -> "🌧"; in 71..77, 85, 86 -> "❄️"; in 95..99 -> "⛈"; else -> "☁️" }
fun moonName(): String { val a = (((System.currentTimeMillis() / 86400000.0) - 10957.76) % 29.530588 + 29.530588) % 29.530588
    return when { a < 1.85 -> "New moon"; a < 5.53 -> "Waxing crescent"; a < 9.22 -> "First quarter"; a < 12.91 -> "Waxing gibbous"; a < 16.61 -> "Full moon"; a < 20.3 -> "Waning gibbous"; a < 23.99 -> "Last quarter"; a < 27.68 -> "Waning crescent"; else -> "New moon" } }

val KEYS = listOf(0f to (0xFF0B1230 to 0xFF1B2350), 5.5f to (0xFF2D2A5E to 0xFF7A5A9A), 7.5f to (0xFF6F8FBF to 0xFFF2C0A0), 10f to (0xFF5E9BD6 to 0xFFCFE6F5),
    15f to (0xFF5E9BD6 to 0xFFCFE6F5), 18f to (0xFF4A5A9A to 0xFFF2A65A), 19.5f to (0xFF2A2650 to 0xFF7A4A7A), 21f to (0xFF0B1230 to 0xFF1B2350), 24f to (0xFF0B1230 to 0xFF1B2350))
fun sky(hf: Float): Pair<Color, Color> {
    val i = KEYS.indexOfLast { it.first <= hf }.coerceIn(0, KEYS.size - 2); val a = KEYS[i]; val b = KEYS[i + 1]; val t = ((hf - a.first) / (b.first - a.first)).coerceIn(0f, 1f)
    return lerp(Color(a.second.first), Color(b.second.first), t) to lerp(Color(a.second.second), Color(b.second.second), t)
}

fun DrawScope.scene(hf: Float, ink: Boolean, night: Boolean) {
    val w = size.width; val h = size.height; val hz = h * 0.88f
    val dayT = (hf - 6f) / 12f; val isDay = dayT in 0f..1f; val tt = if (isDay) dayT else ((hf - 18f + 24f) % 24f) / 12f
    if (night) { val r = java.util.Random(3); repeat(26) { drawCircle(Color.White.copy(0.3f + r.nextFloat() * 0.5f), 1.2.dp.toPx(), Offset(r.nextFloat() * w, r.nextFloat() * h * 0.5f)) } }
    drawCircle(if (isDay) Color(0xFFFFD66B) else Color(0xFFEDEDED), 14.dp.toPx(), Offset(w * (0.1f + 0.8f * tt), h * (0.5f - 0.4f * sin(Math.PI.toFloat() * tt))))
    val rnd = java.util.Random(7); var x = -4f
    val body = if (ink) Color(0xFF404044) else if (night) Color(0xFF141A2C) else Color(0xFF2E3652)
    while (x < w) {
        val bw = w * (0.05f + rnd.nextFloat() * 0.05f); val bh = h * (0.12f + rnd.nextFloat() * 0.2f)
        drawRect(body, Offset(x, hz - bh), Size(bw, bh))
        var wy = hz - bh + 8.dp.toPx()
        while (wy < hz - 6.dp.toPx()) {
            var wx = x + 5.dp.toPx()
            while (wx < x + bw - 6.dp.toPx()) { if (rnd.nextFloat() < 0.35f) drawRect(if (night) (if (ink) Color.White.copy(.7f) else Color(0xFFF2C14E)) else Color.White.copy(.12f), Offset(wx, wy), Size(3.dp.toPx(), 4.dp.toPx())); wx += 8.dp.toPx() }
            wy += 9.dp.toPx()
        }
        x += bw + 2.dp.toPx()
    }
    val red = if (ink) Color(0xFFBBBBBB) else Color(0xFFD83A3A); val tx = w * 0.4f
    drawRect(red, Offset(tx - 2.dp.toPx(), hz - h * 0.6f), Size(4.dp.toPx(), h * 0.6f)); drawCircle(red, 7.dp.toPx(), Offset(tx, hz - h * 0.42f))
    drawRect(Color(0xFF0B0F1E).copy(alpha = 0.55f), Offset(0f, hz), Size(w, h - hz))
}

@Composable fun Precip(snow: Boolean, mod: Modifier) {
    val drops = remember { List(90) { floatArrayOf(kotlin.random.Random.nextFloat(), kotlin.random.Random.nextFloat(), 0.5f + kotlin.random.Random.nextFloat()) } }
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(snow) { var last = 0L; while (true) { withFrameNanos { n -> if (last != 0L) { val dt = (n - last) / 1e9f
        drops.forEach { d -> d[1] += dt * d[2] * (if (snow) 0.15f else 1.4f); if (d[1] > 1f) { d[1] = 0f; d[0] = kotlin.random.Random.nextFloat() } } }; last = n; tick = n } } }
    Canvas(mod) { val t = tick; drops.forEach { d -> val x = d[0] * size.width; val y = d[1] * size.height
        if (snow) drawCircle(Color.White.copy(.8f), 2.dp.toPx(), Offset(x, y)) else drawLine(Color.White.copy(.35f), Offset(x, y), Offset(x - 6f, y + 22f), 1.5f) } }
}

@Composable fun dk(): Boolean = when (S.appearance) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
fun bgC(d: Boolean) = if (d) Color(0xFF0F0F10) else Color(0xFFEDEAE0)
fun fgC(d: Boolean) = if (d) Color.White else Color(0xFF161616)
fun cardC(d: Boolean) = if (d) Color(0xFF1C1C1E) else Color(0xFFF6F4EC)
fun dimC(d: Boolean) = if (d) Color(0xFF8C8C8C) else Color(0xFF8A877C)
fun hh(h: Int) = two(if (use24) h else (h + 11) % 12 + 1)
val RED = Color(0xFFE5483B)

@Composable fun Ico2(k: Int, c: Color) = Canvas(Modifier.size(22.dp)) {
    val w = size.width; val t = 2.dp.toPx(); val s = Stroke(t, cap = StrokeCap.Round); val o = center
    when (k) {
        0 -> { drawCircle(c, w * 0.32f, Offset(w * 0.43f, w * 0.43f), style = s); drawLine(c, Offset(w * 0.67f, w * 0.67f), Offset(w * 0.92f, w * 0.92f), t, StrokeCap.Round) }
        1 -> { drawCircle(c, w / 2 - t, style = s); drawLine(c, o, Offset(o.x, o.y - w * .25f), t, StrokeCap.Round); drawLine(c, o, Offset(o.x + w * .2f, o.y), t, StrokeCap.Round) }
        2 -> { drawCircle(c, w / 2 - t, style = s); drawOval(c, Offset(w * .3f, t), Size(w * .4f, w - 2 * t), style = s); drawLine(c, Offset(t, o.y), Offset(w - t, o.y), t) }
        else -> { drawCircle(c, w * .2f, style = s); repeat(8) { val a = Math.toRadians(it * 45.0)
            drawLine(c, Offset(o.x + (cos(a) * w * .34f).toFloat(), o.y + (sin(a) * w * .34f).toFloat()), Offset(o.x + (cos(a) * w * .48f).toFloat(), o.y + (sin(a) * w * .48f).toFloat()), t * 1.5f, StrokeCap.Round) } }
    }
}

@Composable fun App2() {
    val c = LocalContext.current; remember { S.load(c); 0 }
    val d = dk(); var tab by remember { mutableStateOf(1) }; var layer by remember { mutableStateOf(0) }
    val alarms = remember { mutableStateListOf<AlarmItem>().apply { addAll(Alarms.load(c)) } }
    BackHandler(layer != 0) { layer = if (layer == 2) 1 else 0 }
    MaterialTheme(colorScheme = if (d) darkColorScheme() else lightColorScheme()) {
        Box(Modifier.fillMaxSize().background(bgC(d))) {
            AnimatedContent(tab, Modifier.fillMaxSize(), transitionSpec = { (fadeIn(spring(stiffness = 300f)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150)) }, label = "tab") {
                when (it) { 0 -> SearchTab { tab = 1 }; 1 -> ClockMain({ layer = 1 }, { layer = 3 }); 2 -> GlobeTab { tab = 1 }; else -> SettingsTab() } }
            val nd = tab == 1 || d
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).clip(CircleShape)
                .background(if (S.glass) Color.White.copy(.16f) else if (nd) Color(0xFF2B2B2D) else Color(0xFFDCD9CE)).border(1.dp, if (S.glass) Color.White.copy(.3f) else Color.Transparent, CircleShape).padding(5.dp), Arrangement.spacedBy(4.dp)) {
                repeat(4) { i -> val on = i == tab
                    val bg by animateColorAsState(if (on) (if (nd) Color.White else Color(0xFF111111)) else Color.Transparent, label = "nb")
                    val sc by animateFloatAsState(if (on) 1.05f else 1f, spring(.5f, 500f), label = "ns")
                    Box(Modifier.size(46.dp).scale(sc).background(bg, CircleShape).press { tab = i }, Alignment.Center) { Ico2(i, if (on) (if (nd) Color.Black else Color.White) else (if (nd) Color.White else Color(0xFF111111))) } }
            }
            val enter = slideInVertically(spring(0.85f, 300f)) { it } + fadeIn(); val exit = slideOutVertically { it } + fadeOut()
            AnimatedVisibility(layer == 1, Modifier.fillMaxSize(), enter, exit) { LaunchedEffect(Unit) { alarms.clear(); alarms.addAll(Alarms.load(c)) }; AlarmsLayer(alarms, { layer = 0 }) { layer = 2 } }
            AnimatedVisibility(layer == 2, Modifier.fillMaxSize(), enter, exit) { AddAlarm(alarms) { layer = 1 } }
            AnimatedVisibility(layer == 3, Modifier.fillMaxSize(), enter, exit) { ToolsLayer { layer = 0 } }
        }
    }
}

@Composable fun ClockMain(openAlarms: () -> Unit, openTools: () -> Unit) {
    val city = cityOf(S.cities[S.sel.coerceIn(0, S.cities.size - 1)]); val zone = ZoneId.of(city.tz)
    var now by remember(city) { mutableStateOf(ZonedDateTime.now(zone)) }
    LaunchedEffect(city) { while (true) { now = ZonedDateTime.now(zone); delay(1000) } }
    var wx by remember(city) { mutableStateOf<Wx?>(null) }
    LaunchedEffect(city) { while (true) { fetchWx(city)?.let { wx = it }; delay(600_000) } }
    val hf = now.hour + now.minute / 60f; val code = wx?.code ?: 0; val night = hf < 6f || hf >= 18.5f
    val snow = code in 71..77 || code in 85..86; val storm = snow || code in 51..67 || code in 80..82 || code in 95..99
    val (t0, b0) = sky(hf)
    val top by animateColorAsState(if (storm) lerp(t0, Color(0xFF26272A), .85f) else t0, tween(1200), label = "t")
    val bot by animateColorAsState(if (storm) lerp(b0, Color(0xFF3A3C40), .85f) else b0, tween(1200), label = "b")
    val dark = (top.luminance() + bot.luminance()) / 2 < 0.4f; val fg = if (dark) Color.White else Color(0xFF111111)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bot)))) {
        Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(380.dp).padding(bottom = 70.dp)) { scene(hf, S.ink, night) }
        if (storm) Precip(snow, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), Arrangement.SpaceBetween) {
                Box(Modifier.size(44.dp).background(fg.copy(.14f), CircleShape).press { openTools() }, Alignment.Center) { Ico(3, fg, Color.Transparent) }
                Box(Modifier.size(44.dp).background(fg.copy(.14f), CircleShape).press { openAlarms() }, Alignment.Center) { Ico(2, fg, Color.Transparent) }
            }
            Row(Modifier.padding(top = 16.dp)) {
                Column { for (x in listOf(hh(now.hour), two(now.minute))) Text(x, fontSize = 140.sp, lineHeight = 128.sp, letterSpacing = (-8).sp, fontWeight = FontWeight.Medium, color = fg) }
                Box(Modifier.padding(start = 14.dp, top = 14.dp).width(1.5.dp).height(150.dp).background(RED))
                Column(Modifier.padding(start = 12.dp, top = 14.dp)) {
                    Text(now.format(DateTimeFormatter.ofPattern("EEE,\nd MMM")), fontSize = 22.sp, color = fg)
                    if (!use24) Text(if (now.hour < 12) "AM" else "PM", fontSize = 13.sp, color = fg.copy(.6f))
                    Spacer(Modifier.height(36.dp))
                    AnimatedContent(now.second, transitionSpec = { slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut() }, label = "s") { s ->
                        Column { Text(two((s + 59) % 60), fontSize = 26.sp, color = fg.copy(.3f)); Text(two(s), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = fg); Text(two((s + 1) % 60), fontSize = 26.sp, color = fg.copy(.3f)) } }
                }
            }
            Text(city.name, fontSize = 46.sp, color = fg, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
            Text(city.region, fontSize = 13.sp, color = fg.copy(.6f))
            Text("◐ ${moonName()}  " + (wx?.let { "${it.rise} – ${it.set}" } ?: "") + (wx?.let { "   ${wxIcon(it.code, night)} ${tempStr(it.t)} ›" } ?: ""), fontSize = 12.sp, color = fg.copy(.7f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable fun Seg(o: List<String>, sel: Int, d: Boolean, set: (Int) -> Unit) = Row(Modifier.background(if (d) Color(0xFF2A2A2C) else Color(0xFFE4E1D6), CircleShape).padding(3.dp)) {
    o.forEachIndexed { i, t -> val on = i == sel
        val bg by animateColorAsState(if (on) fgC(d) else Color.Transparent, label = "g")
        Text(t, Modifier.background(bg, CircleShape).press { set(i); S.save() }.padding(12.dp, 6.dp), color = if (on) bgC(d) else fgC(d), fontSize = 13.sp) }
}
@Composable fun SRow(l: String, o: List<String>, sel: Int, d: Boolean, set: (Int) -> Unit) = Row(Modifier.fillMaxWidth().padding(16.dp, 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
    Text(l, color = fgC(d), fontSize = 15.sp); Seg(o, sel, d, set) }

@Composable fun SettingsTab() {
    val d = dk()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp, 24.dp, 20.dp, 110.dp)) {
        Text("Settings", fontSize = 36.sp, color = fgC(d)); Text("Display", color = dimC(d), fontSize = 12.sp, modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 8.dp))
        Column(Modifier.fillMaxWidth().background(cardC(d), RoundedCornerShape(20.dp))) {
            SRow("Appearance", listOf("Auto", "Light", "Dark"), S.appearance, d) { S.appearance = it }
            SRow("Illustration", listOf("Color", "Ink"), if (S.ink) 1 else 0, d) { S.ink = it == 1 }
            SRow("Liquid Glass", listOf("Off", "On"), if (S.glass) 1 else 0, d) { S.glass = it == 1 }
        }
        Column(Modifier.padding(top = 12.dp).fillMaxWidth().background(cardC(d), RoundedCornerShape(20.dp))) {
            SRow("Time", listOf("12h", "24h"), if (use24) 1 else 0, d) { use24 = it == 1 }
            SRow("Temperature", listOf("Auto", "°C", "°F"), S.temp, d) { S.temp = it }
        }
    }
}

@Composable fun SearchTab(go: () -> Unit) {
    val d = dk(); var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp, 24.dp, 20.dp, 0.dp)) {
        Text("Search", fontSize = 36.sp, color = fgC(d))
        TextField(q, { q = it }, Modifier.padding(vertical = 16.dp).fillMaxWidth(), placeholder = { Text("Find a city") }, singleLine = true, shape = CircleShape,
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedContainerColor = cardC(d), unfocusedContainerColor = cardC(d)))
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ALL.filter { it.name.contains(q, true) || it.region.contains(q, true) }) { ct ->
                Column(Modifier.fillMaxWidth().background(cardC(d), RoundedCornerShape(20.dp)).press { if (ct.name !in S.cities) S.cities.add(ct.name); S.sel = S.cities.indexOf(ct.name); S.save(); go() }.padding(18.dp)) {
                    Text(ct.name, fontSize = 18.sp, color = fgC(d)); Text(ct.region, fontSize = 12.sp, color = dimC(d)) } }
        }
    }
}

@Composable fun GlobeTab(go: () -> Unit) {
    val d = dk(); var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(); delay(1000) } }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp, 24.dp, 20.dp, 0.dp)) {
        Text("World Time", fontSize = 36.sp, color = fgC(d)); Spacer(Modifier.height(16.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(S.cities.toList(), key = { _, n -> n }) { i, n -> val ct = cityOf(n); val t = now.withZoneSameInstant(ZoneId.of(ct.tz)); val diff = (t.offset.totalSeconds - now.offset.totalSeconds) / 3600f
                Row(Modifier.animateItemPlacement().enter(i).fillMaxWidth().background(if (i == S.sel) fgC(d) else cardC(d), RoundedCornerShape(24.dp))
                    .press({ if (S.cities.size > 1) { S.cities.remove(n); S.sel = 0; S.save() } }) { S.sel = i; S.save(); go() }.padding(20.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    val f = if (i == S.sel) bgC(d) else fgC(d)
                    Column { Text(ct.name, fontSize = 18.sp, color = f); Text((if (diff >= 0) "+" else "") + (if (diff % 1f == 0f) diff.toInt().toString() else diff.toString()) + "h from you", fontSize = 12.sp, color = f.copy(.6f)) }
                    Text(hm(t.hour, t.minute), fontSize = 40.sp, letterSpacing = (-2).sp, color = f) } }
            item { Text("Tap to open · long-press to remove", color = dimC(d), fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@Composable fun AlarmsLayer(alarms: MutableList<AlarmItem>, close: () -> Unit, add: () -> Unit) {
    val c = LocalContext.current; val d = dk(); val snap = alarms.toList()
    Column(Modifier.fillMaxSize().background(bgC(d)).statusBarsPadding().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Box(Modifier.size(44.dp).background(cardC(d), CircleShape).press { close() }, Alignment.Center) { Text("✕", color = fgC(d)) }
            Box(Modifier.size(44.dp).background(fgC(d), CircleShape).press { add() }, Alignment.Center) { Text("+", color = bgC(d), fontSize = 24.sp) } }
        Text("Alarms", fontSize = 36.sp, color = fgC(d), modifier = Modifier.padding(vertical = 16.dp))
        if (snap.isEmpty()) Text("Wake up on any city's time.", color = dimC(d))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(snap.size, key = { snap[it].id }) { i -> val a = snap[i]; val ct = cityOf(a.city.ifBlank { LOCAL.name })
                val t = ZonedDateTime.now().withHour(a.h).withMinute(a.m).withZoneSameInstant(ZoneId.of(ct.tz)); val hf = t.hour + t.minute / 60f
                Row(Modifier.animateItemPlacement().enter(i).fillMaxWidth().alpha(if (a.on) 1f else .5f).background(cardC(d), RoundedCornerShape(24.dp))
                    .press({ Alarms.cancel(c, a.id); alarms.remove(a); Alarms.save(c, alarms.toList()) }) { alarms[alarms.indexOf(a)] = a.copy(on = !a.on); Alarms.save(c, alarms.toList()) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))) { val (s1, s2) = sky(hf); drawRect(Brush.verticalGradient(listOf(s1, s2))); scene(hf, S.ink, hf < 6f || hf >= 18.5f) }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(ct.name, fontSize = 17.sp, color = fgC(d)); Text(a.label, fontSize = 12.sp, color = dimC(d), maxLines = 1) }
                    Text(hm(t.hour, t.minute), fontSize = 34.sp, letterSpacing = (-1).sp, color = fgC(d)) } }
        }
    }
}

@Composable fun AddAlarm(alarms: MutableList<AlarmItem>, close: () -> Unit) {
    val c = LocalContext.current; val hp = LocalHapticFeedback.current; val dens = LocalDensity.current.density
    var city by remember { mutableStateOf(S.cities[S.sel.coerceIn(0, S.cities.size - 1)]) }; val z = ZoneId.of(cityOf(city).tz)
    var mins by remember { mutableStateOf(ZonedDateTime.now(z).let { (it.hour * 60 + it.minute) / 5 * 5 + 60 } % 1440) }
    var label by remember { mutableStateOf("") }; var days by remember { mutableStateOf(0) }; var dLabel by remember { mutableStateOf(false) }; var dCity by remember { mutableStateOf(false) }
    val now = ZonedDateTime.now(z); var target = now.withHour(mins / 60).withMinute(mins % 60).withSecond(0).withNano(0); if (!target.isAfter(now)) target = target.plusDays(1)
    val diff = Duration.between(now, target).toMinutes(); val mine = target.withZoneSameInstant(ZoneId.systemDefault())
    val (t0, b0) = sky(mins / 60f); val fg = if ((t0.luminance() + b0.luminance()) / 2 < 0.45f) Color.White else Color(0xFF111111); var acc by remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(t0, b0))).pointerInput(Unit) { detectVerticalDragGestures { _, dy -> acc -= dy; val st = 10 * dens
        while (acc >= st) { acc -= st; mins = (mins + 5) % 1440; hp.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        while (acc <= -st) { acc += st; mins = (mins + 1435) % 1440; hp.performHapticFeedback(HapticFeedbackType.TextHandleMove) } } }) {
        Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(380.dp).padding(bottom = 80.dp)) { scene(mins / 60f, S.ink, mins < 360 || mins >= 1110) }
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                Box(Modifier.align(Alignment.CenterStart).size(40.dp).background(fg.copy(.14f), CircleShape).press { close() }, Alignment.Center) { Text("✕", color = fg) }
                Text(label.ifBlank { "Add Label" }, color = fg, fontSize = 13.sp, modifier = Modifier.press { dLabel = true }.padding(12.dp)) }
            Row(Modifier.padding(top = 12.dp).press { pick(c, mins / 60, mins % 60) { h, m -> mins = h * 60 + m } }) {
                Column { for (x in listOf(hh(mins / 60), two(mins % 60))) Text(x, fontSize = 140.sp, lineHeight = 128.sp, letterSpacing = (-8).sp, fontWeight = FontWeight.Medium, color = fg) }
                Box(Modifier.padding(start = 14.dp, top = 14.dp).width(1.5.dp).height(150.dp).background(RED))
                Column(Modifier.padding(start = 12.dp, top = 14.dp)) { Text(target.format(DateTimeFormatter.ofPattern("EEE,\nd MMM")), fontSize = 22.sp, color = fg); Spacer(Modifier.height(40.dp)); Text("+${diff / 60}:${two((diff % 60).toInt())}", fontSize = 22.sp, color = fg.copy(.7f)) }
            }
            Text("$city ⌄", fontSize = 28.sp, color = fg, modifier = Modifier.press { dCity = true }.padding(vertical = 8.dp))
            Text("Your time: " + mine.format(DateTimeFormatter.ofPattern("EEE, MMM d")) + " · " + hm(mine.hour, mine.minute), fontSize = 12.sp, color = fg.copy(.7f))
            Row(Modifier.padding(top = 12.dp), Arrangement.spacedBy(8.dp)) { listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { i, l -> val on = (days shr i) and 1 == 1
                val bg by animateColorAsState(if (on) fg else Color.Transparent, label = "dy")
                Box(Modifier.size(34.dp).background(bg, CircleShape).border(1.dp, fg.copy(.5f), CircleShape).press { days = days xor (1 shl i) }, Alignment.Center) { Text(l, fontSize = 12.sp, color = if (on) (if (fg == Color.White) Color.Black else Color.White) else fg) } } }
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp).fillMaxWidth().height(54.dp).background(Color.White.copy(.92f), CircleShape).press {
            alarms.add(AlarmItem((System.currentTimeMillis() % 1000000).toInt() + 1, mine.hour, mine.minute, label.ifBlank { city }, true, city, days)); Alarms.save(c, alarms.toList()); close() }, Alignment.Center) { Text("Set Alarm", color = Color.Black, fontWeight = FontWeight.Medium) }
    }
    if (dLabel) AlertDialog({ dLabel = false }, confirmButton = { TextButton({ dLabel = false }) { Text("OK") } }, title = { Text("Label") }, text = { TextField(label, { label = it }, singleLine = true) })
    if (dCity) AlertDialog({ dCity = false }, confirmButton = {}, title = { Text("City") }, text = { LazyColumn { items(ALL) { ct -> Text(ct.name, Modifier.fillMaxWidth().press { city = ct.name; dCity = false }.padding(14.dp)) } } })
}

@Composable fun ToolsLayer(close: () -> Unit) {
    var tool by remember { mutableStateOf(-1) }
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().padding(top = 12.dp)) {
        Box(Modifier.padding(start = 20.dp).size(44.dp).background(Chip, CircleShape).press { if (tool == -1) close() else tool = -1 }, Alignment.Center) { Text("✕", color = Ink) }
        when (tool) {
            0 -> Box(Modifier.fillMaxSize().navigationBarsPadding()) { TimerScreen() }
            1 -> Box(Modifier.fillMaxSize().navigationBarsPadding()) { BedScreen() }
            else -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tools", fontSize = 36.sp, color = Ink, modifier = Modifier.padding(bottom = 12.dp)); Pill("Timer & Stopwatch") { tool = 0 }; Pill("Bedtime & Sleep sounds", false) { tool = 1 } }
        }
    }
}

package com.klock.app
import android.app.*
import android.app.KeyguardManager
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Ring { var on by mutableStateOf(false); var label by mutableStateOf("") }

class AlarmService : Service() {
    private var mp: MediaPlayer? = null
    private val h = Handler(Looper.getMainLooper())
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        when (i?.action) { "stop" -> { stopRing(); return START_NOT_STICKY }; "snooze" -> { Alarms.snooze(this, Ring.label); stopRing(); return START_NOT_STICKY } }
        val label = i?.getStringExtra("l").orEmpty().ifBlank { "Alarm" }; Ring.label = label; Ring.on = true
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("ring", "Alarm ringing", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) })
        fun act(a: String, code: Int) = PendingIntent.getService(this, code, Intent(this, AlarmService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val full = PendingIntent.getActivity(this, 5, Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        val ic: android.graphics.drawable.Icon? = null
        val n = Notification.Builder(this, "ring").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(label).setContentText("Alarm")
            .setCategory(Notification.CATEGORY_ALARM).setOngoing(true).setFullScreenIntent(full, true).setContentIntent(full)
            .addAction(Notification.Action.Builder(ic, "Snooze", act("snooze", 6)).build()).addAction(Notification.Action.Builder(ic, "Dismiss", act("stop", 7)).build()).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(1, n)
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mp = MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@AlarmService, uri); isLooping = true; prepare(); start() }
        } catch (e: Exception) { }
        getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
        h.postDelayed({ stopRing() }, 120_000)
        return START_NOT_STICKY
    }
    private fun stopRing() {
        h.removeCallbacksAndMessages(null); mp?.release(); mp = null; getSystemService(Vibrator::class.java).cancel()
        Ring.on = false; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() { mp?.release(); super.onDestroy() }
}

class AlarmActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setShowWhenLocked(true); setTurnScreenOn(true)
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        fun send(a: String) = startService(Intent(this, AlarmService::class.java).setAction(a))
        setContent { MaterialTheme(colorScheme = darkColorScheme()) {
            val on = Ring.on; LaunchedEffect(on) { if (!on) finish() }
            Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(LocalTime.now().format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(this@AlarmActivity)) "HH:mm" else "hh:mm a")), fontSize = 72.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text(Ring.label, fontSize = 22.sp, color = Dim, modifier = Modifier.padding(bottom = 48.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Pill("Snooze", false) { send("snooze") }; Pill("Dismiss") { send("stop") } }
            }
        } }
    }
}

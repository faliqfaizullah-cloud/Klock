package com.klock.app
import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
import java.util.TimeZone
class ClockWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = ids.forEach {
        val v = RemoteViews(c.packageName, R.layout.widget_clock)
        v.setTextViewText(R.id.city, TimeZone.getDefault().id.substringAfter('/').replace('_', ' '))
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        m.updateAppWidget(it, v)
    }
}

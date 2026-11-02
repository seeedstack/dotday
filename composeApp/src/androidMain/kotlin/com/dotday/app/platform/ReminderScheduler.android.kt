package com.dotday.app.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal const val CHANNEL_ID = "dotday_reminder"
internal const val EXTRA_HOUR = "com.dotday.app.reminder.hour"
internal const val EXTRA_MINUTE = "com.dotday.app.reminder.minute"
private const val REQUEST_CODE = 1001

actual fun scheduleDailyReminder(hour: Int, minute: Int) {
    val ctx = AppContextHolder.context
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = reminderPendingIntent(ctx, hour, minute)
    val triggerAt = nextTriggerMillis(hour, minute)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60 * 1000L, pi)
    } else {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }
}

actual fun cancelDailyReminder() {
    val ctx = AppContextHolder.context
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(reminderPendingIntent(ctx, 0, 0))
}

private fun reminderPendingIntent(ctx: Context, hour: Int, minute: Int): PendingIntent {
    val intent = Intent(ctx, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_HOUR, hour)
        putExtra(EXTRA_MINUTE, minute)
    }
    return PendingIntent.getBroadcast(ctx, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

private fun nextTriggerMillis(hour: Int, minute: Int): Long {
    val tz = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    var candidate = LocalDateTime(today, LocalTime(hour, minute)).toInstant(tz)
    if (candidate <= now) candidate = LocalDateTime(today.plus(1, DateTimeUnit.DAY), LocalTime(hour, minute)).toInstant(tz)
    return candidate.toEpochMilliseconds()
}

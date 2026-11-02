package com.dotday.app.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.awt.Color
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
private var job: Job? = null

// ponytail: no OS scheduler integration (launchd/Task Scheduler); the reminder only fires while
// this JVM process is running. Upgrade to a packaged background service if reliability while the
// app is closed matters.
actual fun scheduleDailyReminder(hour: Int, minute: Int) {
    job?.cancel()
    job = scope.launch {
        while (isActive) {
            delay(millisUntilNext(hour, minute))
            showTrayNotification()
        }
    }
}

actual fun cancelDailyReminder() {
    job?.cancel()
    job = null
}

private fun millisUntilNext(hour: Int, minute: Int): Long {
    val tz = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    var candidate = LocalDateTime(today, LocalTime(hour, minute)).toInstant(tz)
    if (candidate <= now) candidate = LocalDateTime(today.plus(1, DateTimeUnit.DAY), LocalTime(hour, minute)).toInstant(tz)
    return (candidate.toEpochMilliseconds() - now.toEpochMilliseconds()).coerceAtLeast(0L)
}

private fun showTrayNotification() {
    if (!SystemTray.isSupported()) return
    val tray = SystemTray.getSystemTray()
    val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        g.color = Color(0xE6, 0x39, 0x46)
        g.fillRect(0, 0, 16, 16)
        g.dispose()
    }
    val icon = TrayIcon(img, "DotDay")
    icon.isImageAutoSize = true
    tray.add(icon)
    icon.displayMessage("DotDay", "Time to check off today's habits!", TrayIcon.MessageType.INFO)
    scope.launch { delay(5000); runCatching { tray.remove(icon) } }
}

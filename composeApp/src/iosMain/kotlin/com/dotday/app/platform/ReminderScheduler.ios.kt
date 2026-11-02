package com.dotday.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

private const val REMINDER_ID = "dotday.daily.reminder"

@OptIn(ExperimentalForeignApi::class)
actual fun scheduleDailyReminder(hour: Int, minute: Int) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_ID))

    val content = UNMutableNotificationContent().apply {
        setTitle("DotDay")
        setBody("Time to check off today's habits!")
    }
    val dateComponents = NSDateComponents().apply {
        this.hour = hour.toLong()
        this.minute = minute.toLong()
    }
    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(dateComponents, repeats = true)
    val request = UNNotificationRequest.requestWithIdentifier(REMINDER_ID, content, trigger)
    center.addNotificationRequest(request, null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun cancelDailyReminder() {
    UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_ID))
}

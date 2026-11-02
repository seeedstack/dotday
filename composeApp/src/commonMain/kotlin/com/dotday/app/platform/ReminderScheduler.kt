package com.dotday.app.platform

/** Schedules (or re-arms) a daily local notification at the given clock time. */
expect fun scheduleDailyReminder(hour: Int, minute: Int)

/** Cancels any pending daily reminder. */
expect fun cancelDailyReminder()

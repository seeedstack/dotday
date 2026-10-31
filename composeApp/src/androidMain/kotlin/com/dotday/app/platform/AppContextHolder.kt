package com.dotday.app.platform

import android.content.Context

/** Set once from MainActivity.onCreate; the scheduler expect/actual has no Context parameter. */
object AppContextHolder {
    lateinit var context: Context
}

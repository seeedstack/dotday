package com.dotday.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberBackupExporter(): (String, String) -> String? = remember {
    val f: (String, String) -> String? = { _, text ->
        val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(vc, animated = true, completion = null)
        null
    }
    f
}

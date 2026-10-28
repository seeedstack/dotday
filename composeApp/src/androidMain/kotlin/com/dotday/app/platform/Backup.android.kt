package com.dotday.app.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupExporter(): (String, String) -> String? {
    val ctx = LocalContext.current
    return remember(ctx) {
        val f: (String, String) -> String? = { name, text ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, name)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            ctx.startActivity(Intent.createChooser(send, "Export backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        }
        f
    }
}

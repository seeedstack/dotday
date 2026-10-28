package com.dotday.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File

@Composable
actual fun rememberBackupExporter(): (String, String) -> String? = remember {
    val f: (String, String) -> String? = { name, text ->
        val file = File(System.getProperty("user.home"), name)
        file.writeText(text)
        "Backup saved to ${file.absolutePath}"
    }
    f
}

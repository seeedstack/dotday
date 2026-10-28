package com.dotday.app.platform

import androidx.compose.runtime.Composable

/** Returns a function (fileName, jsonText) -> optional toast message. */
@Composable
expect fun rememberBackupExporter(): (String, String) -> String?

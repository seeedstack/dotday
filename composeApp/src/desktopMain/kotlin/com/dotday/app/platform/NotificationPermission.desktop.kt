package com.dotday.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionRequester(): (onResult: (Boolean) -> Unit) -> Unit =
    remember { { onResult: (Boolean) -> Unit -> onResult(true) } }

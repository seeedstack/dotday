package com.dotday.app.platform

import androidx.compose.runtime.Composable

/** Returns a function that requests the OS notification permission, reporting granted/denied via the callback. */
@Composable
expect fun rememberNotificationPermissionRequester(): (onResult: (Boolean) -> Unit) -> Unit

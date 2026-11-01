package com.dotday.app.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsScreenTest {
    @Test fun entersEditModeWhenRequestedAndEnabled() =
        assertTrue(shouldEnterEditMode(requested = true, editingEnabled = true))

    @Test fun staysReadOnlyWhenRequestedButEditingDisabled() =
        assertFalse(shouldEnterEditMode(requested = true, editingEnabled = false))

    @Test fun staysReadOnlyWhenNotRequested() =
        assertFalse(shouldEnterEditMode(requested = false, editingEnabled = true))
}

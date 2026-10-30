package com.dotday.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class HabitsScreenTest {
    @Test fun snapsOpenPastHalfway() { assertEquals(-200f, swipeOpenTarget(-101f, 200f)) }
    @Test fun snapsClosedBeforeHalfway() { assertEquals(0f, swipeOpenTarget(-99f, 200f)) }
    @Test fun snapsOpenAtExactHalfway() { assertEquals(-200f, swipeOpenTarget(-100f, 200f)) }
    @Test fun snapsClosedWhenNotDragged() { assertEquals(0f, swipeOpenTarget(0f, 200f)) }
}

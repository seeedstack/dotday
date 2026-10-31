package com.dotday.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class HabitsScreenTest {
    @Test fun closedBelowOpenThreshold() =
        assertEquals(SwipeOutcome(0f, SwipeAction.None), swipeOutcome(50f, 300f, 80f, rtl = false))

    @Test fun snapsOpenRevealingDeleteOnSwipeRight() =
        assertEquals(SwipeOutcome(80f, SwipeAction.None), swipeOutcome(90f, 300f, 80f, rtl = false))

    @Test fun snapsOpenRevealingArchiveOnSwipeLeft() =
        assertEquals(SwipeOutcome(-80f, SwipeAction.None), swipeOutcome(-90f, 300f, 80f, rtl = false))

    @Test fun commitsDeleteOnFullSwipeRight() =
        assertEquals(SwipeOutcome(300f, SwipeAction.Delete), swipeOutcome(200f, 300f, 80f, rtl = false))

    @Test fun commitsArchiveOnFullSwipeLeft() =
        assertEquals(SwipeOutcome(-300f, SwipeAction.Archive), swipeOutcome(-200f, 300f, 80f, rtl = false))

    @Test fun mirrorsActionsInRtl() =
        assertEquals(SwipeOutcome(300f, SwipeAction.Archive), swipeOutcome(200f, 300f, 80f, rtl = true))

    @Test fun staysClosedWhenNotDragged() =
        assertEquals(SwipeOutcome(0f, SwipeAction.None), swipeOutcome(0f, 300f, 80f, rtl = false))

    @Test fun unmeasuredRowStaysClosed() =
        assertEquals(SwipeOutcome(0f, SwipeAction.None), swipeOutcome(200f, 0f, 80f, rtl = false))
}

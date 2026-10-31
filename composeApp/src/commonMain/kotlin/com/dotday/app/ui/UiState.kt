package com.dotday.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate

enum class Tab(val label: String) { Today("Today"), Habits("Habits"), Insights("Insights"), Settings("Settings") }

class Confirm(val msg: String, val yes: String, val onYes: () -> Unit)
class Burst(val text: String, val id: Int)
class SnackAction(val label: String, val onClick: () -> Unit)

class UiState {
    var tab by mutableStateOf(Tab.Today)
    var weekOff by mutableIntStateOf(0)
    var editing by mutableStateOf<String?>(null)      // null | "new" | habit id
    var toastMsg by mutableStateOf<String?>(null); private set
    var toastId by mutableIntStateOf(0); private set
    var toastAction by mutableStateOf<SnackAction?>(null); private set
    var confirm by mutableStateOf<Confirm?>(null)
    var burst by mutableStateOf<Burst?>(null)
    var stampNew by mutableStateOf<String?>(null)
    var selectedDate by mutableStateOf<LocalDate?>(null)   // null = follow today
    var swipedHabitId by mutableStateOf<String?>(null)     // id of the habit row with its swipe actions revealed

    private var burstSeq = 0

    fun toast(m: String?, action: SnackAction? = null) { if (m != null) { toastMsg = m; toastAction = action; toastId++ } }
    fun clearToast() { toastMsg = null; toastAction = null }
    fun fireBurst(t: String) { burst = Burst(t, ++burstSeq) }
    fun go(t: Tab, edit: String? = null) { tab = t; editing = edit }
}

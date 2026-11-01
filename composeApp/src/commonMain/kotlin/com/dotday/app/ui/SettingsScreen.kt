package com.dotday.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.domain.ActivityLevel
import com.dotday.app.domain.AVATARS
import com.dotday.app.domain.Copy
import com.dotday.app.domain.Focus
import com.dotday.app.domain.Habit
import com.dotday.app.domain.PAL
import com.dotday.app.domain.STARTER_HABITS
import com.dotday.app.domain.UnitSystem
import com.dotday.app.domain.UserProfile
import com.dotday.app.domain.cmToFtIn
import com.dotday.app.domain.isValidHeightCm
import com.dotday.app.domain.isValidName
import com.dotday.app.domain.isValidSchedule
import com.dotday.app.domain.isValidWeightKg
import com.dotday.app.domain.kgToLb
import com.dotday.app.domain.maxBirthYear
import com.dotday.app.domain.minBirthYear
import com.dotday.app.platform.rememberBackupExporter
import com.dotday.app.platform.scheduleDailyReminder
import com.dotday.app.sdk.InstalledAddon
import com.dotday.app.sdk.activeTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource

@Composable
fun SettingsScreen(store: AppStore, ui: UiState, today: LocalDate) {
    val export = rememberBackupExporter()
    Column {
        H2("Settings", modifier = Modifier.padding(bottom = 12.dp))
        store.profile?.let { ProfilePanel(store, ui, today, it) }
        Panel {
            FieldLabel("Appearance")
            val addonTheme = store.addons.activeTheme()
            val themeOptions = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark") +
                if (addonTheme != null) listOf("addon" to "Add-on theme") else emptyList()
            SelectField(themeOptions, store.theme) { store.updateTheme(it) }
            FieldLabel("Week starts on")
            SelectField(listOf("1" to "Monday", "0" to "Sunday"), store.weekStart.toString()) { store.updateWeekStart(it.toInt()) }
            Row(
                Modifier.padding(top = 10.dp).heightIn(min = 44.dp)
                    .clickable(role = Role.Checkbox) { store.updateCalm(!store.calm) },
                verticalAlignment = Alignment.CenterVertically,
            ) { Check(store.calm); Spacer(Modifier.width(8.dp)); T("Reduced effects") }
        }
        Panel {
            H2("Data", 20.sp)
            if (store.demo) Chip("DEMO DATA LOADED", Modifier.padding(top = 8.dp))
            FlowRow(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DButton("Export backup", kind = BtnKind.Alt) {
                    ui.toast(export("dotday-backup.json", store.exportJson()))
                }
                DButton("Delete all data", kind = BtnKind.Bad) {
                    ui.confirm = Confirm("Delete every habit and check-in on this device?", "Delete all") {
                        store.wipe(); ui.toast("All data deleted.")
                    }
                }
            }
        }
        Panel {
            H2("Add-ons", 20.sp)
            T(
                "Declarative packages only: themes, dashboard cards, text panels. No code runs — paste a manifest below.",
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            var pasted by remember { mutableStateOf("") }
            DField(pasted, { pasted = it }, maxLen = 8000)
            DButton("Install add-on", modifier = Modifier.padding(top = 8.dp)) {
                val result = store.installAddon(pasted)
                ui.toast(result)
                if (result.startsWith("Installed ")) pasted = ""
            }
            if (store.addons.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                store.addons.forEach { a -> AddonRow(store, ui, a) }
            }
        }
    }
}

private fun fmtHeight(cm: Float, unit: UnitSystem): String =
    if (unit == UnitSystem.METRIC) "${cm.toInt()} cm" else {
        val (ft, inch) = cmToFtIn(cm)
        "$ft'${inch.toInt()}\""
    }

private fun fmtWeight(kg: Float, unit: UnitSystem): String =
    if (unit == UnitSystem.METRIC) "${kg.toInt()} kg" else "${kgToLb(kg).toInt()} lb"

private fun fmtGender(g: com.dotday.app.domain.Gender): String = when (g) {
    com.dotday.app.domain.Gender.MALE -> "Male"; com.dotday.app.domain.Gender.FEMALE -> "Female"
    com.dotday.app.domain.Gender.NON_BINARY -> "Non-binary"; com.dotday.app.domain.Gender.PREFER_NOT_TO_SAY -> "Prefer not to say"
}

private fun addStarterHabits(store: AppStore, ui: UiState, today: LocalDate, focus: Focus, accentHex: String) {
    val missing = STARTER_HABITS.getValue(focus).filter { name -> store.habits.none { h -> h.name == name } }
    missing.forEach { name ->
        store.upsert(
            Habit(
                id = "h" + Clock.System.now().toEpochMilliseconds().toString(36) + name.hashCode().toString(36),
                name = name, category = focus.label, color = accentHex, days = (0..6).toList(), start = today,
            )
        )
    }
    if (missing.isNotEmpty()) ui.toast("Added ${missing.size} habit(s).")
}

@Composable
private fun ProfilePanel(store: AppStore, ui: UiState, today: LocalDate, p: UserProfile) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(editing) { mutableStateOf(p) }
    val valid = isValidName(draft.name) && isValidHeightCm(draft.heightCm) && isValidWeightKg(draft.weightKg) &&
        isValidSchedule(draft.wakeTime, draft.sleepTime)
    val dirty = draft != p
    val currentYear = today.year

    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            H2("Profile", 20.sp)
            if (editing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DButton("Cancel", kind = BtnKind.Alt) {
                        if (dirty) {
                            ui.confirm = Confirm(
                                "Discard changes?", "Discard", yesKind = BtnKind.Bad,
                                cancelLabel = "Keep editing", cancelKind = BtnKind.Primary,
                            ) { editing = false }
                        } else editing = false
                    }
                    DButton("Save!", enabled = valid && dirty) {
                        val focusChanged = draft.focus != p.focus
                        val reminderChanged = draft.reminderTime != p.reminderTime
                        val saved = draft
                        store.updateProfile { saved }
                        if (reminderChanged && saved.notificationsGranted) scheduleDailyReminder(saved.reminderTime.hour, saved.reminderTime.minute)
                        editing = false
                        ui.fireBurst("SAVED!")
                        if (focusChanged) {
                            ui.confirm = Confirm(
                                "New focus! Add ${saved.focus.label} starter habits?", "Add", yesKind = BtnKind.Primary,
                                cancelLabel = "No thanks", cancelKind = BtnKind.Alt,
                            ) { addStarterHabits(store, ui, today, saved.focus, saved.accentHex) }
                        }
                    }
                }
            } else {
                DButton("Edit", kind = BtnKind.Primary) { editing = true }
            }
        }

        if (!editing) {
            ProfileInfoRow("Avatar", contentDescription = "Avatar") {
                Image(
                    painterResource((AVATARS.firstOrNull { it.id == p.avatarId } ?: AVATARS.first()).drawable),
                    contentDescription = null, modifier = Modifier.size(40.dp),
                )
            }
            ProfileInfoRow("Name", p.name)
            ProfileInfoRow("Age", "${currentYear - p.birthYear} (born ${p.birthYear})")
            ProfileInfoRow("Gender", fmtGender(p.gender))
            ProfileInfoRow("Height", fmtHeight(p.heightCm, p.unitSystem))
            ProfileInfoRow("Weight", fmtWeight(p.weightKg, p.unitSystem))
            ProfileInfoRow("Activity level", Copy.ACTIVITY_COPY.getValue(p.activityLevel.name))
            ProfileInfoRow("Wake up", p.wakeTime.toString().take(5))
            ProfileInfoRow("Sleep", p.sleepTime.toString().take(5))
            ProfileInfoRow("Reminder time", p.reminderTime.toString().take(5))
            ProfileInfoRow("Main focus", contentDescription = "Main focus, ${p.focus.label}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FOCUS_ICONS[p.focus]?.let { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(6.dp))
                    T(p.focus.label, weight = FontWeight.SemiBold)
                }
            }
            val accentName = PAL.firstOrNull { it.first == p.accentHex }?.second ?: p.accentHex
            ProfileInfoRow("Accent color", contentDescription = "Accent color, $accentName") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).background(hex(p.accentHex), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    T(accentName, weight = FontWeight.SemiBold)
                }
            }
            ProfileInfoRow(
                "Notifications", if (p.notificationsGranted) "ON" else "OFF — you can turn it on later in system Settings",
            )

            val missing = STARTER_HABITS.getValue(p.focus).filter { name -> store.habits.none { h -> h.name == name } }
            if (missing.isNotEmpty()) {
                DButton("Add ${p.focus.label} starter habits", kind = BtnKind.Alt, modifier = Modifier.padding(top = 8.dp)) {
                    addStarterHabits(store, ui, today, p.focus, p.accentHex)
                }
            }
        } else {
            FieldLabel("Name")
            DField(draft.name, { n -> if (n.length <= 24) draft = draft.copy(name = n) }, maxLen = 24)

            FieldLabel("Birth year")
            WheelPicker(minBirthYear(currentYear)..maxBirthYear(currentYear), draft.birthYear, { y -> draft = draft.copy(birthYear = y) })

            TilePicker(UnitSystem.entries, draft.unitSystem, { u -> draft = draft.copy(unitSystem = u) }, Modifier.padding(top = 8.dp)) { u, sel ->
                T(if (u == UnitSystem.METRIC) "Metric (cm/kg)" else "Imperial (ft/lb)", weight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }

            FieldLabel("Height (cm)")
            WheelPicker(100..250, draft.heightCm.toInt(), { h -> draft = draft.copy(heightCm = h.toFloat()) }) { "$it cm" }

            FieldLabel("Weight (kg)")
            WheelPicker(30..300, draft.weightKg.toInt(), { w -> draft = draft.copy(weightKg = w.toFloat()) }) { "$it kg" }

            FieldLabel("Activity level")
            TilePicker(ActivityLevel.entries, draft.activityLevel, { lvl -> draft = draft.copy(activityLevel = lvl) }) { lvl, sel ->
                T(Copy.ACTIVITY_COPY.getValue(lvl.name), weight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }

            FieldLabel("Wake up")
            TimeWheelPicker(draft.wakeTime, { t -> draft = draft.copy(wakeTime = t) })
            FieldLabel("Sleep")
            TimeWheelPicker(draft.sleepTime, { t -> draft = draft.copy(sleepTime = t) })
            if (draft.wakeTime == draft.sleepTime) ErrText("Wake and sleep times should differ.")
            FieldLabel("Reminder time")
            TimeWheelPicker(draft.reminderTime, { t -> draft = draft.copy(reminderTime = t) })

            FieldLabel("Avatar")
            TilePicker(AVATARS, AVATARS.firstOrNull { it.id == draft.avatarId } ?: AVATARS.first(), { a -> draft = draft.copy(avatarId = a.id) }) { a, _ ->
                Image(painterResource(a.drawable), contentDescription = a.id, modifier = Modifier.size(32.dp))
            }

            FieldLabel("Accent color")
            TilePicker(PAL, PAL.firstOrNull { it.first == draft.accentHex } ?: PAL.first(), { c -> draft = draft.copy(accentHex = c.first) }) { (hexStr, name), _ ->
                Box(Modifier.size(28.dp).background(hex(hexStr), CircleShape).semantics { contentDescription = name })
            }

            FieldLabel("Main focus")
            TilePicker(Focus.entries, draft.focus, { f -> draft = draft.copy(focus = f) }) { f, sel ->
                T(f.label, weight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }

            FieldLabel("Notifications")
            if (draft.notificationsGranted) {
                T("ON")
            } else {
                val requestPermission = com.dotday.app.platform.rememberNotificationPermissionRequester()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T("OFF", modifier = Modifier.padding(end = 8.dp))
                    DButton("Allow reminders", kind = BtnKind.Alt) {
                        requestPermission { granted -> draft = draft.copy(notificationsGranted = granted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonRow(store: AppStore, ui: UiState, addon: InstalledAddon) {
    val m = addon.manifest
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(enabled = m.type == "navigation") { expanded = !expanded }) {
                T("${m.name} v${m.version}", weight = androidx.compose.ui.text.font.FontWeight.Bold)
                T("${m.type} · ${m.id}", 13.sp)
            }
            DButton(if (addon.enabled) "Disable" else "Enable", kind = BtnKind.Alt) {
                store.setAddonEnabled(m.id, !addon.enabled)
            }
            Spacer(Modifier.width(6.dp))
            DButton("Uninstall", kind = BtnKind.Bad) {
                ui.confirm = Confirm("Uninstall \"${m.name}\"? Its settings will be deleted.", "Uninstall") {
                    store.uninstallAddon(m.id); ui.toast("Uninstalled ${m.name}.")
                }
            }
        }
        if (expanded && m.navEntry != null) {
            Column(Modifier.padding(top = 6.dp)) {
                H2(m.navEntry.label, 16.sp)
                T(m.navEntry.text, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

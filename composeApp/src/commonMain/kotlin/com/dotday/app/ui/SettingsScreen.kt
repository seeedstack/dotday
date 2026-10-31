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
import com.dotday.app.domain.UserProfile
import com.dotday.app.domain.isValidHeightCm
import com.dotday.app.domain.isValidName
import com.dotday.app.domain.isValidWeightKg
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

@Composable
private fun ProfilePanel(store: AppStore, ui: UiState, today: LocalDate, p: UserProfile) {
    Panel {
        H2("Profile", 20.sp)
        FieldLabel("Name")
        DField(p.name, { n -> if (isValidName(n) || n.isEmpty()) store.updateProfile { it.copy(name = n) } }, maxLen = 24)

        FieldLabel("Birth year")
        val currentYear = today.year
        WheelPicker(
            minBirthYear(currentYear)..maxBirthYear(currentYear), p.birthYear,
            { y -> store.updateProfile { it.copy(birthYear = y) } },
        )

        FieldLabel("Height (cm)")
        WheelPicker(100..250, p.heightCm.toInt(), { h -> if (isValidHeightCm(h.toFloat())) store.updateProfile { it.copy(heightCm = h.toFloat()) } }) { "$it cm" }

        FieldLabel("Weight (kg)")
        WheelPicker(30..300, p.weightKg.toInt(), { w -> if (isValidWeightKg(w.toFloat())) store.updateProfile { it.copy(weightKg = w.toFloat()) } }) { "$it kg" }

        FieldLabel("Activity level")
        TilePicker(ActivityLevel.entries, p.activityLevel, { lvl -> store.updateProfile { it.copy(activityLevel = lvl) } }) { lvl, sel ->
            T(Copy.ACTIVITY_COPY.getValue(lvl.name), weight = if (sel) FontWeight.Bold else FontWeight.Normal)
        }

        FieldLabel("Wake up")
        TimeWheelPicker(p.wakeTime, { t -> store.updateProfile { it.copy(wakeTime = t) } })
        FieldLabel("Sleep")
        TimeWheelPicker(p.sleepTime, { t -> store.updateProfile { it.copy(sleepTime = t) } })
        FieldLabel("Reminder time")
        TimeWheelPicker(p.reminderTime, { t ->
            store.updateProfile { it.copy(reminderTime = t) }
            if (p.notificationsGranted) scheduleDailyReminder(t.hour, t.minute)
        })

        FieldLabel("Avatar")
        TilePicker(AVATARS, AVATARS.first { it.id == p.avatarId }, { a -> store.updateProfile { it.copy(avatarId = a.id) } }) { a, _ ->
            Image(painterResource(a.drawable), contentDescription = a.id, modifier = Modifier.size(32.dp))
        }

        FieldLabel("Accent color")
        TilePicker(PAL, PAL.first { it.first == p.accentHex }, { c -> store.updateProfile { it.copy(accentHex = c.first) } }) { (hexStr, name), _ ->
            Box(Modifier.size(28.dp).background(hex(hexStr), CircleShape).semantics { contentDescription = name })
        }

        FieldLabel("Main focus")
        TilePicker(Focus.entries, p.focus, { f -> store.updateProfile { it.copy(focus = f) } }) { f, sel ->
            T(f.label, weight = if (sel) FontWeight.Bold else FontWeight.Normal)
        }
        val missing = STARTER_HABITS.getValue(p.focus).filter { name -> store.habits.none { h -> h.name == name } }
        if (missing.isNotEmpty()) {
            DButton("Add ${p.focus.label} starter habits", kind = BtnKind.Alt, modifier = Modifier.padding(top = 8.dp)) {
                missing.forEach { name ->
                    store.upsert(
                        Habit(
                            id = "h" + Clock.System.now().toEpochMilliseconds().toString(36) + name.hashCode().toString(36),
                            name = name, category = p.focus.label, color = p.accentHex, days = (0..6).toList(), start = today,
                        )
                    )
                }
                ui.toast("Added ${missing.size} habit(s).")
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

package com.dotday.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotday.app.data.AppStore
import com.dotday.app.platform.rememberBackupExporter
import com.dotday.app.sdk.InstalledAddon
import com.dotday.app.sdk.activeTheme
import kotlinx.datetime.LocalDate

@Composable
fun SettingsScreen(store: AppStore, ui: UiState, today: LocalDate) {
    val export = rememberBackupExporter()
    Column {
        H2("Settings", modifier = Modifier.padding(bottom = 12.dp))
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

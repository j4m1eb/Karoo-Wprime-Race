package com.j4m1eb.wprimerace.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.j4m1eb.wprimerace.extension.WPrimeModelType
import com.j4m1eb.wprimerace.settings.WPrimeRaceConfig
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settings: WPrimeRaceSettings = koinInject(),
) {
    val config by settings.configFlow.collectAsState(initial = WPrimeRaceConfig())
    val scope = rememberCoroutineScope()

    // Local editable state — seeded from persisted config
    var cpText by remember(config.criticalPower) { mutableStateOf(config.criticalPower.toInt().toString()) }
    var wPrimeText by remember(config.anaerobicCapacityKJ) { mutableStateOf(config.anaerobicCapacityKJ.toString()) }
    var ttText by remember(config.ttDurationMin) { mutableStateOf(config.ttDurationMin.toString()) }
    var critText by remember(config.critDurationMin) { mutableStateOf(config.critDurationMin.toString()) }
    var model by remember(config.modelType) { mutableStateOf(config.modelType) }
    var showArrow by remember(config.showArrow) { mutableStateOf(config.showArrow) }
    var showKjTT by remember(config.showKjTT) { mutableStateOf(config.showKjTT) }
    var showKjCrit by remember(config.showKjCrit) { mutableStateOf(config.showKjCrit) }
    var showKjUsable by remember(config.showKjUsable) { mutableStateOf(config.showKjUsable) }

    var savedVisible by remember { mutableStateOf(false) }

    fun save() {
        val cp = cpText.toDoubleOrNull() ?: return
        val wPrime = wPrimeText.toDoubleOrNull() ?: return
        val tt = ttText.toDoubleOrNull() ?: return
        val crit = critText.toDoubleOrNull() ?: return
        scope.launch {
            settings.save(
                WPrimeRaceConfig(
                    criticalPower = cp,
                    anaerobicCapacityKJ = wPrime,
                    ttDurationMin = tt,
                    critDurationMin = crit,
                    modelType = model,
                    showArrow = showArrow,
                    showKjTT = showKjTT,
                    showKjCrit = showKjCrit,
                    showKjUsable = showKjUsable,
                )
            )
            savedVisible = true
            delay(2000L)
            savedVisible = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("W' Race Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── W' Parameters ──────────────────────────────────────────────
            SectionHeader("W\u2032 Parameters")

            ModelDropdown(selected = model, onSelected = { model = it })

            NumericField(
                label = "Critical Power (CP)",
                value = cpText,
                unit = "W",
                onValueChange = { cpText = it },
            )
            NumericField(
                label = "Anaerobic Capacity (W\u2032)",
                value = wPrimeText,
                unit = "kJ",
                onValueChange = { wPrimeText = it },
            )

            // ── Time Trial ─────────────────────────────────────────────────
            SectionHeader("Time Trial")

            NumericField(
                label = "TT Duration",
                value = ttText,
                unit = "min",
                onValueChange = { ttText = it },
                hint = "e.g. 21.5 for a 10-mile TT",
            )

            // ── Criterium ──────────────────────────────────────────────────
            SectionHeader("Criterium")

            NumericField(
                label = "Crit Duration",
                value = critText,
                unit = "min",
                onValueChange = { critText = it },
                hint = "Total race duration",
            )

            // ── Display ────────────────────────────────────────────────────
            SectionHeader("Display")

            ToggleCard(
                title = "Show Trend Arrow",
                description = "Arrow indicates W\u2032 recovering (up) or depleting (down)",
                checked = showArrow,
                onCheckedChange = { showArrow = it },
            )
            ToggleCard(
                title = "W\u2032 TT — show kJ",
                description = "Display TT field values in kJ instead of %",
                checked = showKjTT,
                onCheckedChange = { showKjTT = it },
            )
            ToggleCard(
                title = "W\u2032 Crit — show kJ",
                description = "Display Crit field values in kJ instead of %",
                checked = showKjCrit,
                onCheckedChange = { showKjCrit = it },
            )
            ToggleCard(
                title = "W\u2032 Usable — show kJ",
                description = "Display Usable W\u2032 in kJ instead of %",
                checked = showKjUsable,
                onCheckedChange = { showKjUsable = it },
            )

            // ── Save ───────────────────────────────────────────────────────
            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(
                visible = savedVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = "Saved",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun NumericField(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            suffix = { Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    selected: WPrimeModelType,
    onSelected: (WPrimeModelType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextField(
                value = formatModel(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text("W\u2032 Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                WPrimeModelType.entries.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(formatModel(m)) },
                        onClick = { onSelected(m); expanded = false },
                    )
                }
            }
        }
    }
}

private fun formatModel(m: WPrimeModelType) = when (m) {
    WPrimeModelType.SKIBA_DIFFERENTIAL -> "Skiba Differential (2014)"
    WPrimeModelType.SKIBA_2012 -> "Skiba 2012"
    WPrimeModelType.BARTRAM -> "Bartram 2018"
    WPrimeModelType.CHORLEY -> "Chorley 2023 (Bi-Exp)"
}

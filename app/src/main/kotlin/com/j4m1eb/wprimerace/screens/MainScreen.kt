package com.j4m1eb.wprimerace.screens

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.j4m1eb.wprimerace.extension.WPrimeModelType
import com.j4m1eb.wprimerace.settings.CritConfig
import com.j4m1eb.wprimerace.settings.CritCurvePoint
import com.j4m1eb.wprimerace.settings.TtConfig
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
    var ttCpText by remember(config.tt.criticalPower) { mutableStateOf(config.tt.criticalPower.toInt().toString()) }
    var ttWPrimeText by remember(config.tt.anaerobicCapacityKJ) { mutableStateOf(config.tt.anaerobicCapacityKJ.toString()) }
    var ttText by remember(config.tt.durationMin) { mutableStateOf(config.tt.durationMin.toString()) }
    var critCpText by remember(config.crit.criticalPower) { mutableStateOf(config.crit.criticalPower.toInt().toString()) }
    var critWPrimeText by remember(config.crit.anaerobicCapacityKJ) { mutableStateOf(config.crit.anaerobicCapacityKJ.toString()) }
    var critText by remember(config.crit.durationMin) { mutableStateOf(config.crit.durationMin.toString()) }
    var model by remember(config.modelType) { mutableStateOf(config.modelType) }
    var showArrow by remember(config.showArrow) { mutableStateOf(config.showArrow) }
    var showKjTT by remember(config.showKjTT) { mutableStateOf(config.showKjTT) }
    var showKjCrit by remember(config.showKjCrit) { mutableStateOf(config.showKjCrit) }
    var showKjUsable by remember(config.showKjUsable) { mutableStateOf(config.showKjUsable) }

    // Crit curve — 4 editable interior points, stored as text pairs
    var curveRace   by remember(config.crit.curve) { mutableStateOf(config.crit.curve.map { it.racePct.toInt().toString() }) }
    var curveWPrime by remember(config.crit.curve) { mutableStateOf(config.crit.curve.map { it.wPrimePct.toInt().toString() }) }

    var savedVisible by remember { mutableStateOf(false) }

    fun parseCurve(): List<CritCurvePoint>? {
        return (0 until 4).map { i ->
            val r = curveRace[i].toDoubleOrNull() ?: return null
            val w = curveWPrime[i].toDoubleOrNull() ?: return null
            CritCurvePoint(r, w)
        }
    }

    fun resetCurve() {
        val def = WPrimeRaceConfig.DEFAULT_CRIT_CURVE
        curveRace   = def.map { it.racePct.toInt().toString() }
        curveWPrime = def.map { it.wPrimePct.toInt().toString() }
    }

    fun save() {
        val ttCp = ttCpText.toDoubleOrNull() ?: return
        val ttWPrime = ttWPrimeText.toDoubleOrNull() ?: return
        val tt = ttText.toDoubleOrNull() ?: return
        val critCp = critCpText.toDoubleOrNull() ?: return
        val critWPrime = critWPrimeText.toDoubleOrNull() ?: return
        val crit = critText.toDoubleOrNull() ?: return
        val curve = parseCurve() ?: return
        scope.launch {
            settings.save(
                WPrimeRaceConfig(
                    tt = TtConfig(
                        criticalPower = ttCp,
                        anaerobicCapacityKJ = ttWPrime,
                        durationMin = tt,
                    ),
                    crit = CritConfig(
                        criticalPower = critCp,
                        anaerobicCapacityKJ = critWPrime,
                        durationMin = crit,
                        curve = curve,
                    ),
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

            // ── Model ──────────────────────────────────────────────────────
            SectionHeader("W\u2032 Model")

            ModelDropdown(selected = model, onSelected = { model = it })

            // ── Time Trial ─────────────────────────────────────────────────
            SectionHeader("Time Trial")

            NumericField(
                label = "TT Critical Power (CP)",
                value = ttCpText,
                unit = "W",
                onValueChange = { ttCpText = it },
            )
            NumericField(
                label = "TT W\u2032 Budget",
                value = ttWPrimeText,
                unit = "kJ",
                onValueChange = { ttWPrimeText = it },
            )
            NumericField(
                label = "TT Duration",
                value = ttText,
                unit = "min",
                onValueChange = { ttText = it },
                hint = "Use your TT pacing-budget values, e.g. CP 240 W / W\u2032 12 kJ for 65 min",
            )

            // ── Criterium ──────────────────────────────────────────────────
            SectionHeader("Criterium")

            NumericField(
                label = "Crit Critical Power (CP)",
                value = critCpText,
                unit = "W",
                onValueChange = { critCpText = it },
            )
            NumericField(
                label = "Crit Anaerobic Capacity (W\u2032)",
                value = critWPrimeText,
                unit = "kJ",
                onValueChange = { critWPrimeText = it },
            )
            NumericField(
                label = "Crit Duration",
                value = critText,
                unit = "min",
                onValueChange = { critText = it },
                hint = "Total race duration",
            )

            CritCurveEditor(
                raceValues      = curveRace,
                wPrimeValues    = curveWPrime,
                critDurationMin = critText.toDoubleOrNull() ?: 60.0,
                onRaceChange   = { i, v -> curveRace   = curveRace.toMutableList().also   { l -> l[i] = v } },
                onWPrimeChange = { i, v -> curveWPrime = curveWPrime.toMutableList().also { l -> l[i] = v } },
                onReset = ::resetCurve,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (savedVisible) Color(0xFF109C77) else MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    if (savedVisible) "Saved ✓" else "Save",
                    fontWeight = FontWeight.SemiBold,
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

@Composable
private fun CritCurveEditor(
    raceValues: List<String>,
    wPrimeValues: List<String>,
    critDurationMin: Double,
    onRaceChange: (Int, String) -> Unit,
    onWPrimeChange: (Int, String) -> Unit,
    onReset: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pacing Curve",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(
                    onClick = onReset,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text("Reset to Default", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Column headers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Race %",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "W\u2032 floor %",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            // Fixed start row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "0%  (start)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
                Text(
                    text = "100%  (fixed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
            }

            // 4 editable rows
            for (i in 0 until 4) {
                val racePct = raceValues[i].toDoubleOrNull()
                val computedMin = if (racePct != null) (racePct / 100.0 * critDurationMin) else null
                val minLabel = if (computedMin != null) "%.0f min".format(computedMin) else ""

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = raceValues[i],
                            onValueChange = { onRaceChange(i, it) },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        )
                        if (minLabel.isNotEmpty()) {
                            Text(
                                text = minLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = wPrimeValues[i],
                        onValueChange = { onWPrimeChange(i, it) },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }

            // Fixed end row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "100%  (finish)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
                Text(
                    text = "0%  (fixed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
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

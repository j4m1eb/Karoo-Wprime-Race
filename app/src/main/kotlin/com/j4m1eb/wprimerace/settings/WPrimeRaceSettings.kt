package com.j4m1eb.wprimerace.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.j4m1eb.wprimerace.extension.WPrimeModelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wprime_race_settings")

data class CritCurvePoint(val racePct: Double, val wPrimePct: Double)

data class WPrimeRaceConfig(
    val criticalPower: Double = 250.0,
    val anaerobicCapacityKJ: Double = 20.0,    // stored and displayed in kJ; converted to J internally
    val ttDurationMin: Double = 21.0,
    val critDurationMin: Double = 60.0,
    val modelType: WPrimeModelType = WPrimeModelType.SKIBA_DIFFERENTIAL,
    val showArrow: Boolean = true,
    // kJ display toggles — default off (% mode)
    val showKjTT: Boolean = false,
    val showKjCrit: Boolean = false,
    val showKjUsable: Boolean = false,
    // Crit pacing curve — 4 interior breakpoints (start 0→100% and end 100→0% are always fixed)
    val critCurve: List<CritCurvePoint> = DEFAULT_CRIT_CURVE,
) {
    val anaerobicCapacityJ: Double get() = anaerobicCapacityKJ * 1000.0

    companion object {
        // Default curve:
        //   0–43%  hold 70%  (opening, conserve)
        //  43–71%  hold 50%  (mid-race)
        //  71–97%  hold 30%  (build)
        //  97–100% empty the tank
        val DEFAULT_CRIT_CURVE = listOf(
            CritCurvePoint(racePct = 43.0, wPrimePct = 70.0),
            CritCurvePoint(racePct = 71.0, wPrimePct = 50.0),
            CritCurvePoint(racePct = 97.0, wPrimePct = 30.0),
            CritCurvePoint(racePct = 99.0, wPrimePct = 10.0),  // steep final drop
        )
    }
}

class WPrimeRaceSettings(private val context: Context) {

    companion object {
        private val KEY_CP = doublePreferencesKey("cp")
        private val KEY_WPRIME_KJ = doublePreferencesKey("wprime_kj")
        private val KEY_TT_DURATION = doublePreferencesKey("tt_duration_min")
        private val KEY_CRIT_DURATION = doublePreferencesKey("crit_duration_min")
        private val KEY_MODEL = stringPreferencesKey("model_type")
        private val KEY_SHOW_ARROW = booleanPreferencesKey("show_arrow")
        private val KEY_SHOW_KJ_TT = booleanPreferencesKey("show_kj_tt")
        private val KEY_SHOW_KJ_CRIT = booleanPreferencesKey("show_kj_crit")
        private val KEY_SHOW_KJ_USABLE = booleanPreferencesKey("show_kj_usable")
        // Crit curve — 4 editable interior points
        private val KEY_C1_RACE   = doublePreferencesKey("curve_1_race")
        private val KEY_C1_WPRIME = doublePreferencesKey("curve_1_wprime")
        private val KEY_C2_RACE   = doublePreferencesKey("curve_2_race")
        private val KEY_C2_WPRIME = doublePreferencesKey("curve_2_wprime")
        private val KEY_C3_RACE   = doublePreferencesKey("curve_3_race")
        private val KEY_C3_WPRIME = doublePreferencesKey("curve_3_wprime")
        private val KEY_C4_RACE   = doublePreferencesKey("curve_4_race")
        private val KEY_C4_WPRIME = doublePreferencesKey("curve_4_wprime")
    }

    val configFlow: Flow<WPrimeRaceConfig> = context.dataStore.data.map { prefs ->
        WPrimeRaceConfig(
            criticalPower = prefs[KEY_CP] ?: 250.0,
            anaerobicCapacityKJ = prefs[KEY_WPRIME_KJ] ?: 20.0,
            ttDurationMin = prefs[KEY_TT_DURATION] ?: 21.0,
            critDurationMin = prefs[KEY_CRIT_DURATION] ?: 60.0,
            modelType = WPrimeModelType.valueOf(prefs[KEY_MODEL] ?: WPrimeModelType.SKIBA_DIFFERENTIAL.name),
            showArrow = prefs[KEY_SHOW_ARROW] ?: true,
            showKjTT = prefs[KEY_SHOW_KJ_TT] ?: false,
            showKjCrit = prefs[KEY_SHOW_KJ_CRIT] ?: false,
            showKjUsable = prefs[KEY_SHOW_KJ_USABLE] ?: false,
            critCurve = listOf(
                CritCurvePoint(
                    racePct   = prefs[KEY_C1_RACE]   ?: 43.0,
                    wPrimePct = prefs[KEY_C1_WPRIME] ?: 70.0,
                ),
                CritCurvePoint(
                    racePct   = prefs[KEY_C2_RACE]   ?: 71.0,
                    wPrimePct = prefs[KEY_C2_WPRIME] ?: 50.0,
                ),
                CritCurvePoint(
                    racePct   = prefs[KEY_C3_RACE]   ?: 97.0,
                    wPrimePct = prefs[KEY_C3_WPRIME] ?: 30.0,
                ),
                CritCurvePoint(
                    racePct   = prefs[KEY_C4_RACE]   ?: 99.0,
                    wPrimePct = prefs[KEY_C4_WPRIME] ?: 10.0,
                ),
            ),
        )
    }.distinctUntilChanged()

    suspend fun save(config: WPrimeRaceConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CP] = config.criticalPower
            prefs[KEY_WPRIME_KJ] = config.anaerobicCapacityKJ
            prefs[KEY_TT_DURATION] = config.ttDurationMin
            prefs[KEY_CRIT_DURATION] = config.critDurationMin
            prefs[KEY_MODEL] = config.modelType.name
            prefs[KEY_SHOW_ARROW] = config.showArrow
            prefs[KEY_SHOW_KJ_TT] = config.showKjTT
            prefs[KEY_SHOW_KJ_CRIT] = config.showKjCrit
            prefs[KEY_SHOW_KJ_USABLE] = config.showKjUsable
            prefs[KEY_C1_RACE]   = config.critCurve[0].racePct
            prefs[KEY_C1_WPRIME] = config.critCurve[0].wPrimePct
            prefs[KEY_C2_RACE]   = config.critCurve[1].racePct
            prefs[KEY_C2_WPRIME] = config.critCurve[1].wPrimePct
            prefs[KEY_C3_RACE]   = config.critCurve[2].racePct
            prefs[KEY_C3_WPRIME] = config.critCurve[2].wPrimePct
            prefs[KEY_C4_RACE]   = config.critCurve[3].racePct
            prefs[KEY_C4_WPRIME] = config.critCurve[3].wPrimePct
        }
    }
}

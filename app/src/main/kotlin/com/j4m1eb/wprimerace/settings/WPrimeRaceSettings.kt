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

data class TtConfig(
    val criticalPower: Double = 240.0,
    val anaerobicCapacityKJ: Double = 12.0,
    val durationMin: Double = 65.0,
) {
    val anaerobicCapacityJ: Double get() = anaerobicCapacityKJ * 1000.0
}

data class CritConfig(
    val criticalPower: Double = 250.0,
    val anaerobicCapacityKJ: Double = 20.0,
    val durationMin: Double = 60.0,
    val curve: List<CritCurvePoint> = WPrimeRaceConfig.DEFAULT_CRIT_CURVE,
) {
    val anaerobicCapacityJ: Double get() = anaerobicCapacityKJ * 1000.0
}

data class WPrimeRaceConfig(
    val tt: TtConfig = TtConfig(),
    val crit: CritConfig = CritConfig(),
    val modelType: WPrimeModelType = WPrimeModelType.SKIBA_DIFFERENTIAL,
    val showArrow: Boolean = true,
    // kJ display toggles — default off (% mode)
    val showKjTT: Boolean = false,
    val showKjCrit: Boolean = false,
    val showKjUsable: Boolean = false,
) {
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
        private val KEY_TT_CP = doublePreferencesKey("tt_cp")
        private val KEY_TT_WPRIME_KJ = doublePreferencesKey("tt_wprime_kj")
        private val KEY_TT_DURATION_V2 = doublePreferencesKey("tt_duration_min_v2")
        private val KEY_CRIT_CP = doublePreferencesKey("crit_cp")
        private val KEY_CRIT_WPRIME_KJ = doublePreferencesKey("crit_wprime_kj")
        private val KEY_CRIT_DURATION_V2 = doublePreferencesKey("crit_duration_min_v2")
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
        val legacyCp = prefs[KEY_CP]
        val legacyWPrime = prefs[KEY_WPRIME_KJ]
        val legacyTtDuration = prefs[KEY_TT_DURATION]
        val legacyCritDuration = prefs[KEY_CRIT_DURATION]
        val critCurve = listOf(
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
        )

        WPrimeRaceConfig(
            tt = TtConfig(
                criticalPower = prefs[KEY_TT_CP] ?: legacyCp ?: 240.0,
                anaerobicCapacityKJ = prefs[KEY_TT_WPRIME_KJ] ?: legacyWPrime ?: 12.0,
                durationMin = prefs[KEY_TT_DURATION_V2] ?: legacyTtDuration ?: 65.0,
            ),
            crit = CritConfig(
                criticalPower = prefs[KEY_CRIT_CP] ?: legacyCp ?: 250.0,
                anaerobicCapacityKJ = prefs[KEY_CRIT_WPRIME_KJ] ?: legacyWPrime ?: 20.0,
                durationMin = prefs[KEY_CRIT_DURATION_V2] ?: legacyCritDuration ?: 60.0,
                curve = critCurve,
            ),
            modelType = WPrimeModelType.valueOf(prefs[KEY_MODEL] ?: WPrimeModelType.SKIBA_DIFFERENTIAL.name),
            showArrow = prefs[KEY_SHOW_ARROW] ?: true,
            showKjTT = prefs[KEY_SHOW_KJ_TT] ?: false,
            showKjCrit = prefs[KEY_SHOW_KJ_CRIT] ?: false,
            showKjUsable = prefs[KEY_SHOW_KJ_USABLE] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun save(config: WPrimeRaceConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TT_CP] = config.tt.criticalPower
            prefs[KEY_TT_WPRIME_KJ] = config.tt.anaerobicCapacityKJ
            prefs[KEY_TT_DURATION_V2] = config.tt.durationMin
            prefs[KEY_CRIT_CP] = config.crit.criticalPower
            prefs[KEY_CRIT_WPRIME_KJ] = config.crit.anaerobicCapacityKJ
            prefs[KEY_CRIT_DURATION_V2] = config.crit.durationMin
            prefs[KEY_MODEL] = config.modelType.name
            prefs[KEY_SHOW_ARROW] = config.showArrow
            prefs[KEY_SHOW_KJ_TT] = config.showKjTT
            prefs[KEY_SHOW_KJ_CRIT] = config.showKjCrit
            prefs[KEY_SHOW_KJ_USABLE] = config.showKjUsable
            prefs[KEY_C1_RACE]   = config.crit.curve[0].racePct
            prefs[KEY_C1_WPRIME] = config.crit.curve[0].wPrimePct
            prefs[KEY_C2_RACE]   = config.crit.curve[1].racePct
            prefs[KEY_C2_WPRIME] = config.crit.curve[1].wPrimePct
            prefs[KEY_C3_RACE]   = config.crit.curve[2].racePct
            prefs[KEY_C3_WPRIME] = config.crit.curve[2].wPrimePct
            prefs[KEY_C4_RACE]   = config.crit.curve[3].racePct
            prefs[KEY_C4_WPRIME] = config.crit.curve[3].wPrimePct
        }
    }
}

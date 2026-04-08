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
) {
    val anaerobicCapacityJ: Double get() = anaerobicCapacityKJ * 1000.0
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
        }
    }
}

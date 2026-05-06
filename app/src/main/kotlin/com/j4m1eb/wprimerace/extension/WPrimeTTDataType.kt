package com.j4m1eb.wprimerace.extension

import com.j4m1eb.wprimerace.settings.WPrimeRaceConfig
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import io.hammerhead.karooext.KarooSystemService

/**
 * W' TT field.
 *
 * Target is a straight linear depletion from 100% → 0% across the set TT duration.
 * Perfect for time trials where you want to arrive at the finish with W' at 0%.
 */
class WPrimeTTDataType(
    karooSystem: KarooSystemService,
    settings: WPrimeRaceSettings,
    extension: String,
) : WPrimeRaceDataTypeBase(karooSystem, settings, extension, TYPE_ID) {

    companion object { const val TYPE_ID = "wprime-tt" }

    override fun durationSec(config: WPrimeRaceConfig) = config.tt.durationMin * 60.0
    override fun criticalPower(config: WPrimeRaceConfig) = config.tt.criticalPower
    override fun wPrimeJ(config: WPrimeRaceConfig) = config.tt.anaerobicCapacityJ
    override fun showKj(config: WPrimeRaceConfig) = config.showKjTT

    override fun targetPercent(elapsedSec: Double, durationSec: Double, config: WPrimeRaceConfig): Double {
        if (durationSec <= 0) return 100.0
        return (100.0 * (1.0 - elapsedSec / durationSec)).coerceIn(0.0, 100.0)
    }
}

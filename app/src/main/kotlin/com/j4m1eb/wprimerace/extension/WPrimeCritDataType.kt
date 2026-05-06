package com.j4m1eb.wprimerace.extension

import com.j4m1eb.wprimerace.settings.WPrimeRaceConfig
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import io.hammerhead.karooext.KarooSystemService

/**
 * W' Crit field — W' Balance vs phase floor target.
 *
 * T: column shows the floor for the CURRENT phase (step function).
 * It holds constant across the phase and jumps down at each phase boundary.
 * The right column shows actual W'%.
 *
 * The rider's goal: keep actual W'% above the T: floor for each phase.
 *
 * Phase floors are defined by the rider's configurable curve (set in the app settings).
 * Default:
 *   0 –  43%  race → floor 70%
 *  43 –  71%  race → floor 50%
 *  71 –  97%  race → floor 30%
 *  97 – 100%  race → floor 0%  (empty the tank)
 */
class WPrimeCritDataType(
    karooSystem: KarooSystemService,
    settings: WPrimeRaceSettings,
    extension: String,
) : WPrimeRaceDataTypeBase(karooSystem, settings, extension, TYPE_ID) {

    companion object { const val TYPE_ID = "wprime-crit" }

    override fun durationSec(config: WPrimeRaceConfig) = config.crit.durationMin * 60.0
    override fun criticalPower(config: WPrimeRaceConfig) = config.crit.criticalPower
    override fun wPrimeJ(config: WPrimeRaceConfig) = config.crit.anaerobicCapacityJ
    override fun showKj(config: WPrimeRaceConfig) = config.showKjCrit

    /**
     * Target is the floor for the current phase — a step function.
     * Holds constant within a phase and jumps down at each boundary.
     */
    override fun targetPercent(elapsedSec: Double, durationSec: Double, config: WPrimeRaceConfig): Double =
        critPhaseFloorFraction(elapsedSec, durationSec, config.crit.curve) * 100.0
}

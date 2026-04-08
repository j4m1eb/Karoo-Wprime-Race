package com.j4m1eb.wprimerace.extension

import com.j4m1eb.wprimerace.settings.WPrimeRaceConfig
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import io.hammerhead.karooext.KarooSystemService

/**
 * W' Crit field.
 *
 * Target follows a progressive depletion curve designed for criterium racing:
 * conserve W' early (covering wheels, staying safe) and spend aggressively
 * in the final third when decisive moves and the sprint happen.
 *
 * Fixed curve (% of race elapsed → minimum W'% remaining):
 *   0%  → 100%
 *  20%  →  90%   (very conservative — 10% spent in first fifth)
 *  40%  →  75%   (still building — 15% spent)
 *  60%  →  55%   (heating up — 20% spent)
 *  80%  →  30%   (final push — 25% spent)
 * 100%  →   0%   (finish line — 30% spent)
 *
 * Between milestones the target is linearly interpolated.
 */
class WPrimeCritDataType(
    karooSystem: KarooSystemService,
    settings: WPrimeRaceSettings,
    extension: String,
) : WPrimeRaceDataTypeBase(karooSystem, settings, extension, TYPE_ID) {

    companion object {
        const val TYPE_ID = "wprime-crit"

        // (race fraction 0-1, W'% target 0-100)
        private val CURVE = listOf(
            0.0  to 100.0,
            0.2  to  90.0,
            0.4  to  75.0,
            0.6  to  55.0,
            0.8  to  30.0,
            1.0  to   0.0,
        )
    }

    override fun durationSec(config: WPrimeRaceConfig) = config.critDurationMin * 60.0
    override fun showKj(config: WPrimeRaceConfig) = config.showKjCrit

    override fun targetPercent(elapsedSec: Double, durationSec: Double): Double {
        if (durationSec <= 0) return 100.0
        val progress = (elapsedSec / durationSec).coerceIn(0.0, 1.0)

        for (i in 0 until CURVE.size - 1) {
            val (x0, y0) = CURVE[i]
            val (x1, y1) = CURVE[i + 1]
            if (progress <= x1) {
                val t = (progress - x0) / (x1 - x0)
                return y0 + t * (y1 - y0)
            }
        }
        return 0.0
    }
}

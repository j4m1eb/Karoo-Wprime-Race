package com.j4m1eb.wprimerace.extension

import com.j4m1eb.wprimerace.settings.CritCurvePoint
import com.j4m1eb.wprimerace.settings.WPrimeRaceConfig
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import io.hammerhead.karooext.KarooSystemService

/**
 * W' Crit field.
 *
 * Target follows a configurable depletion curve. The curve is defined by 4 interior
 * breakpoints (set in the app settings) — the start (0% → 100%) and end (100% → 0%)
 * are always fixed. Between breakpoints the target is linearly interpolated.
 *
 * Default curve:
 *   0%  → 100%  (race start)
 *  25%  →  85%  (opening — conserve)
 *  50%  →  65%  (mid-race)
 *  75%  →  40%  (build)
 *  92%  →  15%  (finale approach)
 * 100%  →   0%  (finish line — empty the tank)
 */
class WPrimeCritDataType(
    karooSystem: KarooSystemService,
    settings: WPrimeRaceSettings,
    extension: String,
) : WPrimeRaceDataTypeBase(karooSystem, settings, extension, TYPE_ID) {

    companion object { const val TYPE_ID = "wprime-crit" }

    override fun durationSec(config: WPrimeRaceConfig) = config.critDurationMin * 60.0
    override fun showKj(config: WPrimeRaceConfig) = config.showKjCrit

    override fun targetPercent(elapsedSec: Double, durationSec: Double, config: WPrimeRaceConfig): Double {
        if (durationSec <= 0) return 100.0
        val progress = (elapsedSec / durationSec).coerceIn(0.0, 1.0)
        val curve = buildFullCurve(config.critCurve)

        for (i in 0 until curve.size - 1) {
            val (x0, y0) = curve[i]
            val (x1, y1) = curve[i + 1]
            if (progress <= x1) {
                val t = (progress - x0) / (x1 - x0)
                return y0 + t * (y1 - y0)
            }
        }
        return 0.0
    }

    /** Prepend the fixed (0, 100) start and append the fixed (1, 0) end around the editable points. */
    private fun buildFullCurve(points: List<CritCurvePoint>): List<Pair<Double, Double>> =
        buildList {
            add(0.0 to 100.0)
            points.forEach { add((it.racePct / 100.0) to it.wPrimePct) }
            add(1.0 to 0.0)
        }
}

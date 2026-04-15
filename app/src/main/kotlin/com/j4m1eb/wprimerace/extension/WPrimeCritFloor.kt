package com.j4m1eb.wprimerace.extension

import com.j4m1eb.wprimerace.settings.CritCurvePoint

/**
 * Shared floor-phase logic for all crit fields.
 *
 * The race is divided into phases defined by the rider's configurable curve.
 * Each phase has a floor (minimum W' to hold). The floor STEPS at each phase
 * boundary — it does not interpolate. This makes the target immediately actionable:
 * "right now, do not go below X%".
 *
 * Example with default curve:
 *   0 –  43%  → hold 70%
 *  43 –  71%  → hold 50%
 *  71 –  97%  → hold 30%
 *  97 – 100%  → 0%  (empty the tank)
 */

/**
 * Returns the current phase floor as a fraction of total W' (0.0–1.0),
 * using the rider's configurable curve. Step function — no interpolation.
 */
fun critPhaseFloorFraction(
    elapsedSec: Double,
    raceDurationSec: Double,
    curve: List<CritCurvePoint>,
): Double {
    if (raceDurationSec <= 0.0) return curve.firstOrNull()?.wPrimePct?.div(100.0) ?: 1.0
    val progressPct = (elapsedSec / raceDurationSec).coerceIn(0.0, 1.0) * 100.0
    for (point in curve) {
        if (progressPct < point.racePct) return point.wPrimePct / 100.0
    }
    return 0.0  // past the last breakpoint — empty the tank
}

/**
 * How many seconds the rider can sustain [power] above [cp] before [wBalJ] drops to [floorJ].
 *
 * Returns [Double.MAX_VALUE] if power ≤ CP (recovering — floor not at risk).
 * Returns 0.0 if already at or below the floor.
 */
fun timeToFloorSec(wBalJ: Double, floorJ: Double, power: Double, cp: Double): Double {
    val excess = power - cp
    if (excess <= 0.0) return Double.MAX_VALUE
    val headroom = wBalJ - floorJ
    if (headroom <= 0.0) return 0.0
    return headroom / excess
}

package com.j4m1eb.wprimerace.extension

/**
 * Shared floor-phase logic for the Crit floor data fields.
 *
 * The race is divided into four phases based on progress through the total duration:
 *   0 – 31%  → hold 70% W'  (conserve, cover wheels)
 *  31 – 62%  → hold 50% W'  (mid-race, measured efforts)
 *  62 – 92%  → hold 30% W'  (final push, commit to moves)
 *  92 – 100% → hold  0% W'  (last 8% — empty the tank)
 */

/** Returns the phase floor as a fraction of total W' (0.0 – 0.70). */
fun critPhaseFloorFraction(elapsedSec: Double, raceDurationSec: Double): Double {
    if (raceDurationSec <= 0) return 0.0
    val progress = (elapsedSec / raceDurationSec).coerceIn(0.0, 1.0)
    return when {
        progress < 0.31 -> 0.70
        progress < 0.62 -> 0.50
        progress < 0.92 -> 0.30
        else            -> 0.00
    }
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

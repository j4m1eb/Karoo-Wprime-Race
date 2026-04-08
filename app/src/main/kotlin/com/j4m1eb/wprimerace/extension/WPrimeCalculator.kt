package com.j4m1eb.wprimerace.extension

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

// ─── Model interface ────────────────────────────────────────────────────────

interface IWPrimeModel {
    fun update(power: Double, dt: Double): Double
    fun getCurrentWPrime(): Double
    fun getWPrimePercent(): Double
    fun reset()
}

abstract class BaseWPrimeModel(
    protected val cp: Double,
    protected val wPrime: Double,
) : IWPrimeModel {
    protected var wBal: Double = wPrime
    override fun getCurrentWPrime() = wBal
    override fun getWPrimePercent() = if (wPrime > 0) (wBal / wPrime) * 100.0 else 0.0
    override fun reset() { wBal = wPrime }
}

// ─── Model implementations ──────────────────────────────────────────────────

class SkibaDifferentialModel(cp: Double, wPrime: Double) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        val delta = if (power > cp) -(power - cp)
        else if (wPrime > 0) ((cp - power) / wPrime) * (wPrime - wBal) else 0.0
        wBal = (wBal + delta * dt).coerceIn(0.0, wPrime)
        return wBal
    }
}

class Skiba2012Model(cp: Double, wPrime: Double) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        wBal = if (power > cp) {
            (wBal - (power - cp) * dt).coerceAtLeast(0.0)
        } else {
            (wBal + ((cp - power) / wPrime) * (wPrime - wBal) * dt).coerceAtMost(wPrime)
        }
        return wBal
    }
}

class BartramModel(cp: Double, wPrime: Double, private val tauOverride: Double?) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        val delta = if (power > cp) -(power - cp)
        else {
            val dcp = (cp - power.coerceAtMost(cp)).coerceAtLeast(1.0)
            val tau = tauOverride ?: (2287 * dcp.pow(-0.688))
            ((cp - power) / wPrime) * (wPrime - wBal) / tau
        }
        wBal = (wBal + delta * dt).coerceIn(0.0, wPrime)
        return wBal
    }
}

class ChorleyModel(cp: Double, wPrime: Double) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        wBal = if (power > cp) {
            (wBal - (power - cp) * dt).coerceAtLeast(0.0)
        } else {
            val deficit = (wPrime - wBal).coerceAtLeast(0.0)
            val rec = 0.3 * deficit * (1 - exp(-dt / 60.0)) + 0.7 * deficit * (1 - exp(-dt / 400.0))
            (wBal + rec).coerceAtMost(wPrime)
        }
        return wBal
    }
}

// ─── Enum + Factory ─────────────────────────────────────────────────────────

enum class WPrimeModelType { SKIBA_DIFFERENTIAL, SKIBA_2012, BARTRAM, CHORLEY }

object WPrimeModelFactory {
    fun create(type: WPrimeModelType, cp: Double, wPrime: Double, tau: Double? = null): IWPrimeModel = when (type) {
        WPrimeModelType.SKIBA_DIFFERENTIAL -> SkibaDifferentialModel(cp, wPrime)
        WPrimeModelType.SKIBA_2012 -> Skiba2012Model(cp, wPrime)
        WPrimeModelType.BARTRAM -> BartramModel(cp, wPrime, tau)
        WPrimeModelType.CHORLEY -> ChorleyModel(cp, wPrime)
    }
}

// ─── Calculator ─────────────────────────────────────────────────────────────

class WPrimeCalculator(
    private var cp: Double,
    private var wPrime: Double,
    private var tau: Double = 300.0,
    private var modelType: WPrimeModelType = WPrimeModelType.SKIBA_DIFFERENTIAL,
) {
    private var model: IWPrimeModel = WPrimeModelFactory.create(modelType, cp, wPrime, tau)
    private var lastTimestamp: Long = 0L

    companion object {
        private const val MAX_DT = 3600.0
    }

    fun updateConfig(cp: Double, wPrime: Double, tau: Double, modelType: WPrimeModelType) {
        this.cp = cp; this.wPrime = wPrime; this.tau = tau; this.modelType = modelType
        model = WPrimeModelFactory.create(modelType, cp, wPrime, tau)
    }

    fun update(power: Double, timestamp: Long): Double {
        val p = power.coerceIn(0.0, 2000.0)
        if (lastTimestamp == 0L) { lastTimestamp = timestamp; return model.getCurrentWPrime() }
        val dt = ((timestamp - lastTimestamp) / 1000.0).coerceIn(0.0, MAX_DT)
        if (dt <= 1e-6) return model.getCurrentWPrime()
        lastTimestamp = timestamp
        return model.update(p, dt)
    }

    fun reset() {
        model.reset()
        lastTimestamp = 0L
    }

    fun getCurrentWPrimeJ() = model.getCurrentWPrime()
    fun getWPrimePercent() = model.getWPrimePercent()
    fun getCp() = cp
    fun getWPrime() = wPrime
}

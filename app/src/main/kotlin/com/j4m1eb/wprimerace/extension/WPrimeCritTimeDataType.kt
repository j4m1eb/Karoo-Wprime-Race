package com.j4m1eb.wprimerace.extension

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import com.j4m1eb.wprimerace.ui.WPrimeSingleView
import com.j4m1eb.wprimerace.ui.timeToFloorColor
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * W' Time to Floor field.
 *
 * Shows how many seconds the rider can sustain current 3s power before W' drops to the
 * current phase floor. Displays "---" when recovering (power ≤ CP).
 *
 * The phase floor is a step function from the rider's configurable curve — consistent
 * with the W' Balance crit field and the Usable W' field.
 *
 * Colours:
 *   > 20s  → green   (comfortable)
 *  10–20s  → orange  (use carefully)
 *   5–10s  → red     (danger)
 *    < 5s  → purple  (last resort)
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WPrimeCritTimeDataType(
    private val karooSystem: KarooSystemService,
    private val settings: WPrimeRaceSettings,
    extension: String,
) : DataTypeImpl(extension, TYPE_ID) {

    companion object { const val TYPE_ID = "wprime-crit-time" }

    private val glance = GlanceRemoteViews()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Numeric stream ────────────────────────────────────────────────────────

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = scope.launch {
            val cfg = settings.configFlow.first()
            val calculator = WPrimeCalculator(cfg.crit.criticalPower, cfg.crit.anaerobicCapacityJ, modelType = cfg.modelType)
            var elapsedOffset = -1.0

            launch {
                karooSystem.consumerFlow<RideState>().collect { state ->
                    if (state is RideState.Recording) {
                        calculator.reset()
                        elapsedOffset = -1.0
                    }
                }
            }
            launch {
                settings.configFlow.collect { updated ->
                    calculator.updateConfig(updated.crit.criticalPower, updated.crit.anaerobicCapacityJ, 300.0, updated.modelType)
                }
            }

            emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to 0.0))))

            combine(
                karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER),
                karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME),
                settings.configFlow,
            ) { p, e, c -> Triple(p, e, c) }.collect { (p, e, c) ->
                val power = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                val rawElapsed = ((e as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0) / 1000.0
                if (elapsedOffset < 0) elapsedOffset = rawElapsed
                val elapsed = (rawElapsed - elapsedOffset).coerceAtLeast(0.0)
                calculator.update(power, System.currentTimeMillis())

                val raceSec = c.crit.durationMin * 60.0
                val floorJ = critPhaseFloorFraction(elapsed, raceSec, c.crit.curve) * c.crit.anaerobicCapacityJ
                val seconds = timeToFloorSec(calculator.getCurrentWPrimeJ(), floorJ, power, c.crit.criticalPower)
                val streamValue = if (seconds == Double.MAX_VALUE) -1.0 else seconds.coerceAtMost(999.0)
                emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to streamValue))))
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    // ── Graphical view ────────────────────────────────────────────────────────

    @SuppressLint("RestrictedApi")
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        val viewJob = scope.launch {
            try {
                val cfg = settings.configFlow.first()
                val calculator = WPrimeCalculator(cfg.crit.criticalPower, cfg.crit.anaerobicCapacityJ, modelType = cfg.modelType)
                var latestConfig = cfg
                var elapsedOffset = -1.0

                launch {
                    karooSystem.consumerFlow<RideState>().collect { state ->
                        if (state is RideState.Recording) {
                            calculator.reset()
                            elapsedOffset = -1.0
                        }
                    }
                }
                launch {
                    settings.configFlow.collect { updated ->
                        latestConfig = updated
                        calculator.updateConfig(updated.crit.criticalPower, updated.crit.anaerobicCapacityJ, 300.0, updated.modelType)
                    }
                }

                combine(
                    karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER),
                    karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME),
                    settings.configFlow,
                ) { p, e, c -> Triple(p, e, c) }.collect { (p, e, c) ->
                    val power = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                    val rawElapsed = ((e as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0) / 1000.0
                    if (elapsedOffset < 0) elapsedOffset = rawElapsed
                    val elapsed = (rawElapsed - elapsedOffset).coerceAtLeast(0.0)
                    calculator.update(power, System.currentTimeMillis())

                    val raceSec = c.crit.durationMin * 60.0
                    val floorJ = critPhaseFloorFraction(elapsed, raceSec, c.crit.curve) * c.crit.anaerobicCapacityJ
                    val seconds = timeToFloorSec(calculator.getCurrentWPrimeJ(), floorJ, power, c.crit.criticalPower)

                    val mainNum  = if (seconds == Double.MAX_VALUE) "---" else "${seconds.roundToInt()}"
                    val mainUnit = if (seconds == Double.MAX_VALUE) "" else "s"
                    val bgColor  = timeToFloorColor(seconds)

                    val view = withContext(Dispatchers.Main) {
                        glance.compose(context, DpSize.Unspecified) {
                            WPrimeSingleView(
                                context = context,
                                mainNum = mainNum,
                                mainUnit = mainUnit,
                                headerLabel = "TO FLOOR",
                                bgColor = bgColor,
                                config = config,
                            )
                        }.remoteViews
                    }
                    withContext(Dispatchers.Main) { emitter.updateView(view) }
                    delay(500L)
                }
            } catch (e: Exception) {
                Timber.e(e, "W' CritTime view error")
            }
        }

        emitter.setCancellable {
            configJob.cancel()
            viewJob.cancel()
        }
    }
}

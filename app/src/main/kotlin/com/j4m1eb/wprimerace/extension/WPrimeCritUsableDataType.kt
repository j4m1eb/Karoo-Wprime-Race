package com.j4m1eb.wprimerace.extension

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import com.j4m1eb.wprimerace.ui.WPrimeSingleView
import com.j4m1eb.wprimerace.ui.usableWPrimeColor
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
 * W' Usable field.
 *
 * Shows how much W' is available above the current phase floor — i.e. what the rider
 * can actually spend without compromising their race plan.
 *
 *   Usable = W'bal − phase floor
 *
 * Displays as % of total W' (default) or kJ (settings toggle).
 * Clamped at 0 if the rider is already below the floor.
 *
 * Colours (based on usable % of total W'):
 *   > 20%  → green   (good headroom)
 *  10–20%  → orange  (shrinking)
 *   5–10%  → red     (nearly at floor)
 *    < 5%  → purple  (at or below floor)
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WPrimeCritUsableDataType(
    private val karooSystem: KarooSystemService,
    private val settings: WPrimeRaceSettings,
    extension: String,
) : DataTypeImpl(extension, TYPE_ID) {

    companion object { const val TYPE_ID = "wprime-crit-usable" }

    private val glance = GlanceRemoteViews()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Numeric stream (emits usable % 0–100) ────────────────────────────────

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = scope.launch {
            val cfg = settings.configFlow.first()
            val calculator = WPrimeCalculator(cfg.criticalPower, cfg.anaerobicCapacityJ, modelType = cfg.modelType)

            launch {
                karooSystem.consumerFlow<RideState>().collect { state ->
                    if (state is RideState.Recording) calculator.reset()
                }
            }
            launch {
                settings.configFlow.collect { updated ->
                    calculator.updateConfig(updated.criticalPower, updated.anaerobicCapacityJ, 300.0, updated.modelType)
                }
            }

            emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to 100.0))))

            combine(
                karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER),
                karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME),
                settings.configFlow,
            ) { p, e, c -> Triple(p, e, c) }.collect { (p, e, c) ->
                val power = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                val elapsed = (e as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                calculator.update(power, System.currentTimeMillis())

                val raceSec = c.critDurationMin * 60.0
                val floorFraction = critPhaseFloorFraction(elapsed, raceSec)
                val floorJ = floorFraction * c.anaerobicCapacityJ
                val usableJ = (calculator.getCurrentWPrimeJ() - floorJ).coerceAtLeast(0.0)
                val usablePct = if (c.anaerobicCapacityJ > 0) (usableJ / c.anaerobicCapacityJ) * 100.0 else 0.0

                emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to usablePct))))
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
                val calculator = WPrimeCalculator(cfg.criticalPower, cfg.anaerobicCapacityJ, modelType = cfg.modelType)
                var latestConfig = cfg

                launch {
                    karooSystem.consumerFlow<RideState>().collect { state ->
                        if (state is RideState.Recording) calculator.reset()
                    }
                }
                launch {
                    settings.configFlow.collect { updated ->
                        latestConfig = updated
                        calculator.updateConfig(updated.criticalPower, updated.anaerobicCapacityJ, 300.0, updated.modelType)
                    }
                }

                combine(
                    karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER),
                    karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME),
                    settings.configFlow,
                ) { p, e, c -> Triple(p, e, c) }.collect { (p, e, c) ->
                    val power = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                    val elapsed = (e as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                    calculator.update(power, System.currentTimeMillis())

                    val raceSec = c.critDurationMin * 60.0
                    val floorFraction = critPhaseFloorFraction(elapsed, raceSec)
                    val floorJ = floorFraction * c.anaerobicCapacityJ
                    val usableJ = (calculator.getCurrentWPrimeJ() - floorJ).coerceAtLeast(0.0)
                    val usablePct = if (c.anaerobicCapacityJ > 0) (usableJ / c.anaerobicCapacityJ) * 100.0 else 0.0

                    val mainNum  = if (latestConfig.showKjUsable) "%.1f".format(usableJ / 1000.0)
                                   else "${usablePct.roundToInt()}"
                    val mainUnit = if (latestConfig.showKjUsable) "kJ" else "%"
                    val bgColor  = usableWPrimeColor(usablePct)

                    val view = withContext(Dispatchers.Main) {
                        glance.compose(context, DpSize.Unspecified) {
                            WPrimeSingleView(
                                context = context,
                                mainNum = mainNum,
                                mainUnit = mainUnit,
                                headerLabel = "USABLE W\u2032",
                                bgColor = bgColor,
                                config = config,
                            )
                        }.remoteViews
                    }
                    withContext(Dispatchers.Main) { emitter.updateView(view) }
                    delay(500L)
                }
            } catch (e: Exception) {
                Timber.e(e, "W' CritUsable view error")
            }
        }

        emitter.setCancellable {
            configJob.cancel()
            viewJob.cancel()
        }
    }
}

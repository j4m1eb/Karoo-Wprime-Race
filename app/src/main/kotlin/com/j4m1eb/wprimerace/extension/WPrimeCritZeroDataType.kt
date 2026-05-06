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
 * W' Time to Zero field.
 *
 * Shows how many seconds the rider can sustain current 3s power before W' hits 0% —
 * i.e. the point of complete depletion (going pop).
 *
 * Simpler than Time to Floor: no phase curve needed, floor is always 0.
 *   seconds = W'bal ÷ (power − CP)
 *
 * Displays "---" when recovering (power ≤ CP).
 *
 * Colours:
 *   > 20s  → green   (comfortable)
 *  10–20s  → orange  (burning fast)
 *   5–10s  → red     (danger)
 *    < 5s  → purple  (about to pop)
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WPrimeCritZeroDataType(
    private val karooSystem: KarooSystemService,
    private val settings: WPrimeRaceSettings,
    extension: String,
) : DataTypeImpl(extension, TYPE_ID) {

    companion object { const val TYPE_ID = "wprime-crit-zero" }

    private val glance = GlanceRemoteViews()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Numeric stream ────────────────────────────────────────────────────────

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = scope.launch {
            val cfg = settings.configFlow.first()
            val calculator = WPrimeCalculator(cfg.crit.criticalPower, cfg.crit.anaerobicCapacityJ, modelType = cfg.modelType)

            launch {
                karooSystem.consumerFlow<RideState>().collect { state ->
                    if (state is RideState.Recording) calculator.reset()
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
                settings.configFlow,
            ) { p, c -> Pair(p, c) }.collect { (p, c) ->
                val power = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                calculator.update(power, System.currentTimeMillis())
                val seconds = timeToFloorSec(calculator.getCurrentWPrimeJ(), 0.0, power, c.crit.criticalPower)
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

                launch {
                    karooSystem.consumerFlow<RideState>().collect { state ->
                        if (state is RideState.Recording) calculator.reset()
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
                    settings.configFlow,
                ) { p, c -> Pair(p, c) }.collect { (p, c) ->
                    val power = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                    calculator.update(power, System.currentTimeMillis())

                    val seconds = timeToFloorSec(calculator.getCurrentWPrimeJ(), 0.0, power, latestConfig.crit.criticalPower)

                    val mainNum  = if (seconds == Double.MAX_VALUE) "---" else "${seconds.roundToInt()}"
                    val mainUnit = if (seconds == Double.MAX_VALUE) "" else "s"
                    val bgColor  = timeToFloorColor(seconds)

                    val view = withContext(Dispatchers.Main) {
                        glance.compose(context, DpSize.Unspecified) {
                            WPrimeSingleView(
                                context = context,
                                mainNum = mainNum,
                                mainUnit = mainUnit,
                                headerLabel = "TO EMPTY",
                                bgColor = bgColor,
                                config = config,
                            )
                        }.remoteViews
                    }
                    withContext(Dispatchers.Main) { emitter.updateView(view) }
                    delay(500L)
                }
            } catch (e: Exception) {
                Timber.e(e, "W' CritZero view error")
            }
        }

        emitter.setCancellable {
            configJob.cancel()
            viewJob.cancel()
        }
    }
}

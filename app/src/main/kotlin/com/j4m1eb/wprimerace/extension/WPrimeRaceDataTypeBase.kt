package com.j4m1eb.wprimerace.extension

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.j4m1eb.wprimerace.settings.WPrimeRaceConfig
import com.j4m1eb.wprimerace.settings.WPrimeRaceSettings
import com.j4m1eb.wprimerace.ui.WPrimeRaceView
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
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class WPrimeRaceDataTypeBase(
    protected val karooSystem: KarooSystemService,
    protected val settings: WPrimeRaceSettings,
    extension: String,
    typeId: String,
) : DataTypeImpl(extension, typeId) {

    private val glance = GlanceRemoteViews()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Return the duration in seconds for this field type from the given config. */
    abstract fun durationSec(config: WPrimeRaceConfig): Double

    /** Compute the target W'% at the given elapsed seconds and duration. */
    abstract fun targetPercent(elapsedSec: Double, durationSec: Double): Double

    /** Whether this field should display in kJ (true) or % (false). */
    abstract fun showKj(config: WPrimeRaceConfig): Boolean

    // ── Numeric stream ────────────────────────────────────────────────────────

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = scope.launch {
            val cfg = settings.configFlow.first()
            val calculator = buildCalculator(cfg)

            // Reset calculator on new ride
            launch {
                karooSystem.consumerFlow<RideState>().collect { state ->
                    if (state is RideState.Recording) calculator.reset()
                }
            }

            // Keep calculator in sync with config changes
            launch {
                settings.configFlow.collect { updated ->
                    calculator.updateConfig(updated.criticalPower, updated.anaerobicCapacityJ, updated.ttDurationMin, updated.modelType)
                }
            }

            emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to 100.0))))

            karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER).collect { state ->
                val power = (state as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                calculator.update(power, System.currentTimeMillis())
                emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to calculator.getWPrimePercent()))))
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
                val calculator = buildCalculator(cfg)
                var latestConfig = cfg

                // Reset on new ride
                launch {
                    karooSystem.consumerFlow<RideState>().collect { state ->
                        if (state is RideState.Recording) calculator.reset()
                    }
                }

                // Track config changes
                launch {
                    settings.configFlow.collect { updated ->
                        latestConfig = updated
                        calculator.updateConfig(updated.criticalPower, updated.anaerobicCapacityJ, updated.ttDurationMin, updated.modelType)
                    }
                }

                val dataFlow = if (config.preview) previewFlow(cfg, calculator) else liveFlow(calculator)

                dataFlow.collect { (currentPct, targetPct, power) ->
                    val view = withContext(Dispatchers.Main) {
                        glance.compose(context, DpSize.Unspecified) {
                            WPrimeRaceView(
                                context = context,
                                currentPercent = currentPct,
                                targetPercent = targetPct,
                                currentPower = power,
                                criticalPower = calculator.getCp(),
                                config = config,
                                showArrow = latestConfig.showArrow,
                                showKj = showKj(latestConfig),
                                wPrimeJ = latestConfig.anaerobicCapacityJ,
                            )
                        }.remoteViews
                    }
                    withContext(Dispatchers.Main) { emitter.updateView(view) }
                    delay(500L)
                }
            } catch (e: Exception) {
                Timber.e(e, "W' Race view error for $typeId")
            }
        }

        emitter.setCancellable {
            configJob.cancel()
            viewJob.cancel()
        }
    }

    // ── Live data flow ────────────────────────────────────────────────────────

    private fun liveFlow(calculator: WPrimeCalculator) =
        karooSystem.streamDataFlow(DataType.Type.POWER)
            .combine(karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME)) { p, e -> Pair(p, e) }
            .combine(settings.configFlow) { (p, e), cfg ->
                val power = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                val elapsed = (e as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                calculator.update(power, System.currentTimeMillis())
                val currentPct = calculator.getWPrimePercent()
                val targetPct = targetPercent(elapsed, durationSec(cfg))
                Triple(currentPct, targetPct, power)
            }

    // ── Preview flow (simulates progressive depletion) ────────────────────────

    private fun previewFlow(cfg: WPrimeRaceConfig, calculator: WPrimeCalculator) =
        kotlinx.coroutines.flow.flow<Triple<Double, Double, Double>> {
            var t = 0.0
            val dur = durationSec(cfg)
            while (true) {
                val power = cfg.criticalPower * (1.0 + 0.4 * sin(t * 2 * PI / 30.0))
                calculator.update(power, System.currentTimeMillis())
                val elapsed = t * (dur / 180.0)  // run through the full target in ~180 preview ticks
                emit(Triple(calculator.getWPrimePercent(), targetPercent(elapsed, dur), power))
                t += 1.0
                delay(2000L)
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildCalculator(cfg: WPrimeRaceConfig) = WPrimeCalculator(
        cp = cfg.criticalPower,
        wPrime = cfg.anaerobicCapacityJ,
        tau = 300.0,
        modelType = cfg.modelType,
    )
}

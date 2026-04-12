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

    /** Compute the target W'% at the given elapsed seconds, duration, and live config. */
    abstract fun targetPercent(elapsedSec: Double, durationSec: Double, config: WPrimeRaceConfig): Double

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
                    calculator.updateConfig(updated.criticalPower, updated.anaerobicCapacityJ, 300.0, updated.modelType)
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

                // elapsedOffset anchors elapsed time to when this recording started.
                // Without this, a warm-up before the crit would make elapsed >> duration
                // and target would read 0% from the first second.
                var elapsedOffset = -1.0

                // Reset on new ride — also re-anchor the elapsed offset
                launch {
                    karooSystem.consumerFlow<RideState>().collect { state ->
                        if (state is RideState.Recording) {
                            calculator.reset()
                            elapsedOffset = -1.0
                        }
                    }
                }

                // Track config changes
                launch {
                    settings.configFlow.collect { updated ->
                        latestConfig = updated
                        calculator.updateConfig(updated.criticalPower, updated.anaerobicCapacityJ, 300.0, updated.modelType)
                    }
                }

                val dataFlow = if (config.preview) previewFlow(cfg, calculator) else
                    karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)
                        .combine(karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME)) { p, e -> Pair(p, e) }
                        .combine(settings.configFlow) { (p, e), c ->
                            val power      = (p as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
                            // ELAPSED_TIME is in milliseconds on Karoo — convert to seconds
                            val rawElapsedSec = ((e as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0) / 1000.0
                            // First reading sets the offset so the curve always starts from 0
                            // regardless of how long the rider was active before this field started
                            if (elapsedOffset < 0) elapsedOffset = rawElapsedSec
                            val elapsed = (rawElapsedSec - elapsedOffset).coerceAtLeast(0.0)
                            calculator.update(power, System.currentTimeMillis())
                            Triple(calculator.getWPrimePercent(), targetPercent(elapsed, durationSec(c), c), power)
                        }

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

    // ── Preview flow ──────────────────────────────────────────────────────────
    // Sweeps current W'% through all four colour zones so every state is visible
    // in the Karoo field picker demo screen.
    //   gap = target - current:  ≤0 → green  ≤8 → orange  ≤20 → red  >20 → purple
    // Target is fixed at 80 %; current oscillates 50–90 % over ~12 s.

    private fun previewFlow(cfg: WPrimeRaceConfig, calculator: WPrimeCalculator) =
        kotlinx.coroutines.flow.flow<Triple<Double, Double, Double>> {
            var t = 0.0
            val cp = cfg.criticalPower
            while (true) {
                val targetPct  = 80.0
                // sine sweep: 70 + 22*sin → range [48, 92], period = 24 ticks = 12 s
                val currentPct = 70.0 + 22.0 * sin(t * 2 * PI / 24.0)
                val power      = cp * (1.0 + 0.3 * sin(t * 2 * PI / 8.0))
                emit(Triple(currentPct, targetPct, power))
                t += 1.0
                delay(500L)
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

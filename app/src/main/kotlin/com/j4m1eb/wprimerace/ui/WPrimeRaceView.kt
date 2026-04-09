package com.j4m1eb.wprimerace.ui

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.j4m1eb.wprimerace.R
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * W' pacing view.
 *
 * Single-width:
 *   ┌─────────────┐
 *   │  T:97%      │  ← small header (target only, no arrow)
 *   │ ↓  90%      │  ← arrow + big current value inline
 *   └─────────────┘
 *
 * Double-width:
 *   ┌───────────────────────────┐
 *   │  T:97%    ↓    90%        │  ← target | arrow | current — all full height
 *   └───────────────────────────┘
 */
@SuppressLint("RestrictedApi")
@Composable
fun WPrimeRaceView(
    context: android.content.Context,
    currentPercent: Double,
    targetPercent: Double,
    currentPower: Double,
    criticalPower: Double,
    config: ViewConfig,
    showArrow: Boolean,
    showKj: Boolean = false,
    wPrimeJ: Double = 20000.0,
) {
    val density = context.resources.displayMetrics.density
    val finalTextSize = config.textSize.toFloat()
    val viewHeightDp = ceil(config.viewSize.second / density)
    val isDoubleWidth = config.viewSize.first > 400
    val topRowPadding = if (config.viewSize.first <= 238) 2f else 0f

    // Font metrics — drives header height so the big number sits at the field bottom
    val numPaint = Paint().apply {
        textSize = finalTextSize * density
        typeface = Typeface.MONOSPACE
    }
    val fm = numPaint.fontMetrics
    val baselineFromTopDp = (-fm.top) / density
    val topRowHeight = maxOf(20f, viewHeightDp - topRowPadding - baselineFromTopDp - 5f)

    val gap = targetPercent - currentPercent
    val (bgColor, textColor) = pacingColors(gap)
    val arrowRes = arrowDrawable(currentPower, criticalPower)

    val textAlign = when (config.alignment) {
        ViewConfig.Alignment.CENTER -> TextAlign.Center
        ViewConfig.Alignment.LEFT   -> TextAlign.Start
        ViewConfig.Alignment.RIGHT  -> TextAlign.End
    }
    val hAlign = when (config.alignment) {
        ViewConfig.Alignment.CENTER -> Alignment.CenterHorizontally
        ViewConfig.Alignment.LEFT   -> Alignment.Start
        ViewConfig.Alignment.RIGHT  -> Alignment.End
    }

    val unit           = if (showKj) "kJ" else "%"
    val currentNum     = if (showKj) "%.1f".format(currentPercent / 100.0 * wPrimeJ / 1000.0)
                         else "${currentPercent.roundToInt()}"
    val targetNum      = if (showKj) "%.1f".format(targetPercent / 100.0 * wPrimeJ / 1000.0)
                         else "${targetPercent.roundToInt()}"
    val targetText     = "T:$targetNum$unit"          // single-width header (18sp, safe)
    val unitSizeFull   = finalTextSize * 0.60f         // unit alongside full-size number
    val unitSizeWide   = finalTextSize * 0.78f * 0.60f // unit alongside 78% target number

    if (isDoubleWidth) {
        // ── Double-width: spacer pushes numbers low, then target | arrow | current ──
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, top = topRowPadding.dp)
                .cornerRadius(8.dp)
                .background(ColorProvider(bgColor)),
        ) {
            // Header label
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(topRowHeight.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "W\u2032 BALANCE",
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            // Main row: target | arrow | current
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Left: small "T:" + target number + small unit
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "T:",
                        modifier = GlanceModifier.padding(top = (unitSizeWide * 0.38f).dp),
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = TextUnit(unitSizeWide, TextUnitType.Sp),
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                    Text(
                        text = targetNum,
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = TextUnit(finalTextSize * 0.78f, TextUnitType.Sp),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Start,
                        ),
                    )
                    Text(
                        text = unit,
                        modifier = GlanceModifier.padding(top = (unitSizeWide * 0.38f).dp),
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = TextUnit(unitSizeWide, TextUnitType.Sp),
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
                // Centre: arrow
                if (showArrow) {
                    Image(
                        provider = ImageProvider(arrowRes),
                        contentDescription = null,
                        modifier = GlanceModifier.height(44.dp).width(44.dp).padding(top = 10.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(textColor)),
                    )
                }
                // Right: current number + small unit
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = currentNum,
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = TextUnit(finalTextSize, TextUnitType.Sp),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                        ),
                    )
                    Text(
                        text = unit,
                        modifier = GlanceModifier.padding(top = (unitSizeFull * 0.38f).dp),
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = TextUnit(unitSizeFull, TextUnitType.Sp),
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }
    } else {
        // ── Single-width: small target header, then arrow + big current inline ──
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp, top = topRowPadding.dp)
                .cornerRadius(8.dp)
                .background(ColorProvider(bgColor)),
            horizontalAlignment = hAlign,
        ) {
            // Header: target only (no arrow — arrow moves to main row)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(topRowHeight.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = hAlign,
            ) {
                Text(
                    text = targetText,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        textAlign = textAlign,
                    ),
                )
            }
            // Main row: arrow inline with big number
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = hAlign,
            ) {
                if (showArrow) {
                    Image(
                        provider = ImageProvider(arrowRes),
                        contentDescription = null,
                        modifier = GlanceModifier.height(28.dp).width(28.dp).padding(end = 4.dp, top = 6.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(textColor)),
                    )
                }
                Text(
                    text = currentNum,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = TextUnit(finalTextSize, TextUnitType.Sp),
                        fontFamily = FontFamily.Monospace,
                        textAlign = textAlign,
                    ),
                )
                Text(
                    text = unit,
                    modifier = GlanceModifier.padding(top = (unitSizeFull * 0.38f).dp),
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = TextUnit(unitSizeFull, TextUnitType.Sp),
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }
    }
}

// ── Colour palette ────────────────────────────────────────────────────────────

fun pacingColors(gap: Double): Pair<Color, Color> = when {
    gap <= 0.0  -> Color(0xFF109C77) to Color.White
    gap <= 8.0  -> Color(0xFFE5683C) to Color.White
    gap <= 20.0 -> Color(0xFFC7292A) to Color.White
    else        -> Color(0xFFAF26A0) to Color.White
}

// ── Arrow selection ───────────────────────────────────────────────────────────

fun arrowDrawable(power: Double, cp: Double): Int {
    if (power < 10.0) return R.drawable.ic_direction_arrow  // no data — show neutral
    val ratio = ((power - cp) / 150.0).coerceIn(-1.0, 1.0)
    val stepped = ((ratio * 6).roundToInt() * 15).coerceIn(-90, 90)
    return when (stepped) {
        -90 -> R.drawable.ic_direction_arrow_n90
        -75 -> R.drawable.ic_direction_arrow_n75
        -60 -> R.drawable.ic_direction_arrow_n60
        -45 -> R.drawable.ic_direction_arrow_n45
        -30 -> R.drawable.ic_direction_arrow_n30
        -15 -> R.drawable.ic_direction_arrow_n15
        0   -> R.drawable.ic_direction_arrow
        15  -> R.drawable.ic_direction_arrow_p15
        30  -> R.drawable.ic_direction_arrow_p30
        45  -> R.drawable.ic_direction_arrow_p45
        60  -> R.drawable.ic_direction_arrow_p60
        75  -> R.drawable.ic_direction_arrow_p75
        else -> R.drawable.ic_direction_arrow_p90
    }
}

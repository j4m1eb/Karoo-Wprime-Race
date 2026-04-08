package com.j4m1eb.wprimerace.ui

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.j4m1eb.wprimerace.R
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Colour-coded W' pacing view.
 *
 * Single-width: header row (arrow + target%) above large current%.
 * Double-width:  target% on left, current% on right, arrow between them.
 *
 * Colour logic (gap = target% − current%, i.e. how far *below* target):
 *   gap ≤ 0    → green  (on or ahead of target)
 *   gap 1–8    → orange (slightly behind)
 *   gap 8–20   → red    (behind)
 *   gap > 20   → purple (significantly behind)
 */
@SuppressLint("RestrictedApi")
@Composable
fun WPrimeRaceView(
    context: android.content.Context,
    currentPercent: Double,       // W'% right now (0–100)
    targetPercent: Double,        // W'% the pacing plan says you should have (0–100)
    currentPower: Double,         // for arrow direction
    criticalPower: Double,
    config: ViewConfig,
    showArrow: Boolean,
) {
    val isWide = config.gridSize.first >= 60

    val gap = targetPercent - currentPercent          // positive = behind plan
    val (bgColor, textColor) = pacingColors(gap)

    val density = context.resources.displayMetrics.density
    val viewHeightDp = ceil(config.viewSize.second / density)

    // Arrow: up when recovering (power < CP), down when depleting (power > CP)
    val arrowRes = arrowDrawable(currentPower, criticalPower)

    val textAlign = when (config.alignment) {
        ViewConfig.Alignment.CENTER -> TextAlign.Center
        ViewConfig.Alignment.LEFT -> TextAlign.Start
        ViewConfig.Alignment.RIGHT -> TextAlign.End
    }
    val hAlign = when (config.alignment) {
        ViewConfig.Alignment.CENTER -> Alignment.CenterHorizontally
        ViewConfig.Alignment.LEFT -> Alignment.Start
        ViewConfig.Alignment.RIGHT -> Alignment.End
    }

    val currentText = "${currentPercent.roundToInt()}%"
    val targetText = "T:${targetPercent.roundToInt()}%"

    // Calculate main number font size (same approach as colorspeed)
    val paint = Paint().apply {
        textSize = config.textSize.toFloat() * density
        typeface = Typeface.MONOSPACE
    }
    val fm = paint.fontMetrics
    val baselineFromTopDp = (-fm.top) / density
    val headerHeightDp = if (isWide) 0f else maxOf(18f, viewHeightDp * 0.25f)
    val numberSizeSp = config.textSize.toFloat()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(8.dp)
            .background(ColorProvider(bgColor)),
    ) {
        if (isWide) {
            // ── Double-width: [target%]  [arrow]  [current%] ──────────────
            Row(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: target
                Text(
                    text = targetText,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = (numberSizeSp * 0.55f).sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Start,
                    ),
                )
                // Centre: arrow
                if (showArrow) {
                    Image(
                        provider = ImageProvider(arrowRes),
                        contentDescription = "W' trend",
                        modifier = GlanceModifier.size(28.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(textColor)),
                    )
                }
                // Right: current (large)
                Text(
                    text = currentText,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = numberSizeSp.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.End,
                    ),
                )
            }
        } else {
            // ── Single-width: header (arrow + target) above large current ──
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(start = 5.dp, end = 5.dp),
                horizontalAlignment = hAlign,
            ) {
                // Header row
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(headerHeightDp.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = hAlign,
                ) {
                    if (showArrow) {
                        Image(
                            provider = ImageProvider(arrowRes),
                            contentDescription = "W' trend",
                            modifier = GlanceModifier.size(16.dp).padding(end = 3.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(textColor)),
                        )
                    }
                    Text(
                        text = targetText,
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = textAlign,
                        ),
                    )
                }
                // Main number
                Text(
                    modifier = GlanceModifier.fillMaxWidth(),
                    text = currentText,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = numberSizeSp.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        textAlign = textAlign,
                    ),
                )
            }
        }
    }
}

// ── Colour palette ───────────────────────────────────────────────────────────

fun pacingColors(gap: Double): Pair<Color, Color> = when {
    gap <= 0.0  -> Color(0xFF109C77) to Color.White  // green — on/ahead of target
    gap <= 8.0  -> Color(0xFFE5683C) to Color.White  // orange — slightly behind
    gap <= 20.0 -> Color(0xFFC7292A) to Color.White  // red — behind
    else        -> Color(0xFFAF26A0) to Color.White  // purple — significantly behind
}

// ── Arrow selection (power vs CP) ────────────────────────────────────────────

fun arrowDrawable(power: Double, cp: Double): Int {
    val delta = power - cp
    val maxDelta = 150.0
    val ratio = (delta / maxDelta).coerceIn(-1.0, 1.0)
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

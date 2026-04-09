package com.j4m1eb.wprimerace.ui

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.hammerhead.karooext.models.ViewConfig

/**
 * Single-value Glance view for W' floor fields (Time to Floor, Usable W').
 * [mainNum] is the number, [mainUnit] is the suffix at 60% size (pass "" for none).
 */
@SuppressLint("RestrictedApi")
@Composable
fun WPrimeSingleView(
    context: android.content.Context,
    mainNum: String,
    mainUnit: String,
    headerLabel: String,
    bgColor: Color,
    config: ViewConfig,
) {
    val density = context.resources.displayMetrics.density
    val finalTextSize = config.textSize.toFloat()
    val viewHeightDp = kotlin.math.ceil(config.viewSize.second / density)
    val topRowPadding = if (config.viewSize.first <= 238) 2f else 0f

    // Identical font metrics calculation to WPrimeRaceView — positions the
    // main value flush at the field bottom matching the right-hand field
    val numPaint = Paint().apply {
        textSize = finalTextSize * density
        typeface = Typeface.MONOSPACE
    }
    val fm = numPaint.fontMetrics
    val baselineFromTopDp = (-fm.top) / density
    val topRowHeight = maxOf(20f, viewHeightDp - topRowPadding - baselineFromTopDp - 5f)

    val textColor = Color.White
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

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = 5.dp, end = 5.dp, top = topRowPadding.dp)
            .cornerRadius(8.dp)
            .background(ColorProvider(bgColor)),
        horizontalAlignment = hAlign,
    ) {
        // Small header label
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(topRowHeight.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = hAlign,
        ) {
            Text(
                text = headerLabel,
                style = TextStyle(
                    color = ColorProvider(textColor.copy(alpha = 0.85f)),
                    fontSize = TextUnit(18f, TextUnitType.Sp),
                    textAlign = textAlign,
                ),
            )
        }
        // Number full-size, unit at 60% — Row with Alignment.Top keeps Glance stable
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = hAlign,
        ) {
            Text(
                text = mainNum,
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = TextUnit(finalTextSize, TextUnitType.Sp),
                    fontFamily = FontFamily.Monospace,
                    textAlign = textAlign,
                ),
            )
            if (mainUnit.isNotEmpty()) {
                val unitSize = finalTextSize * 0.60f
                Text(
                    text = mainUnit,
                    modifier = GlanceModifier.padding(start = 2.dp, top = (unitSize * 0.38f).dp),
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = TextUnit(unitSize, TextUnitType.Sp),
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }
    }
}

// ── Colour helpers for floor fields ──────────────────────────────────────────

fun timeToFloorColor(seconds: Double): Color = when {
    seconds == Double.MAX_VALUE -> Color(0xFF109C77)
    seconds > 20.0              -> Color(0xFF109C77)
    seconds > 10.0              -> Color(0xFFE5683C)
    seconds > 5.0               -> Color(0xFFC7292A)
    else                        -> Color(0xFFAF26A0)
}

fun usableWPrimeColor(usablePercent: Double): Color = when {
    usablePercent > 20.0 -> Color(0xFF109C77)
    usablePercent > 10.0 -> Color(0xFFE5683C)
    usablePercent > 5.0  -> Color(0xFFC7292A)
    else                 -> Color(0xFFAF26A0)
}

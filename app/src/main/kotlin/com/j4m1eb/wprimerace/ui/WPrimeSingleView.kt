package com.j4m1eb.wprimerace.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.ceil

/**
 * Simple single-value Glance view used by the W' floor fields (Time to Floor, Usable W').
 *
 * Shows an optional small label in the header and a large main value, coloured by [bgColor].
 */
@SuppressLint("RestrictedApi")
@Composable
fun WPrimeSingleView(
    context: android.content.Context,
    mainValue: String,           // large number, e.g. "23s", "20%", "4.0kJ", "---"
    headerLabel: String,         // small top label, e.g. "TO FLOOR", "USABLE"
    bgColor: Color,
    config: ViewConfig,
) {
    val density = context.resources.displayMetrics.density
    val viewHeightDp = ceil(config.viewSize.second / density)
    val headerHeightDp = maxOf(18f, viewHeightDp * 0.25f)
    val numberSizeSp = config.textSize.toFloat()

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

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(8.dp)
            .background(ColorProvider(bgColor)),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp),
            horizontalAlignment = hAlign,
        ) {
            // Small header label
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = when (config.alignment) {
                    ViewConfig.Alignment.CENTER -> Alignment.Center
                    ViewConfig.Alignment.LEFT   -> Alignment.CenterStart
                    ViewConfig.Alignment.RIGHT  -> Alignment.CenterEnd
                },
            ) {
                Text(
                    text = headerLabel,
                    style = TextStyle(
                        color = ColorProvider(textColor.copy(alpha = 0.75f)),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = textAlign,
                    ),
                )
            }
            // Main large value
            Text(
                modifier = GlanceModifier.fillMaxWidth(),
                text = mainValue,
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

// ── Colour helpers for floor fields ──────────────────────────────────────────

/** Colour for the Time to Floor field based on seconds remaining. */
fun timeToFloorColor(seconds: Double): Color = when {
    seconds == Double.MAX_VALUE -> Color(0xFF109C77)   // recovering — green
    seconds > 20.0              -> Color(0xFF109C77)   // comfortable — green
    seconds > 10.0              -> Color(0xFFE5683C)   // use carefully — orange
    seconds > 5.0               -> Color(0xFFC7292A)   // danger — red
    else                        -> Color(0xFFAF26A0)   // last resort — purple
}

/** Colour for the Usable W' field based on usable % of total W'. */
fun usableWPrimeColor(usablePercent: Double): Color = when {
    usablePercent > 20.0 -> Color(0xFF109C77)   // good headroom — green
    usablePercent > 10.0 -> Color(0xFFE5683C)   // shrinking — orange
    usablePercent > 5.0  -> Color(0xFFC7292A)   // nearly at floor — red
    else                 -> Color(0xFFAF26A0)   // at/below floor — purple
}

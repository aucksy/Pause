package com.pause.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders text as a floating "sticker" — a bold white fill with a dark outline stroke and a soft
 * drop shadow, and no background box. It reads cleanly on top of any content, which is exactly the
 * cutout look we want for the overlay message. Two stacked [Text]s (outline behind, fill in front)
 * wrap identically because they share the same constraints and alignment.
 */
@Composable
fun CutoutText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fillColor: Color = Color.White,
    outlineColor: Color = Color(0xFF14111F),
    outlineWidth: Dp = 3.dp,
    textAlign: TextAlign = TextAlign.Center,
) {
    val outlinePx = with(LocalDensity.current) { outlineWidth.toPx() }
    val base = style.merge(TextStyle(fontWeight = FontWeight.Bold, textAlign = textAlign))

    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = base.merge(
                TextStyle(
                    color = outlineColor,
                    drawStyle = Stroke(width = outlinePx, join = StrokeJoin.Round),
                ),
            ),
        )
        Text(
            text = text,
            style = base.merge(
                TextStyle(
                    color = fillColor,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 4f), blurRadius = 16f),
                ),
            ),
        )
    }
}

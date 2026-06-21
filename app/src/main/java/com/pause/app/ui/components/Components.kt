package com.pause.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pause.app.domain.model.AppDefinition

/** Readable text/foreground colour for a given brand background. */
fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.6f) Color(0xFF1A1826) else Color.White

private fun Color.lighten(fraction: Float): Color = lerp(this, Color.White, fraction)

/**
 * A brand-coloured rounded tile showing the app's first letter. We render this instead of the
 * real launcher icon so the UI looks complete even for apps that aren't installed, and so Pause
 * never needs permission to enumerate installed apps.
 */
@Composable
fun AppMonogram(
    definition: AppDefinition,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val accent = Color(definition.accent)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 30))
            .background(Brush.linearGradient(listOf(accent.lighten(0.16f), accent))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = definition.label.take(1),
            color = onColorFor(accent),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** The standard calm container: a flat surface with a hairline border and generous radius. */
@Composable
fun PauseCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

package com.pause.app.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pause.app.R
import com.pause.app.core.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * The interruption itself. A full-screen, dimmed + blurred scrim with the (placeholder)
 * character cutout floating in front of whatever the user was scrolling, the session line,
 * and a Continue button that only arms after a short, calming countdown.
 *
 * Owns its own enter/exit animation; when the exit finishes it calls [onFinished], which the
 * [OverlayController] uses to remove the window and re-arm the next interval.
 */
@Composable
fun PauseOverlay(
    appLabel: String,
    intervalMinutes: Int,
    onFinished: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 320 else 240, easing = FastOutSlowInEasing),
        label = "scrimAlpha",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 360 else 200),
        label = "contentAlpha",
    )
    val contentScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = if (visible) {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(durationMillis = 220)
        },
        label = "contentScale",
    )

    fun beginDismiss() {
        if (dismissing) return
        dismissing = true
        visible = false
        scope.launch {
            delay(EXIT_DURATION_MS)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrimAlpha }
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B0A12).copy(alpha = 0.50f),
                        Color(0xFF15111F).copy(alpha = 0.64f),
                    ),
                ),
            )
            // Absorb any taps that miss the button so nothing leaks to the app underneath.
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Soft glow + floating character cutout (no rectangle behind it).
            Box(
                modifier = Modifier.size(248.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFA395FF).copy(alpha = 0.30f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Image(
                    painter = painterResource(R.drawable.ic_pause_character),
                    contentDescription = null,
                    modifier = Modifier.size(208.dp),
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "You've been scrolling for ${formatMinutes(intervalMinutes)}.",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "$appLabel — take a breath. Still want to keep going?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.74f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            ContinueButton(
                countdownSeconds = Constants.CONTINUE_COUNTDOWN_SECONDS,
                onContinue = ::beginDismiss,
            )
        }
    }
}

@Composable
private fun ContinueButton(
    countdownSeconds: Int,
    onContinue: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val progress = remember { Animatable(0f) }
    var enabled by remember { mutableStateOf(countdownSeconds <= 0) }
    val pressScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (countdownSeconds > 0) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = countdownSeconds * 1000, easing = LinearEasing),
            )
            enabled = true
        } else {
            progress.snapTo(1f)
        }
    }

    val remaining = ceil((1f - progress.value) * countdownSeconds).toInt().coerceAtLeast(1)
    val textColor = Color.White.copy(alpha = if (enabled) 1f else 0.62f)

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = pressScale.value; scaleY = pressScale.value }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.16f else 0.08f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.55f else 0.22f),
                        Color.White.copy(alpha = 0.10f),
                    ),
                ),
                shape = CircleShape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    pressScale.animateTo(0.94f, tween(70))
                    pressScale.animateTo(1f, tween(140))
                }
                onContinue()
            }
            .padding(horizontal = 30.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (!enabled) {
                CountdownRing(
                    progress = progress.value,
                    remaining = remaining,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
            )
        }
    }
}

@Composable
private fun CountdownRing(
    progress: Float,
    remaining: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.5.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = (1f - progress) * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = remaining.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

private fun formatMinutes(minutes: Int): String =
    if (minutes == 1) "1 minute" else "$minutes minutes"

private const val EXIT_DURATION_MS = 280L

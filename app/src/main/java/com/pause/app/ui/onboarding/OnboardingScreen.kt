package com.pause.app.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pause.app.core.PausePermissions
import com.pause.app.domain.model.AppCatalog
import com.pause.app.domain.model.AppDefinition
import com.pause.app.domain.model.IntervalOptions
import com.pause.app.ui.AppViewModel
import com.pause.app.ui.components.AppMonogram
import com.pause.app.ui.components.PauseCard
import com.pause.app.ui.permissions.rememberSystemPermissions

private const val STEP_COUNT = 3

@Composable
fun OnboardingScreen(
    viewModel: AppViewModel,
    onFinished: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions = rememberSystemPermissions()
    var step by rememberSaveable { mutableIntStateOf(0) }

    val canAdvance = when (step) {
        0 -> settings.selectedPackages.isNotEmpty()
        1 -> true
        else -> permissions.allGranted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            StepDots(current = step)
            Spacer(Modifier.height(12.dp))

            Crossfade(targetState = step, label = "onboardingStep", modifier = Modifier.weight(1f)) { which ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (which) {
                        0 -> StepApps(
                            selected = settings.selectedPackages,
                            onToggle = viewModel::toggleApp,
                        )
                        1 -> StepInterval(
                            selected = settings.intervalMinutes,
                            onSelect = viewModel::setInterval,
                        )
                        else -> StepPermissions(
                            accessibilityGranted = permissions.accessibility,
                            overlayGranted = permissions.overlay,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            BottomBar(
                isLastStep = step == STEP_COUNT - 1,
                canAdvance = canAdvance,
                showBack = step > 0,
                onBack = { if (step > 0) step-- },
                onNext = {
                    if (step < STEP_COUNT - 1) {
                        step++
                    } else {
                        viewModel.completeOnboarding()
                        onFinished()
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(10.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun StepApps(selected: Set<String>, onToggle: (String) -> Unit) {
    StepHeader(
        title = "Which apps pull you in?",
        subtitle = "Pick the ones where time tends to disappear. Pause will only ever interrupt these.",
    )
    PauseCard(Modifier.fillMaxWidth()) {
        Column {
            AppCatalog.apps.forEachIndexed { index, app ->
                AppSelectRow(
                    definition = app,
                    selected = app.packageName in selected,
                    onClick = { onToggle(app.packageName) },
                )
                if (index < AppCatalog.apps.lastIndex) RowDivider()
            }
        }
    }
}

@Composable
private fun StepInterval(selected: Int, onSelect: (Int) -> Unit) {
    StepHeader(
        title = "How long before a nudge?",
        subtitle = "After this much continuous scrolling in a chosen app, Pause gently appears.",
    )
    PauseCard(Modifier.fillMaxWidth()) {
        Column {
            IntervalOptions.minutes.forEachIndexed { index, minutes ->
                ChoiceRow(
                    label = "$minutes minutes",
                    selected = minutes == selected,
                    onClick = { onSelect(minutes) },
                )
                if (index < IntervalOptions.minutes.lastIndex) RowDivider()
            }
        }
    }
}

@Composable
private fun StepPermissions(accessibilityGranted: Boolean, overlayGranted: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    StepHeader(
        title = "Two quick permissions",
        subtitle = "Pause needs these to work. Nothing is collected, and nothing leaves your phone.",
    )
    PauseCard(Modifier.fillMaxWidth()) {
        Column {
            PermissionRow(
                title = "Accessibility access",
                description = "Lets Pause notice which app is in front. It never reads your screen.",
                granted = accessibilityGranted,
                onClick = { PausePermissions.openAccessibilitySettings(context) },
            )
            RowDivider()
            PermissionRow(
                title = "Display over other apps",
                description = "Lets the gentle reminder appear on top of what you're scrolling.",
                granted = overlayGranted,
                onClick = { PausePermissions.openOverlaySettings(context) },
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "No account. No analytics. No login.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AppSelectRow(definition: AppDefinition, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMonogram(definition = definition, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            definition.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        SelectionIndicator(selected)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        SelectionIndicator(selected)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Granted", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            TextButton(onClick = onClick) { Text("Grant") }
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "selectionColor",
    )
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(color)
                else Modifier.border(2.dp, color, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun StepDots(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(STEP_COUNT) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 26.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun BottomBar(
    isLastStep: Boolean,
    canAdvance: Boolean,
    showBack: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onNext,
            enabled = canAdvance,
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                if (isLastStep) "Start Pause" else "Continue",
                style = MaterialTheme.typography.titleMedium,
            )
            if (!isLastStep) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        if (showBack) {
            TextButton(onClick = onBack) { Text("Back") }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

package com.pause.app.ui.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pause.app.core.PausePermissions
import com.pause.app.domain.model.AppCatalog
import com.pause.app.domain.model.IntervalOptions
import com.pause.app.domain.model.MonitoringStatus
import com.pause.app.domain.model.PauseSettings
import com.pause.app.ui.AppViewModel
import com.pause.app.ui.components.AppIcon
import com.pause.app.ui.components.PauseCard
import com.pause.app.ui.permissions.SystemPermissions
import com.pause.app.ui.permissions.rememberSystemPermissions

private val AmberAccent = Color(0xFFE0A458)

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenCustomize: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions = rememberSystemPermissions()
    val context = LocalContext.current

    val status = when {
        !permissions.allGranted -> MonitoringStatus.NEEDS_PERMISSION
        settings.isActivelyMonitoring -> MonitoringStatus.RUNNING
        else -> MonitoringStatus.PAUSED
    }

    var showAppPicker by remember { mutableStateOf(false) }
    var showIntervalPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(28.dp))
            Text("Pause", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(
                "Interrupt the autopilot. Let yourself choose.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            StatusToggleCard(
                status = status,
                settings = settings,
                permissionsGranted = permissions.allGranted,
                onToggle = { viewModel.setEnabled(it) },
                onNeedsPermission = {
                    if (!permissions.accessibility) PausePermissions.openAccessibilitySettings(context)
                    else PausePermissions.openOverlaySettings(context)
                },
            )

            if (!permissions.allGranted) {
                Spacer(Modifier.height(14.dp))
                PermissionBanner(
                    permissions = permissions,
                    onFixAccessibility = { PausePermissions.openAccessibilitySettings(context) },
                    onFixOverlay = { PausePermissions.openOverlaySettings(context) },
                )
            }

            Spacer(Modifier.height(14.dp))
            SelectedAppsCard(
                selected = settings.selectedPackages,
                onClick = { showAppPicker = true },
            )

            Spacer(Modifier.height(14.dp))
            IntervalCard(
                intervalMinutes = settings.intervalMinutes,
                onClick = { showIntervalPicker = true },
            )

            Spacer(Modifier.height(14.dp))
            ReminderCard(
                message = settings.overlayMessage,
                showImage = settings.showImage,
                showText = settings.showText,
                hasCustomImage = settings.customImagePath != null,
                onClick = onOpenCustomize,
            )

            Spacer(Modifier.height(28.dp))
            Text(
                "No accounts, no analytics, no shame — just a calm moment of awareness.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showAppPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAppPicker = false }, sheetState = sheetState) {
            AppPickerContent(
                selected = settings.selectedPackages,
                onToggle = viewModel::toggleApp,
            )
        }
    }

    if (showIntervalPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showIntervalPicker = false }, sheetState = sheetState) {
            IntervalPickerContent(
                selected = settings.intervalMinutes,
                onSelect = {
                    viewModel.setInterval(it)
                    showIntervalPicker = false
                },
            )
        }
    }
}

@Composable
private fun StatusToggleCard(
    status: MonitoringStatus,
    settings: PauseSettings,
    permissionsGranted: Boolean,
    onToggle: (Boolean) -> Unit,
    onNeedsPermission: () -> Unit,
) {
    val (label, detail, dotColor) = when (status) {
        MonitoringStatus.RUNNING -> Triple(
            "Running",
            "Watching ${settings.selectedPackages.size} ${appWord(settings.selectedPackages.size)}, every ${settings.intervalMinutes} min.",
            MaterialTheme.colorScheme.primary,
        )
        MonitoringStatus.PAUSED -> Triple(
            "Paused",
            if (settings.selectedPackages.isEmpty()) "Choose at least one app to watch." else "Turn on to start gentle reminders.",
            MaterialTheme.colorScheme.outline,
        )
        MonitoringStatus.NEEDS_PERMISSION -> Triple(
            "Action needed",
            "Grant the permissions below to begin.",
            AmberAccent,
        )
    }

    PauseCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (permissionsGranted) onToggle(!settings.isEnabled) else onNeedsPermission() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            // Gated on permissions: until both are granted, the switch can't promise to run, so
            // it's disabled and the whole card routes to granting them instead.
            Switch(
                checked = settings.isEnabled,
                onCheckedChange = onToggle,
                enabled = permissionsGranted,
            )
        }
    }
}

@Composable
private fun PermissionBanner(
    permissions: SystemPermissions,
    onFixAccessibility: () -> Unit,
    onFixOverlay: () -> Unit,
) {
    val onClick = if (!permissions.accessibility) onFixAccessibility else onFixOverlay
    val what = if (!permissions.accessibility) "Enable accessibility access" else "Allow display over other apps"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(AmberAccent.copy(alpha = 0.16f))
            .border(1.dp, AmberAccent.copy(alpha = 0.45f), MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            what,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SelectedAppsCard(selected: Set<String>, onClick: () -> Unit) {
    PauseCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Selected apps", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                if (selected.isEmpty()) {
                    Text(
                        "None yet — tap to choose.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppCatalog.apps.filter { it.packageName in selected }.take(6).forEach { app ->
                            AppIcon(definition = app, size = 36.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IntervalCard(intervalMinutes: Int, onClick: () -> Unit) {
    PauseCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Interruption interval", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Every $intervalMinutes minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderCard(
    message: String,
    showImage: Boolean,
    showText: Boolean,
    hasCustomImage: Boolean,
    onClick: () -> Unit,
) {
    val parts = buildList {
        if (showImage) add(if (hasCustomImage) "Custom image" else "Character")
        if (showText && message.isNotBlank()) add("“$message”")
    }
    val summary = if (parts.isEmpty()) "Tap to customize" else parts.joinToString("  ·  ")

    PauseCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Interruption style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppPickerContent(selected: Set<String>, onToggle: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Text("Apps to watch", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pause only interrupts the apps you pick here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        AppCatalog.apps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(app.packageName) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(definition = app, size = 40.dp)
                Spacer(Modifier.width(14.dp))
                Text(app.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                CheckCircle(selected = app.packageName in selected)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun IntervalPickerContent(selected: Int, onSelect: (Int) -> Unit) {
    val isCustom = !IntervalOptions.isPreset(selected)
    // Keyed on `selected` so the prefill refreshes if the stored interval changes while open.
    var customText by remember(selected) { mutableStateOf(if (isCustom) selected.toString() else "") }
    val customValue = customText.toIntOrNull()
    val customValid = customValue != null && customValue in IntervalOptions.MIN_MINUTES..IntervalOptions.MAX_MINUTES

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Text("Interruption interval", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        IntervalOptions.minutes.forEach { minutes ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(minutes) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$minutes minutes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                CheckCircle(selected = minutes == selected)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Custom", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.width(130.dp),
                label = { Text("Minutes") },
                singleLine = true,
                isError = customText.isNotEmpty() && !customValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { if (customValid) onSelect(customValue) },
                enabled = customValid,
            ) {
                Text("Set")
            }
            if (isCustom) {
                Spacer(Modifier.weight(1f))
                CheckCircle(selected = true)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${IntervalOptions.MIN_MINUTES}–${IntervalOptions.MAX_MINUTES} minutes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CheckCircle(selected: Boolean) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "checkColor",
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

private fun appWord(count: Int): String = if (count == 1) "app" else "apps"

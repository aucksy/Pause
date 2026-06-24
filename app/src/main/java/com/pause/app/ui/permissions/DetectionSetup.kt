package com.pause.app.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pause.app.domain.model.DetectionMode
import com.pause.app.ui.components.PauseCard

private val Positive = Color(0xFF4CAF82)

/**
 * The plain-English "why we need this & why it's safe" setup, shared by onboarding and the home
 * screen. It explains what Pause reads (and what it can't), lets the user pick the detection
 * method, and surfaces the exact permissions the chosen method needs.
 */
@Composable
fun DetectionSetup(
    mode: DetectionMode,
    permissions: SystemPermissions,
    onPickMode: (DetectionMode) -> Unit,
    onGrantDetection: () -> Unit,
    onGrantOverlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // --- Plain-English explainer ---
        PauseCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "Pause needs to know which app is open and for how long — nothing else.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Bullet(true, "Reads only the app's name (e.g. \"Instagram\") and your time in it.")
                Bullet(false, "Can't read your messages, see your screen, or know what you type.")
                Bullet(true, "Everything stays on your phone — no account, no internet, no data collected.")
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "How should Pause watch your time?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        MethodChoice(
            title = "Usage access",
            recommended = true,
            description = "A gentle \"Usage access\" toggle. Shows a small ongoing notification while watching.",
            selected = mode == DetectionMode.USAGE_ACCESS,
            onClick = { onPickMode(DetectionMode.USAGE_ACCESS) },
        )
        Spacer(Modifier.height(10.dp))
        MethodChoice(
            title = "Accessibility",
            recommended = false,
            description = "No notification, but Android shows a strong warning screen for this one.",
            selected = mode == DetectionMode.ACCESSIBILITY,
            onClick = { onPickMode(DetectionMode.ACCESSIBILITY) },
        )

        Spacer(Modifier.height(18.dp))
        PauseCard(Modifier.fillMaxWidth()) {
            Column {
                PermissionRow(
                    title = if (mode == DetectionMode.USAGE_ACCESS) "Usage access" else "Accessibility access",
                    granted = permissions.hasDetection(mode),
                    onGrant = onGrantDetection,
                )
                RowDivider()
                PermissionRow(
                    title = "Display over other apps",
                    granted = permissions.overlay,
                    onGrant = onGrantOverlay,
                )
            }
        }

        if (mode == DetectionMode.ACCESSIBILITY) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Heads up: Android shows a strong warning on the next screen. That's standard for this permission — Pause only uses it for the single purpose above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Bullet(positive: Boolean, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Icon(
            imageVector = if (positive) Icons.Rounded.Check else Icons.Rounded.Close,
            contentDescription = null,
            tint = if (positive) Positive else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MethodChoice(
    title: String,
    recommended: Boolean,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(if (selected) 2.dp else 1.dp, border, MaterialTheme.shapes.large)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (recommended) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("Recommended", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        SelectionDot(selected)
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Granted", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            TextButton(onClick = onGrant) { Text("Grant") }
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

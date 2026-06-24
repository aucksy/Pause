package com.pause.app.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pause.app.ui.components.PauseCard

private val Positive = Color(0xFF4CAF82)

/**
 * The plain-English "why we need this & why it's safe" setup, shared by onboarding and the home
 * screen. It explains what Pause reads (and what it can't), then surfaces the two permissions Pause
 * needs: "Usage access" (to know which app is open and for how long) and "Display over other apps"
 * (to draw the reminder).
 */
@Composable
fun DetectionSetup(
    permissions: SystemPermissions,
    onGrantUsageAccess: () -> Unit,
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
                Bullet(true, "Runs quietly in the background — no nagging notification to dismiss.")
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Two quick permissions",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        PauseCard(Modifier.fillMaxWidth()) {
            Column {
                PermissionRow(
                    title = "Usage access",
                    subtitle = "Lets Pause see which app is open and for how long.",
                    granted = permissions.usageAccess,
                    onGrant = onGrantUsageAccess,
                )
                RowDivider()
                PermissionRow(
                    title = "Display over other apps",
                    subtitle = "Lets Pause show the gentle reminder on top.",
                    granted = permissions.overlay,
                    onGrant = onGrantOverlay,
                )
            }
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
private fun PermissionRow(title: String, subtitle: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

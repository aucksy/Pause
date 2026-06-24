package com.pause.app.ui.permissions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pause.app.core.PausePermissions
import com.pause.app.domain.model.DetectionMode

/** Snapshot of the system permissions Pause can rely on. */
data class SystemPermissions(
    val accessibility: Boolean,
    val usageAccess: Boolean,
    val overlay: Boolean,
) {
    /** Whether everything needed for the chosen detection [mode] is granted. */
    fun ready(mode: DetectionMode): Boolean = overlay && when (mode) {
        DetectionMode.USAGE_ACCESS -> usageAccess
        DetectionMode.ACCESSIBILITY -> accessibility
    }

    /** The detection permission the chosen [mode] needs (ignoring overlay). */
    fun hasDetection(mode: DetectionMode): Boolean = when (mode) {
        DetectionMode.USAGE_ACCESS -> usageAccess
        DetectionMode.ACCESSIBILITY -> accessibility
    }
}

private fun read(context: Context) = SystemPermissions(
    accessibility = PausePermissions.isAccessibilityServiceEnabled(context),
    usageAccess = PausePermissions.hasUsageAccess(context),
    overlay = PausePermissions.canDrawOverlays(context),
)

/**
 * Returns the live permission state, re-reading it every time the app resumes — so when the
 * user comes back from the system Settings screen the UI reflects what they just granted.
 */
@Composable
fun rememberSystemPermissions(): SystemPermissions {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissions by remember { mutableStateOf(read(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissions = read(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return permissions
}

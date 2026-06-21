package com.pause.app.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import com.pause.app.service.PauseAccessibilityService

/**
 * Helpers for the two system permissions Pause depends on:
 *  - the Accessibility Service (to learn which app is foreground), and
 *  - "Display over other apps" / SYSTEM_ALERT_WINDOW (to draw the overlay).
 *
 * These are pure system-state reads, so the UI re-checks them on every resume.
 */
object PausePermissions {

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** True when Pause's accessibility service is enabled in system settings. */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, PauseAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (entry in splitter) {
            val component = ComponentName.unflattenFromString(entry) ?: continue
            if (component == expected) return true
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

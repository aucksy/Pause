package com.pause.app

import com.google.common.truth.Truth.assertThat
import com.pause.app.domain.model.PauseSettings
import org.junit.Test

class PauseSettingsTest {

    private fun settings(enabled: Boolean, apps: Set<String>) =
        PauseSettings(selectedPackages = apps, intervalMinutes = 15, isEnabled = enabled, onboardingComplete = true)

    @Test
    fun `monitoring requires both enabled and at least one app`() {
        assertThat(settings(enabled = true, apps = setOf("com.instagram.android")).isActivelyMonitoring).isTrue()
        assertThat(settings(enabled = false, apps = setOf("com.instagram.android")).isActivelyMonitoring).isFalse()
        assertThat(settings(enabled = true, apps = emptySet()).isActivelyMonitoring).isFalse()
    }

    @Test
    fun `default settings are inert`() {
        assertThat(PauseSettings.DEFAULT.isActivelyMonitoring).isFalse()
        assertThat(PauseSettings.DEFAULT.onboardingComplete).isFalse()
    }
}

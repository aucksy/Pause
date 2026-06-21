package com.pause.app.domain.repository

import com.pause.app.domain.model.PauseSettings
import kotlinx.coroutines.flow.Flow

/** Reads and writes the user's Pause configuration. Backed by DataStore. */
interface SettingsRepository {
    val settings: Flow<PauseSettings>

    suspend fun snapshot(): PauseSettings

    suspend fun setSelectedPackages(packages: Set<String>)

    suspend fun setIntervalMinutes(minutes: Int)

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setOnboardingComplete(complete: Boolean)
}

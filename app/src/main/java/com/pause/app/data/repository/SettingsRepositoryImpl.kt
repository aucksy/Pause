package com.pause.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pause.app.domain.model.IntervalOptions
import com.pause.app.domain.model.PauseSettings
import com.pause.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val SELECTED = stringSetPreferencesKey("selected_packages")
        val INTERVAL = intPreferencesKey("interval_minutes")
        val ENABLED = booleanPreferencesKey("is_enabled")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
    }

    override val settings: Flow<PauseSettings> = dataStore.data
        // Never let a transient read error (IOException) terminate the collectors in the
        // ViewModel and the accessibility service — fall back to empty (defaults).
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
        PauseSettings(
            selectedPackages = p[Keys.SELECTED] ?: emptySet(),
            intervalMinutes = IntervalOptions.sanitize(p[Keys.INTERVAL] ?: IntervalOptions.DEFAULT),
            isEnabled = p[Keys.ENABLED] ?: false,
            onboardingComplete = p[Keys.ONBOARDING] ?: false,
        )
    }

    override suspend fun snapshot(): PauseSettings = settings.first()

    override suspend fun setSelectedPackages(packages: Set<String>) {
        dataStore.edit { it[Keys.SELECTED] = packages }
    }

    override suspend fun setIntervalMinutes(minutes: Int) {
        dataStore.edit { it[Keys.INTERVAL] = IntervalOptions.sanitize(minutes) }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING] = complete }
    }
}

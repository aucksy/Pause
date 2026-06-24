package com.pause.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pause.app.domain.model.DetectionMode
import com.pause.app.domain.model.IntervalOptions
import com.pause.app.domain.model.MessagePresets
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
        val CUSTOM_IMAGE = stringPreferencesKey("custom_image_path")
        val OVERLAY_MESSAGE = stringPreferencesKey("overlay_message")
        val SHOW_IMAGE = booleanPreferencesKey("show_image")
        val SHOW_TEXT = booleanPreferencesKey("show_text")
        val DETECTION_MODE = stringPreferencesKey("detection_mode")
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
            customImagePath = p[Keys.CUSTOM_IMAGE],
            overlayMessage = p[Keys.OVERLAY_MESSAGE] ?: MessagePresets.default,
            showImage = p[Keys.SHOW_IMAGE] ?: true,
            showText = p[Keys.SHOW_TEXT] ?: true,
            detectionMode = DetectionMode.fromName(p[Keys.DETECTION_MODE]),
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

    override suspend fun setCustomImagePath(path: String?) {
        dataStore.edit {
            if (path == null) it.remove(Keys.CUSTOM_IMAGE) else it[Keys.CUSTOM_IMAGE] = path
        }
    }

    override suspend fun setOverlayMessage(message: String) {
        dataStore.edit { it[Keys.OVERLAY_MESSAGE] = MessagePresets.clamp(message) }
    }

    override suspend fun setShowImage(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_IMAGE] = show }
    }

    override suspend fun setShowText(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_TEXT] = show }
    }

    override suspend fun setDetectionMode(mode: DetectionMode) {
        dataStore.edit { it[Keys.DETECTION_MODE] = mode.name }
    }
}

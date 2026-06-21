package com.pause.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.system.CustomImageStore
import com.pause.app.domain.model.PauseSettings
import com.pause.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The single source of UI state, shared across onboarding and the main screen. It mirrors the
 * persisted [PauseSettings] and exposes intent-style actions that write straight back to
 * DataStore — the accessibility service observes the same store and reacts on its own.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val customImageStore: CustomImageStore,
) : ViewModel() {

    val settings: StateFlow<PauseSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PauseSettings.DEFAULT)

    private val _imageProcessing = MutableStateFlow(false)
    /** True while a picked image is being resized/saved, so the UI can show progress. */
    val imageProcessing: StateFlow<Boolean> = _imageProcessing.asStateFlow()

    private val _imageError = MutableStateFlow(false)
    /** Set when the last image import failed; the UI clears it once shown. */
    val imageError: StateFlow<Boolean> = _imageError.asStateFlow()

    private val _startOnHome = MutableStateFlow<Boolean?>(null)
    /**
     * Null until the first persisted value is read; then true if onboarding is already complete.
     * Captured from a direct [SettingsRepository.snapshot] read so the routing decision never
     * depends on the lazily-subscribed [settings] StateFlow (which would still hold DEFAULT here).
     */
    val startOnHome: StateFlow<Boolean?> = _startOnHome.asStateFlow()

    init {
        viewModelScope.launch {
            val first = settingsRepository.snapshot()
            // Heal a stored image path whose file no longer exists (e.g. after a backup restore),
            // so the UI doesn't claim a custom image while the overlay shows the default.
            val path = first.customImagePath
            if (path != null && !customImageStore.exists(path)) {
                settingsRepository.setCustomImagePath(null)
            }
            _startOnHome.value = first.onboardingComplete
        }
    }

    fun setSelectedPackages(packages: Set<String>) = viewModelScope.launch {
        settingsRepository.setSelectedPackages(packages)
    }

    fun toggleApp(packageName: String) {
        val current = settings.value.selectedPackages
        val next = if (packageName in current) current - packageName else current + packageName
        setSelectedPackages(next)
    }

    fun setInterval(minutes: Int) = viewModelScope.launch {
        settingsRepository.setIntervalMinutes(minutes)
    }

    fun setEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setEnabled(enabled)
    }

    fun completeOnboarding() = viewModelScope.launch {
        settingsRepository.setOnboardingComplete(true)
        settingsRepository.setEnabled(true)
    }

    fun setCustomImage(uri: Uri) = viewModelScope.launch {
        _imageError.value = false
        _imageProcessing.value = true
        val path = customImageStore.save(uri)
        if (path != null) settingsRepository.setCustomImagePath(path) else _imageError.value = true
        _imageProcessing.value = false
    }

    fun clearCustomImage() = viewModelScope.launch {
        // Clear the reference first so the overlay stops pointing at the file before it's deleted.
        settingsRepository.setCustomImagePath(null)
        customImageStore.delete()
    }

    fun consumeImageError() {
        _imageError.value = false
    }

    fun setOverlayMessage(message: String) = viewModelScope.launch {
        settingsRepository.setOverlayMessage(message)
    }

    fun setShowImage(show: Boolean) = viewModelScope.launch {
        settingsRepository.setShowImage(show)
    }

    fun setShowText(show: Boolean) = viewModelScope.launch {
        settingsRepository.setShowText(show)
    }
}

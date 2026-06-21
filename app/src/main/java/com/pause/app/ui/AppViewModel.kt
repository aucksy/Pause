package com.pause.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    val settings: StateFlow<PauseSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PauseSettings.DEFAULT)

    private val _startOnHome = MutableStateFlow<Boolean?>(null)
    /**
     * Null until the first persisted value is read; then true if onboarding is already complete.
     * Captured from a direct [SettingsRepository.snapshot] read so the routing decision never
     * depends on the lazily-subscribed [settings] StateFlow (which would still hold DEFAULT here).
     */
    val startOnHome: StateFlow<Boolean?> = _startOnHome.asStateFlow()

    init {
        viewModelScope.launch {
            _startOnHome.value = settingsRepository.snapshot().onboardingComplete
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
}

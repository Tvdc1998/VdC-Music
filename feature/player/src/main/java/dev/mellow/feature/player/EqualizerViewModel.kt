package dev.mellow.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mellow.core.data.repository.EqualizerRepository
import dev.mellow.core.model.EqualizerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerRepository: EqualizerRepository,
) : ViewModel() {

    val equalizerState: StateFlow<EqualizerState> = equalizerRepository.equalizerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EqualizerState(),
        )

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            equalizerRepository.setEnabled(enabled)
        }
    }

    fun selectPreset(presetName: String) {
        viewModelScope.launch {
            equalizerRepository.selectPreset(presetName)
        }
    }

    fun setBandLevel(bandIndex: Int, levelDb: Int) {
        viewModelScope.launch {
            equalizerRepository.setBandLevel(bandIndex, levelDb)
        }
    }

    fun setBassBoost(strength: Int) {
        viewModelScope.launch {
            equalizerRepository.setBassBoost(strength)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            equalizerRepository.setPlaybackSpeed(speed)
        }
    }

    fun setPitchWithSpeed(enabled: Boolean) {
        viewModelScope.launch {
            equalizerRepository.setPitchWithSpeed(enabled)
        }
    }

    fun reset() {
        viewModelScope.launch {
            equalizerRepository.reset()
        }
    }
}

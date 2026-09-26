package dev.mellow.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mellow.core.common.MellowResult
import dev.mellow.core.data.preferences.DisplayPreferences
import dev.mellow.core.data.preferences.DownloadPreferences
import dev.mellow.core.data.repository.DownloadRepository
import dev.mellow.core.data.scanner.LocalMediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val downloadPreferences: DownloadPreferences,
    private val displayPreferences: DisplayPreferences,
    private val downloadRepository: DownloadRepository,
    private val localMediaScanner: LocalMediaScanner,
    private val userRepository: dev.mellow.core.data.repository.UserRepository,
    @Named("appVersion") val appVersion: String,
) : ViewModel() {

    private val _isLocalScanning = MutableStateFlow(false)
    val isLocalScanning: StateFlow<Boolean> = _isLocalScanning.asStateFlow()

    private val _localScanResult = MutableStateFlow<String?>(null)
    val localScanResult: StateFlow<String?> = _localScanResult.asStateFlow()

    fun hasStoragePermission(): Boolean = localMediaScanner.hasStoragePermission()

    fun scanLocalLibrary() {
        if (_isLocalScanning.value) return
        viewModelScope.launch {
            _isLocalScanning.value = true
            _localScanResult.value = null
            val result = localMediaScanner.scanLocalLibrary()
            _isLocalScanning.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _localScanResult.value = "Scanned $count tracks"
                if (count > 0) {
                    userRepository.switchServer(LocalMediaScanner.LOCAL_SERVER_ID)
                }
            } else {
                _localScanResult.value = "Scan failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    val lowPowerMode: StateFlow<Boolean> = displayPreferences.lowPowerMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setLowPowerMode(enabled: Boolean) {
        viewModelScope.launch { displayPreferences.setLowPowerMode(enabled) }
    }

    val downloadQuality: StateFlow<String> = downloadPreferences.downloadQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadPreferences.DEFAULT_QUALITY)

    val wifiOnly: StateFlow<Boolean> = downloadPreferences.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val storageCap: StateFlow<Long> = downloadPreferences.storageCap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadPreferences.DEFAULT_STORAGE_CAP_BYTES)

    val autoCleanupDays: StateFlow<Int> = downloadPreferences.autoCleanupDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadPreferences.DEFAULT_AUTO_CLEANUP_DAYS)

    val totalDownloadedBytes: StateFlow<Long> = downloadRepository.getTotalDownloadedBytes()
        .map { result -> (result as? MellowResult.Success)?.data ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch { downloadPreferences.setDownloadQuality(quality) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { downloadPreferences.setWifiOnly(enabled) }
    }

    fun setStorageCap(bytes: Long) {
        viewModelScope.launch { downloadPreferences.setStorageCap(bytes) }
    }

    fun setAutoCleanupDays(days: Int) {
        viewModelScope.launch { downloadPreferences.setAutoCleanupDays(days) }
    }

    fun clearAllDownloads() {
        viewModelScope.launch { downloadRepository.clearAllDownloads() }
    }
}

package com.lin0721.linmusic.core.update

import android.content.Context
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.update.data.ApkDownloader
import com.lin0721.linmusic.core.update.data.ApkInstaller
import com.lin0721.linmusic.core.update.data.DownloadState
import com.lin0721.linmusic.core.update.data.UpdateNotificationHelper
import com.lin0721.linmusic.core.update.data.UpdateRepository
import com.lin0721.linmusic.core.update.domain.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "UpdateManager"

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data class Available(val info: UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateInfo, val progress: Int) : UpdateUiState()
    data class ReadyToInstall(val info: UpdateInfo, val file: File) : UpdateUiState()
    data class DownloadFailed(val info: UpdateInfo, val message: String) : UpdateUiState()
}

// of ，Application  and 、
class UpdateManager(
    private val context: Context,
    private val updateRepository: UpdateRepository,
    private val apkDownloader: ApkDownloader,
    private val apkInstaller: ApkInstaller,
    private val settingsPreferences: SettingsPreferences,
    private val notificationHelper: UpdateNotificationHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    private var downloadJob: Job? = null

    fun checkForUpdate(manual: Boolean) {
        scope.launch {
            val currentState = _uiState.value
            if (currentState is UpdateUiState.Downloading) {
                if (manual) {
                    ToastManager.showToast("Downloading in background (${currentState.progress}%)")
                    _isDialogVisible.value = true
                }
                return@launch
            }
            if (currentState is UpdateUiState.ReadyToInstall) {
                if (manual) {
                    _isDialogVisible.value = true
                }
                return@launch
            }

            if (!manual && !settingsPreferences.autoCheckUpdateEnabled.first()) return@launch

            val allowPrerelease = settingsPreferences.allowPrereleaseChannel.first()
            updateRepository.fetchLatestUpdate(allowPrerelease)
                .onSuccess { info ->
                    if (info == null) {
                        if (manual) ToastManager.showToast("Already the latest version")
                        return@onSuccess
                    }
                    val ignoredTag = settingsPreferences.ignoredUpdateTag.first()
                    if (!manual && info.versionName == ignoredTag) return@onSuccess
                    _uiState.value = UpdateUiState.Available(info)
                    _isDialogVisible.value = true
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Check for updates failed", e)
                    if (manual) ToastManager.showToast("Failed to check for updates, please retry later")
                }
        }
    }

    // CloseDownload，Download
    fun startDownload() {
        if (_uiState.value is UpdateUiState.Downloading) {
            _isDialogVisible.value = false
            return
        }
        val info = currentInfoOrNull() ?: return
        _isDialogVisible.value = false
        ToastManager.showToast("Downloading new version in background...")

        downloadJob?.cancel()
        downloadJob = scope.launch {
            apkDownloader.download(info.apkDownloadUrl, info.versionName).collect { state ->
                when (state) {
                    is DownloadState.Downloading -> {
                        _uiState.value = UpdateUiState.Downloading(info, state.progress)
                        notificationHelper.showDownloading(info.versionName, state.progress)
                    }
                    is DownloadState.Success -> {
                        _uiState.value = UpdateUiState.ReadyToInstall(info, state.file)
                        notificationHelper.showDownloadSuccess(
                            info.versionName,
                            apkInstaller.buildInstallIntent(state.file)
                        )
                        _isDialogVisible.value = true
                        tryInstall(state.file)
                    }
                    is DownloadState.Failed -> {
                        _uiState.value = UpdateUiState.DownloadFailed(info, state.message)
                        notificationHelper.showDownloadFailed(state.message)
                        ToastManager.showToast("Download failed, please retry later")
                    }
                }
            }
        }
    }

    // DownloadInstall，"Install unknown apps"，Back of InstallRetry
    fun retryInstall() {
        val state = _uiState.value
        if (state is UpdateUiState.ReadyToInstall) tryInstall(state.file)
    }

    private fun tryInstall(file: File) {
        if (!apkInstaller.canRequestPackageInstalls()) {
            ToastManager.showToast("Please allow installing unknown apps first")
            context.startActivity(apkInstaller.buildUnknownSourceSettingsIntent())
            return
        }
        apkInstaller.install(file)
    }

    fun ignoreCurrentVersion() {
        val info = currentInfoOrNull() ?: return
        scope.launch { settingsPreferences.saveIgnoredUpdateTag(info.versionName) }
        _isDialogVisible.value = false
        _uiState.value = UpdateUiState.Idle
        notificationHelper.cancelNotification()
    }

    fun dismiss() {
        _isDialogVisible.value = false
        if (_uiState.value !is UpdateUiState.Downloading) {
            _uiState.value = UpdateUiState.Idle
        }
    }

    private fun currentInfoOrNull(): UpdateInfo? = when (val state = _uiState.value) {
        is UpdateUiState.Available -> state.info
        is UpdateUiState.DownloadFailed -> state.info
        is UpdateUiState.Downloading -> state.info
        else -> null
    }
}

package com.example.notificationlog.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notificationlog.AppContainer
import com.example.notificationlog.NotificationLogApp
import com.example.notificationlog.domain.AppInfo
import com.example.notificationlog.domain.NotificationQuery
import com.example.notificationlog.domain.RecordMode
import com.example.notificationlog.domain.RetentionPeriod
import com.example.notificationlog.ui.list.ExportController
import com.example.notificationlog.ui.list.ExportState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 설정 화면: 보관 기간, 진행 중 알림 제외, 전체 내보내기, 데이터 삭제 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settings = container.settingsRepository
    private val notifications = container.notificationRepository
    private val exportController = ExportController(container.notificationExporter)

    val retention: StateFlow<RetentionPeriod> = settings.retention
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RetentionPeriod.DEFAULT)

    val recordMode: StateFlow<RecordMode> = settings.recordMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordMode.ALL)

    val excludeOngoing: StateFlow<Boolean> = settings.excludeOngoing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val totalCount: StateFlow<Int> = notifications.observeCount(NotificationQuery())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val exportState: StateFlow<ExportState> = exportController.state

    fun setRetention(period: RetentionPeriod) {
        viewModelScope.launch {
            settings.setRetention(period)
            // 기간을 줄였다면 즉시 정리
            notifications.cleanupExpired()
        }
    }

    fun setExcludeOngoing(exclude: Boolean) {
        viewModelScope.launch { settings.setExcludeOngoing(exclude) }
    }

    fun deleteAll() {
        viewModelScope.launch { notifications.deleteAll() }
    }

    fun exportAll(uri: Uri) = exportController.start(viewModelScope, uri, NotificationQuery())

    fun dismissExport() = exportController.dismiss()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NotificationLogApp
                SettingsViewModel(app.container)
            }
        }
    }
}

/** 앱 선택 목록의 한 줄 */
data class SelectableApp(val app: AppInfo, val selected: Boolean)

/** 앱별 기록 설정 화면: 기록 모드 + 허용/제외할 앱 선택 (Room 에 저장) */
@OptIn(ExperimentalCoroutinesApi::class)
class AppSelectionViewModel(private val container: AppContainer) : ViewModel() {

    private val settings = container.settingsRepository
    private val rules = container.appRuleRepository

    val mode: StateFlow<RecordMode> = settings.recordMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecordMode.ALL)

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>?>(null)

    private val selected = mode.flatMapLatest { rules.observeSelected(it) }

    /** null 이면 앱 목록 로딩 중 */
    val apps: StateFlow<List<SelectableApp>?> = combine(_apps, selected, _search) { apps, selected, search ->
        apps?.filter { search.isBlank() || it.label.contains(search, true) || it.packageName.contains(search, true) }
            ?.map { SelectableApp(it, it.packageName in selected) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedCount: StateFlow<Int> = selected.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            val installed = container.installedAppsRepository.launchableApps()
            // 런처에 없지만 알림을 보낸 적 있는 앱(시스템 앱 등)도 선택할 수 있게 포함
            val recorded = container.notificationRepository.recordedApps()
                .map { AppInfo(it.packageName, it.appName) }
            _apps.value = (installed + recorded).distinctBy { it.packageName }
        }
    }

    fun setMode(mode: RecordMode) {
        viewModelScope.launch { settings.setRecordMode(mode) }
    }

    fun setSearch(text: String) {
        _search.value = text
    }

    fun toggle(item: SelectableApp) {
        viewModelScope.launch { rules.setSelected(mode.value, item.app, !item.selected) }
    }

    fun clearSelection() {
        viewModelScope.launch { rules.clear(mode.value) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NotificationLogApp
                AppSelectionViewModel(app.container)
            }
        }
    }
}

package com.example.notificationlog.ui.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notificationlog.AppContainer
import com.example.notificationlog.NotificationLogApp
import com.example.notificationlog.domain.KeywordFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 필터 관리 화면 ViewModel: 필터 추가·수정·삭제·활성화 */
class FilterViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.filterRepository

    val filters: StateFlow<List<KeywordFilter>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 필터 대상 앱 선택용: 설치된 앱 + 기록된 적 있는 앱 (패키지명, 이름) */
    private val _selectableApps = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val selectableApps: StateFlow<List<Pair<String, String>>> = _selectableApps.asStateFlow()

    fun loadSelectableApps() {
        if (_selectableApps.value.isNotEmpty()) return
        viewModelScope.launch {
            val installed = container.installedAppsRepository.launchableApps().map { it.packageName to it.label }
            val recorded = container.notificationRepository.recordedApps().map { it.packageName to it.appName }
            _selectableApps.value = (installed + recorded).distinctBy { it.first }.sortedBy { it.second }
        }
    }

    fun save(filter: KeywordFilter) {
        viewModelScope.launch { repository.save(filter) }
    }

    fun setEnabled(filter: KeywordFilter, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(filter.id, enabled) }
    }

    fun delete(filter: KeywordFilter) {
        viewModelScope.launch { repository.delete(filter.id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NotificationLogApp
                FilterViewModel(app.container)
            }
        }
    }
}

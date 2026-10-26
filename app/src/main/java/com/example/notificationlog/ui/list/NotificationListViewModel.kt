package com.example.notificationlog.ui.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.notificationlog.AppContainer
import com.example.notificationlog.NotificationLogApp
import com.example.notificationlog.domain.KeywordFilter
import com.example.notificationlog.domain.NotificationItem
import com.example.notificationlog.domain.NotificationQuery
import com.example.notificationlog.domain.RecordedApp
import com.example.notificationlog.export.ExportResult
import com.example.notificationlog.export.NotificationExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 내보내기 진행 상태 */
sealed interface ExportState {
    data object Idle : ExportState
    data object Running : ExportState
    data class Done(val result: ExportResult) : ExportState
    data class Failed(val message: String) : ExportState
}

/** 여러 화면에서 재사용하는 내보내기 실행기 (백그라운드에서 실행, 진행 상태 노출) */
class ExportController(private val exporter: NotificationExporter) {
    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun start(scope: CoroutineScope, uri: Uri, query: NotificationQuery) {
        if (_state.value == ExportState.Running) return
        _state.value = ExportState.Running
        scope.launch {
            _state.value = try {
                ExportState.Done(exporter.export(uri, query))
            } catch (e: Exception) {
                ExportState.Failed(e.message ?: "알 수 없는 오류가 발생했습니다")
            }
        }
    }

    fun dismiss() {
        _state.value = ExportState.Idle
    }
}

/**
 * 홈 / 검색 / 필터 결과 화면이 공유하는 목록 ViewModel.
 * 조회 조건(NotificationQuery)을 바꾸면 Paging 목록과 건수가 자동으로 갱신된다.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotificationListViewModel(
    container: AppContainer,
    initialQuery: NotificationQuery,
    filterId: Long?,
) : ViewModel() {

    private val repository = container.notificationRepository
    private val exportController = ExportController(container.notificationExporter)

    private val _query = MutableStateFlow(initialQuery)
    val query: StateFlow<NotificationQuery> = _query.asStateFlow()

    // 검색어 입력 중 매 글자마다 쿼리가 실행되지 않도록 약간 지연한다.
    private val debouncedQuery = _query.debounce(250).distinctUntilChanged()

    val items: Flow<PagingData<NotificationItem>> =
        debouncedQuery.flatMapLatest { repository.pagedItems(it) }.cachedIn(viewModelScope)

    val count: StateFlow<Int> = debouncedQuery
        .flatMapLatest { repository.observeCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val recordedApps: StateFlow<List<RecordedApp>> = repository.observeRecordedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filters: StateFlow<List<KeywordFilter>> = container.filterRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exportState: StateFlow<ExportState> = exportController.state

    private var deleteJob: Job? = null

    init {
        // 필터 결과 화면: 필터가 수정되면 조건도 함께 갱신
        if (filterId != null) {
            viewModelScope.launch {
                container.filterRepository.observe(filterId).collect { filter ->
                    _query.update { it.copy(filter = filter) }
                }
            }
        }
    }

    fun setKeyword(keyword: String) = _query.update { it.copy(keyword = keyword) }
    fun setPackages(packages: Set<String>) = _query.update { it.copy(packages = packages) }
    fun setDateRange(from: Long?, to: Long?) = _query.update { it.copy(fromMillis = from, toMillis = to) }
    fun setFavoritesOnly(only: Boolean) = _query.update { it.copy(favoritesOnly = only) }
    fun setFilter(filter: KeywordFilter?) = _query.update { it.copy(filter = filter) }

    fun toggleFavorite(item: NotificationItem) {
        viewModelScope.launch { repository.setFavorite(item.id, !item.isFavorite) }
    }

    fun delete(item: NotificationItem) {
        deleteJob = viewModelScope.launch { repository.delete(item.id) }
    }

    fun undoDelete() {
        val pending = deleteJob
        viewModelScope.launch {
            pending?.join()
            repository.undoLastDelete()
        }
    }

    /** 현재 화면에 적용된 조건 그대로 내보낸다. */
    fun export(uri: Uri) = exportController.start(viewModelScope, uri, _query.value)

    fun dismissExport() = exportController.dismiss()

    companion object {
        fun factory(
            initialQuery: NotificationQuery = NotificationQuery(),
            filterId: Long? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NotificationLogApp
                NotificationListViewModel(app.container, initialQuery, filterId)
            }
        }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.notificationlog.service.NotificationAccess
import com.example.notificationlog.ui.common.ExportStateDialogs
import com.example.notificationlog.ui.common.PagedNotificationList
import com.example.notificationlog.ui.common.PermissionCard
import com.example.notificationlog.ui.common.QueryChips
import com.example.notificationlog.ui.common.rememberXlsxExportLauncher
import com.example.notificationlog.ui.list.NotificationListViewModel
import kotlinx.coroutines.launch

/** 홈 / 알림 목록 화면: 최신순 무한 스크롤 + 앱·기간·필터·키워드·즐겨찾기 조건 + 내보내기 */
@Composable
fun HomeScreen(
    accessGranted: Boolean,
    onOpenDetail: (Long) -> Unit,
    viewModel: NotificationListViewModel = viewModel(factory = NotificationListViewModel.factory()),
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val count by viewModel.count.collectAsStateWithLifecycle()
    val apps by viewModel.recordedApps.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val launchExport = rememberXlsxExportLauncher { uri -> viewModel.export(uri) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("알림 기록")
                        Text(
                            "${count}건",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = launchExport) {
                        Icon(Icons.Filled.Share, contentDescription = "XLSX로 내보내기")
                    }
                },
            )
        },
    ) { padding ->
        PagedNotificationList(
            pagingItems = pagingItems,
            highlightTerms = query.highlightTerms,
            emptyMessage = if (accessGranted) "조건에 맞는 알림이 없습니다.\n새 알림이 도착하면 여기에 기록됩니다."
            else "알림 접근 권한을 허용하면 이후 도착하는 알림이 기록됩니다.",
            onClick = { onOpenDetail(it.id) },
            onToggleFavorite = viewModel::toggleFavorite,
            onDelete = { item ->
                viewModel.delete(item)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "알림을 삭제했습니다",
                        actionLabel = "실행 취소",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
            },
            modifier = Modifier.padding(padding),
            header = {
                if (!accessGranted) {
                    item(key = "permission") {
                        PermissionCard(onOpenSettings = { NotificationAccess.openSettings(context) })
                    }
                }
                item(key = "chips") {
                    QueryChips(
                        query = query,
                        recordedApps = apps,
                        onFavoritesChange = viewModel::setFavoritesOnly,
                        onPackagesChange = viewModel::setPackages,
                        onDateRangeChange = viewModel::setDateRange,
                        filters = filters,
                        onFilterChange = viewModel::setFilter,
                        onKeywordChange = viewModel::setKeyword,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            },
        )
    }

    ExportStateDialogs(exportState, onDismiss = viewModel::dismissExport)
}

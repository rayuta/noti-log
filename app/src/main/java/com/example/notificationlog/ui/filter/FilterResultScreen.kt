@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.notificationlog.ui.common.ExportStateDialogs
import com.example.notificationlog.ui.common.PagedNotificationList
import com.example.notificationlog.ui.common.QueryChips
import com.example.notificationlog.ui.common.rememberXlsxExportLauncher
import com.example.notificationlog.ui.list.NotificationListViewModel
import kotlinx.coroutines.launch

/** 필터 결과 화면: 선택한 필터 조건에 맞는 알림 목록 (+ 앱/기간/즐겨찾기 추가 조건, 내보내기) */
@Composable
fun FilterResultScreen(
    filterId: Long,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: NotificationListViewModel = viewModel(
        factory = NotificationListViewModel.factory(filterId = filterId),
    ),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val count by viewModel.count.collectAsStateWithLifecycle()
    val apps by viewModel.recordedApps.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val launchExport = rememberXlsxExportLauncher { uri -> viewModel.export(uri) }
    val filter = query.filter

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(filter?.name ?: "필터")
                        Text(
                            "${count}건",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                },
                actions = {
                    IconButton(onClick = launchExport, enabled = filter != null) {
                        Icon(Icons.Filled.Share, contentDescription = "XLSX로 내보내기")
                    }
                },
            )
        },
    ) { padding ->
        PagedNotificationList(
            pagingItems = pagingItems,
            highlightTerms = query.highlightTerms,
            emptyMessage = "이 필터에 맞는 알림이 없습니다.",
            onClick = { onOpenDetail(it.id) },
            onToggleFavorite = viewModel::toggleFavorite,
            onDelete = { item ->
                viewModel.delete(item)
                scope.launch {
                    val result = snackbarHostState.showSnackbar("알림을 삭제했습니다", "실행 취소")
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
            },
            modifier = Modifier.padding(padding).navigationBarsPadding(),
            header = {
                item(key = "desc") {
                    Text(
                        "키워드: " + (filter?.keywords?.joinToString(", ") ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                item(key = "chips") {
                    QueryChips(
                        query = query,
                        recordedApps = apps,
                        onFavoritesChange = viewModel::setFavoritesOnly,
                        onPackagesChange = viewModel::setPackages,
                        onDateRangeChange = viewModel::setDateRange,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            },
        )
    }

    ExportStateDialogs(exportState, onDismiss = viewModel::dismissExport)
}

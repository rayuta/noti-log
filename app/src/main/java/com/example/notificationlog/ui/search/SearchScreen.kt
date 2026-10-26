@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.notificationlog.ui.common.CenterMessage
import com.example.notificationlog.ui.common.ExportStateDialogs
import com.example.notificationlog.ui.common.PagedNotificationList
import com.example.notificationlog.ui.common.QueryChips
import com.example.notificationlog.ui.common.rememberXlsxExportLauncher
import com.example.notificationlog.ui.list.NotificationListViewModel
import kotlinx.coroutines.launch

/**
 * 검색 화면: 제목/본문 키워드 검색 (공백으로 여러 단어 AND 검색).
 * 앱/기간 칩으로 검색 범위를 좁힐 수 있고, 결과에서 검색어를 강조 표시한다.
 */
@Composable
fun SearchScreen(
    onOpenDetail: (Long) -> Unit,
    viewModel: NotificationListViewModel = viewModel(factory = NotificationListViewModel.factory()),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val count by viewModel.count.collectAsStateWithLifecycle()
    val apps by viewModel.recordedApps.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val launchExport = rememberXlsxExportLauncher { uri -> viewModel.export(uri) }

    val hasCondition = query.keyword.isNotBlank() || query.packages.isNotEmpty() ||
        query.fromMillis != null || query.toMillis != null || query.favoritesOnly

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("검색") },
                actions = {
                    if (hasCondition) {
                        IconButton(onClick = launchExport) {
                            Icon(Icons.Filled.Share, contentDescription = "검색 결과 XLSX로 내보내기")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query.keyword,
                onValueChange = viewModel::setKeyword,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                placeholder = { Text("제목 또는 본문 검색") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.keyword.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setKeyword("") }) { Icon(Icons.Filled.Clear, "지우기") }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )
            QueryChips(
                query = query,
                recordedApps = apps,
                onFavoritesChange = viewModel::setFavoritesOnly,
                onPackagesChange = viewModel::setPackages,
                onDateRangeChange = viewModel::setDateRange,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            if (!hasCondition) {
                CenterMessage("검색어를 입력하거나 앱·기간을 선택하세요.\n여러 단어는 공백으로 구분하면 모두 포함된 알림을 찾습니다.")
            } else {
                Text(
                    "검색 결과 ${count}건",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                PagedNotificationList(
                    pagingItems = pagingItems,
                    highlightTerms = query.highlightTerms,
                    emptyMessage = "검색 결과가 없습니다.",
                    onClick = { onOpenDetail(it.id) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDelete = { item ->
                        viewModel.delete(item)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("알림을 삭제했습니다", "실행 취소")
                            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                        }
                    },
                )
            }
        }
    }

    ExportStateDialogs(exportState, onDismiss = viewModel::dismissExport)
}

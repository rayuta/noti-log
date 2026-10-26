@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notificationlog.NotificationLogApp
import com.example.notificationlog.data.repository.NotificationRepository
import com.example.notificationlog.domain.NotificationCategories
import com.example.notificationlog.domain.NotificationItem
import com.example.notificationlog.ui.common.AppIcon
import com.example.notificationlog.ui.common.CenterMessage
import com.example.notificationlog.ui.common.EMPTY_CONTENT
import com.example.notificationlog.ui.common.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 상세 화면 상태: 로딩 중 / 항목 / 삭제됨(없음) */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Loaded(val item: NotificationItem) : DetailUiState
    data object NotFound : DetailUiState
}

class DetailViewModel(private val repository: NotificationRepository, private val id: Long) : ViewModel() {

    val state: StateFlow<DetailUiState> = repository.observeItem(id)
        .map { item -> if (item != null) DetailUiState.Loaded(item) else DetailUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    fun toggleFavorite(item: NotificationItem) {
        viewModelScope.launch { repository.setFavorite(item.id, !item.isFavorite) }
    }

    fun delete() {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        fun factory(id: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NotificationLogApp
                DetailViewModel(app.container.notificationRepository, id)
            }
        }
    }
}

/** 알림 상세: 전체 제목·본문, 앱 정보, 수신 시각, 원본 앱 열기 */
@Composable
fun DetailScreen(
    id: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(key = "detail_$id", factory = DetailViewModel.factory(id)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    val item = (state as? DetailUiState.Loaded)?.item

    // 삭제 후에는 이전 화면으로 돌아간다.
    LaunchedEffect(state, deleted) {
        if (deleted && state is DetailUiState.NotFound) onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("알림 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                            Icon(
                                if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (item.isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                                tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "삭제")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            DetailUiState.Loading -> Unit
            DetailUiState.NotFound -> CenterMessage("삭제되었거나 존재하지 않는 알림입니다.", Modifier.padding(padding))
            is DetailUiState.Loaded -> DetailContent(
                item = s.item,
                onOpenApp = { openApp(context, s.item.packageName) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("알림 삭제") },
            text = { Text("이 알림 기록을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    deleted = true
                    viewModel.delete()
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun DetailContent(item: NotificationItem, onOpenApp: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(item.packageName, item.appName, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            Formatters.detailTime(item.postedAt),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()

        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    item.title.ifBlank { "(제목 없음)" },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    item.text.ifBlank { EMPTY_CONTENT },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        val category = NotificationCategories.label(item.category, item.channelId)
        if (category.isNotBlank()) {
            HorizontalDivider()
            Text("카테고리", style = MaterialTheme.typography.labelLarge)
            Text(category, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.padding(4.dp))
        Button(onClick = onOpenApp, modifier = Modifier.fillMaxWidth()) {
            Text("원본 앱 열기")
        }
    }
}

/** 알림을 보낸 앱을 실행한다. (원래 알림의 PendingIntent 는 저장할 수 없으므로 앱의 시작 화면을 연다) */
private fun openApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent == null) {
        Toast.makeText(context, "이 앱을 열 수 없습니다", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "이 앱을 열 수 없습니다", Toast.LENGTH_SHORT).show()
    }
}

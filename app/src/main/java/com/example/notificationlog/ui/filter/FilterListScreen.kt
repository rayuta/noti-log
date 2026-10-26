@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.data.repository.FilterRepository
import com.example.notificationlog.domain.KeywordFilter
import com.example.notificationlog.ui.common.AppPickerDialog
import com.example.notificationlog.ui.common.CenterMessage

/** 필터 관리 화면: 목록, 활성화 토글, 추가/수정/삭제. 필터를 누르면 해당 조건의 알림 목록으로 이동한다. */
@Composable
fun FilterListScreen(
    onOpenFilter: (Long) -> Unit,
    viewModel: FilterViewModel = viewModel(factory = FilterViewModel.Factory),
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val selectableApps by viewModel.selectableApps.collectAsStateWithLifecycle()
    // null: 닫힘, id=0: 새 필터, 그 외: 수정
    var editing by remember { mutableStateOf<KeywordFilter?>(null) }

    LaunchedEffect(Unit) { viewModel.loadSelectableApps() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("필터") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = KeywordFilter(name = "", keywords = emptyList()) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("필터 추가") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "키워드 중 하나라도 제목 또는 본문에 포함된 알림을 모아 봅니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (filters.isEmpty()) {
                item { CenterMessage("필터가 없습니다. '필터 추가'로 만들어 보세요.") }
            }
            items(filters, key = { it.id }) { filter ->
                FilterCard(
                    filter = filter,
                    appNames = selectableApps.toMap(),
                    onClick = { onOpenFilter(filter.id) },
                    onEdit = { editing = filter },
                    onToggle = { viewModel.setEnabled(filter, it) },
                )
            }
        }
    }

    editing?.let { target ->
        FilterEditDialog(
            initial = target,
            selectableApps = selectableApps,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
            onDelete = if (target.id != 0L) ({ viewModel.delete(target); editing = null }) else null,
        )
    }
}

@Composable
private fun FilterCard(
    filter: KeywordFilter,
    appNames: Map<String, String>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(filter.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    filter.keywords.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (filter.targetPackages.isEmpty()) "대상: 모든 앱"
                    else "대상: " + filter.targetPackages.joinToString(", ") { appNames[it] ?: it },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "수정") }
            Switch(checked = filter.enabled, onCheckedChange = onToggle)
        }
    }
}

/** 필터 추가/수정 대화상자: 이름, 키워드(쉼표 구분), 대상 앱, 활성화 여부 */
@Composable
private fun FilterEditDialog(
    initial: KeywordFilter,
    selectableApps: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (KeywordFilter) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial.name) }
    var keywordsText by remember { mutableStateOf(initial.keywords.joinToString(", ")) }
    var targets by remember { mutableStateOf(initial.targetPackages) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var showAppPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val keywords = FilterRepository.parseKeywords(keywordsText)
    val valid = name.isNotBlank() && keywords.isNotEmpty()
    val appNames = selectableApps.toMap()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "필터 추가" else "필터 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text("키워드") },
                    placeholder = { Text("예: 배송, 도착, 택배") },
                    supportingText = { Text("쉼표(,)로 구분 · 하나라도 포함되면 일치") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("대상 앱", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (targets.isEmpty()) "모든 앱"
                    else targets.joinToString(", ") { appNames[it] ?: it },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedButton(onClick = { showAppPicker = true }) { Text("대상 앱 선택") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("활성화", Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(initial.copy(name = name.trim(), keywords = keywords, targetPackages = targets, enabled = enabled))
                },
            ) { Text("저장") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )

    if (showAppPicker) {
        AppPickerDialog(
            title = "대상 앱 (선택 안 하면 모든 앱)",
            apps = selectableApps,
            initialSelection = targets,
            onDismiss = { showAppPicker = false },
            onConfirm = { targets = it; showAppPicker = false },
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("필터 삭제") },
            text = { Text("'${initial.name}' 필터를 삭제할까요? 알림 기록은 삭제되지 않습니다.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

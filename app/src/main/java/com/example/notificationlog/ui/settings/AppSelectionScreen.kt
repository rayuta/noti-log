@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.domain.RecordMode
import com.example.notificationlog.ui.common.AppIcon

/** 앱별 기록 설정: 모든 앱 / 선택한 앱만 / 선택한 앱 제외 + 설치된 앱 검색·선택 */
@Composable
fun AppSelectionScreen(
    onBack: () -> Unit,
    viewModel: AppSelectionViewModel = viewModel(factory = AppSelectionViewModel.Factory),
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("앱별 기록 설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            item(key = "modes") {
                Column(Modifier.padding(vertical = 4.dp)) {
                    RecordMode.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == mode,
                                    onClick = { viewModel.setMode(option) },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == mode, onClick = null)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (mode == RecordMode.ALL) {
                item(key = "all_info") {
                    InfoCard("모든 앱의 알림을 기록합니다. 특정 앱만 기록하거나 제외하려면 위에서 모드를 바꾸세요.")
                }
                return@LazyColumn
            }

            item(key = "mode_info") {
                InfoCard(
                    when {
                        mode == RecordMode.ALLOW_LIST && selectedCount == 0 ->
                            "선택한 앱의 알림만 기록됩니다. 아직 선택한 앱이 없어 아무 알림도 기록되지 않습니다."
                        mode == RecordMode.ALLOW_LIST -> "선택한 앱의 알림만 기록됩니다. (${selectedCount}개 선택)"
                        else -> "선택한 앱의 알림은 기록되지 않습니다. (${selectedCount}개 제외)"
                    }
                )
            }

            item(key = "search") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = viewModel::setSearch,
                        singleLine = true,
                        placeholder = { Text("앱 이름 검색") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearch("") }) { Icon(Icons.Filled.Clear, "지우기") }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                    )
                    TextButton(onClick = viewModel::clearSelection, enabled = selectedCount > 0) {
                        Text("모두 해제")
                    }
                }
            }

            val list = apps
            if (list == null) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(list, key = { it.app.packageName }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggle(item) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(item.app.packageName, item.app.label, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                item.app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Checkbox(checked = item.selected, onCheckedChange = { viewModel.toggle(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

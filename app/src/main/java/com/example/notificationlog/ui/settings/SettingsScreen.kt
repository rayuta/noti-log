@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.BuildConfig
import com.example.notificationlog.domain.RetentionPeriod
import com.example.notificationlog.service.NotificationAccess
import com.example.notificationlog.ui.common.ExportStateDialogs
import com.example.notificationlog.ui.common.rememberXlsxExportLauncher

@Composable
fun SettingsScreen(
    accessGranted: Boolean,
    onOpenAppSelection: () -> Unit,
    onShowPrivacy: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val retention by viewModel.retention.collectAsStateWithLifecycle()
    val recordMode by viewModel.recordMode.collectAsStateWithLifecycle()
    val excludeOngoing by viewModel.excludeOngoing.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val launchExport = rememberXlsxExportLauncher { uri -> viewModel.exportAll(uri) }

    var showRetentionDialog by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("설정") }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("알림 권한")
            ListItem(
                headlineContent = { Text(if (accessGranted) "알림 접근 허용됨" else "알림 접근 권한이 필요합니다") },
                supportingContent = {
                    Text(
                        if (accessGranted) "새 알림이 기록되고 있습니다. 탭하면 시스템 설정에서 변경할 수 있습니다."
                        else "탭하여 시스템 설정에서 '알림 기록'의 알림 접근을 허용하세요."
                    )
                },
                leadingContent = {
                    Icon(
                        if (accessGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (accessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.clickable { NotificationAccess.openSettings(context) },
            )

            SectionHeader("기록 대상")
            ListItem(
                headlineContent = { Text("앱별 기록 설정") },
                supportingContent = { Text(recordMode.label + " · " + recordMode.description) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable(onClick = onOpenAppSelection),
            )
            ListItem(
                headlineContent = { Text("진행 중 알림 제외") },
                supportingContent = { Text("음악 재생, 다운로드 진행률처럼 계속 표시되는 알림은 기록하지 않습니다.") },
                trailingContent = {
                    Switch(checked = excludeOngoing, onCheckedChange = viewModel::setExcludeOngoing)
                },
            )

            SectionHeader("보관 기간")
            ListItem(
                headlineContent = { Text("알림 보관 기간: ${retention.label}") },
                supportingContent = {
                    Text("저장 기간이 지난 알림은 자동 삭제됩니다. (즐겨찾기한 알림은 보존)")
                },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { showRetentionDialog = true },
            )

            SectionHeader("데이터")
            ListItem(
                headlineContent = { Text("저장된 알림") },
                supportingContent = { Text("${totalCount}건 · 기기 내부에만 저장됨") },
            )
            ListItem(
                headlineContent = { Text("전체 알림 내보내기 (XLSX)") },
                supportingContent = { Text("저장 위치를 직접 선택합니다. 조건별 내보내기는 목록·검색 화면에서 할 수 있습니다.") },
                modifier = Modifier.clickable(onClick = launchExport),
            )
            ListItem(
                headlineContent = { Text("모든 알림 삭제", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("저장된 알림 기록을 모두 삭제합니다. 되돌릴 수 없습니다.") },
                modifier = Modifier.clickable { confirmDeleteAll = true },
            )

            SectionHeader("앱 정보")
            ListItem(
                headlineContent = { Text("알림 기록 (Notification Log)") },
                supportingContent = { Text("버전 ${BuildConfig.VERSION_NAME}") },
            )
            ListItem(
                headlineContent = { Text("개인정보 안내") },
                supportingContent = {
                    Text("이 앱은 기기에 표시된 알림만 저장합니다. 알림은 기기 내부에만 저장되며 서버로 전송되지 않습니다.")
                },
                modifier = Modifier.clickable(onClick = onShowPrivacy),
            )
        }
    }

    if (showRetentionDialog) {
        RetentionDialog(
            current = retention,
            onDismiss = { showRetentionDialog = false },
            onSelect = { viewModel.setRetention(it); showRetentionDialog = false },
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("모든 알림 삭제") },
            text = { Text("저장된 알림 ${totalCount}건(즐겨찾기 포함)을 모두 삭제할까요? 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAll(); confirmDeleteAll = false }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("취소") } },
        )
    }

    ExportStateDialogs(exportState, onDismiss = viewModel::dismissExport)
}

@Composable
private fun SectionHeader(title: String) {
    HorizontalDivider()
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun RetentionDialog(
    current: RetentionPeriod,
    onDismiss: () -> Unit,
    onSelect: (RetentionPeriod) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("보관 기간") },
        text = {
            Column {
                Text(
                    "저장 기간이 지난 알림은 자동 삭제됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                RetentionPeriod.entries.forEach { period ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = period == current,
                                onClick = { onSelect(period) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = period == current, onClick = null)
                        Text(
                            if (period.isForever) "${period.label} (자동 삭제 안 함)" else period.label,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

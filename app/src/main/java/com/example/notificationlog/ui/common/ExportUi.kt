package com.example.notificationlog.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.window.DialogProperties
import com.example.notificationlog.export.XlsxExporter
import com.example.notificationlog.ui.list.ExportState

/**
 * SAF(문서 생성) 런처. 사용자가 저장 위치와 파일명을 직접 선택한다.
 * 저장소 권한이 필요 없고, 앱은 사용자가 고른 파일 하나에만 접근한다.
 */
@Composable
fun rememberXlsxExportLauncher(onUriSelected: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XlsxExporter.MIME_TYPE)
    ) { uri -> if (uri != null) onUriSelected(uri) }
    return { launcher.launch(XlsxExporter.defaultFileName()) }
}

/** 내보내기 진행/완료/실패 대화상자 */
@Composable
fun ExportStateDialogs(state: ExportState, onDismiss: () -> Unit) {
    when (state) {
        ExportState.Idle -> Unit
        ExportState.Running -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            confirmButton = {},
            title = { Text("내보내는 중") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("XLSX 파일을 만들고 있습니다…")
                }
            },
        )
        is ExportState.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("내보내기 완료") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("알림 ${state.result.count}건을 저장했습니다.")
                    Text("저장 위치", style = MaterialTheme.typography.labelLarge)
                    Text(state.result.location, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
        )
        is ExportState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("내보내기 실패") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
        )
    }
}

/** 알림 접근 권한이 없을 때 홈/설정 화면 상단에 표시하는 안내 카드 */
@Composable
fun PermissionCard(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("알림 접근 권한이 필요합니다", style = MaterialTheme.typography.titleMedium)
            Text(
                "이 앱은 기기에 표시된 알림만 저장합니다. 설정에서 '알림 기록'의 알림 접근을 허용해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "설정 화면에서 허용이 비활성화되어 있다면: 설정 > 애플리케이션 > 알림 기록 > ⋮ 메뉴 > '제한된 설정 허용' 후 다시 시도하세요.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onOpenSettings) { Text("설정으로 이동") }
        }
    }
}

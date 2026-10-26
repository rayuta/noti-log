@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notificationlog.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notificationlog.domain.KeywordFilter
import com.example.notificationlog.domain.NotificationQuery
import com.example.notificationlog.domain.RecordedApp

/**
 * 목록 상단의 조건 칩 모음 (즐겨찾기 / 앱 / 기간 / 사용자 필터 / 키워드).
 * null 콜백으로 전달된 항목은 표시하지 않는다.
 */
@Composable
fun QueryChips(
    query: NotificationQuery,
    recordedApps: List<RecordedApp>,
    onFavoritesChange: (Boolean) -> Unit,
    onPackagesChange: (Set<String>) -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<KeywordFilter>? = null,
    onFilterChange: ((KeywordFilter?) -> Unit)? = null,
    onKeywordChange: ((String) -> Unit)? = null,
) {
    var showAppPicker by remember { mutableStateOf(false) }
    var showKeywordDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = query.favoritesOnly,
            onClick = { onFavoritesChange(!query.favoritesOnly) },
            label = { Text("즐겨찾기") },
        )

        FilterChip(
            selected = query.packages.isNotEmpty(),
            onClick = { showAppPicker = true },
            label = { Text(appChipLabel(query.packages, recordedApps)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
        )

        DateRangeChip(
            from = query.fromMillis,
            to = query.toMillis,
            onChange = onDateRangeChange,
        )

        if (filters != null && onFilterChange != null) {
            FilterSelectChip(
                selected = query.filter,
                filters = filters.filter { it.enabled },
                onChange = onFilterChange,
            )
        }

        if (onKeywordChange != null) {
            FilterChip(
                selected = query.keyword.isNotBlank(),
                onClick = { showKeywordDialog = true },
                label = {
                    Text(
                        if (query.keyword.isBlank()) "키워드" else "키워드: ${query.keyword}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
            )
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            title = "앱 선택",
            apps = recordedApps.map { it.packageName to "${it.appName} (${it.count})" },
            initialSelection = query.packages,
            onDismiss = { showAppPicker = false },
            onConfirm = { onPackagesChange(it); showAppPicker = false },
        )
    }

    if (showKeywordDialog && onKeywordChange != null) {
        KeywordDialog(
            initial = query.keyword,
            onDismiss = { showKeywordDialog = false },
            onConfirm = { onKeywordChange(it); showKeywordDialog = false },
        )
    }
}

private fun appChipLabel(packages: Set<String>, apps: List<RecordedApp>): String {
    if (packages.isEmpty()) return "앱: 전체"
    val first = apps.firstOrNull { it.packageName in packages }?.appName ?: packages.first()
    return if (packages.size == 1) "앱: $first" else "앱: $first 외 ${packages.size - 1}"
}

/** 기간 선택 칩: 빠른 선택(오늘/7일/30일) 또는 달력에서 직접 선택 */
@Composable
private fun DateRangeChip(from: Long?, to: Long?, onChange: (Long?, Long?) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val label = when {
        from == null && to == null -> "기간: 전체"
        else -> {
            val start = from?.let { Formatters.shortDate(it) } ?: "처음"
            // to 는 '미포함' 경계이므로 하루 전 날짜를 표시한다.
            val end = to?.let { Formatters.shortDate(it - 1) } ?: "현재"
            "$start ~ $end"
        }
    }
    Box {
        FilterChip(
            selected = from != null || to != null,
            onClick = { menuOpen = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            val today = Formatters.today()
            val tomorrowStart = Formatters.startOfDay(today.plusDays(1))
            DropdownMenuItem(text = { Text("전체 기간") }, onClick = { menuOpen = false; onChange(null, null) })
            DropdownMenuItem(text = { Text("오늘") }, onClick = {
                menuOpen = false; onChange(Formatters.startOfDay(today), tomorrowStart)
            })
            DropdownMenuItem(text = { Text("최근 7일") }, onClick = {
                menuOpen = false; onChange(Formatters.startOfDay(today.minusDays(6)), tomorrowStart)
            })
            DropdownMenuItem(text = { Text("최근 30일") }, onClick = {
                menuOpen = false; onChange(Formatters.startOfDay(today.minusDays(29)), tomorrowStart)
            })
            DropdownMenuItem(text = { Text("직접 선택…") }, onClick = { menuOpen = false; showPicker = true })
        }
    }
    if (showPicker) {
        DateRangePickerDialog(
            onDismiss = { showPicker = false },
            onConfirm = { start, end -> onChange(start, end); showPicker = false },
        )
    }
}

@Composable
private fun DateRangePickerDialog(onDismiss: () -> Unit, onConfirm: (Long, Long) -> Unit) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val startUtc = state.selectedStartDateMillis ?: return@TextButton
                    val endUtc = state.selectedEndDateMillis ?: startUtc
                    // DatePicker 는 UTC 자정 기준 값을 주므로 로컬 날짜의 시작/다음날 시작으로 변환
                    val start = Formatters.startOfDay(Formatters.utcMillisToLocalDate(startUtc))
                    val end = Formatters.startOfDay(Formatters.utcMillisToLocalDate(endUtc).plusDays(1))
                    onConfirm(start, end)
                },
            ) { Text("적용") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f),
            title = { Text("기간 선택", Modifier.padding(start = 24.dp, top = 16.dp)) },
        )
    }
}

@Composable
private fun FilterSelectChip(
    selected: KeywordFilter?,
    filters: List<KeywordFilter>,
    onChange: (KeywordFilter?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { menuOpen = true },
            label = { Text(selected?.let { "필터: ${it.name}" } ?: "필터") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("필터 없음") }, onClick = { menuOpen = false; onChange(null) })
            filters.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.name) },
                    onClick = { menuOpen = false; onChange(filter) },
                )
            }
            if (filters.isEmpty()) {
                DropdownMenuItem(text = { Text("활성화된 필터가 없습니다") }, enabled = false, onClick = {})
            }
        }
    }
}

@Composable
private fun KeywordDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("키워드로 거르기") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("제목 또는 본문에 포함된 단어") },
                trailingIcon = {
                    if (text.isNotEmpty()) IconButton(onClick = { text = "" }) { Icon(Icons.Filled.Clear, "지우기") }
                },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text("적용") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

/**
 * 여러 앱을 선택하는 대화상자 (검색 가능).
 * @param apps (패키지명, 표시 이름) 목록
 */
@Composable
fun AppPickerDialog(
    title: String,
    apps: List<Pair<String, String>>,
    initialSelection: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    var search by remember { mutableStateOf("") }
    val visible = remember(apps, search) {
        if (search.isBlank()) apps
        else apps.filter { (pkg, label) -> label.contains(search, true) || pkg.contains(search, true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("앱 검색") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(4.dp))
                if (apps.isEmpty()) {
                    Text("아직 기록된 앱이 없습니다.", style = MaterialTheme.typography.bodyMedium)
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(visible, key = { it.first }) { (pkg, label) ->
                        val checked = pkg in selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selection = if (checked) selection - pkg else selection + pkg }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            AppIcon(pkg, label, size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selection) }) { Text("적용") } },
        dismissButton = {
            Row {
                TextButton(onClick = { selection = emptySet() }) { Text("전체 해제") }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}

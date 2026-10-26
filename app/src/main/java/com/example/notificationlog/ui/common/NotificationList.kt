package com.example.notificationlog.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.notificationlog.domain.NotificationItem

/** 알림 목록 한 줄. 탭하면 상세, 길게 누르면 즐겨찾기/삭제 메뉴. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationRow(
    item: NotificationItem,
    highlightTerms: List<String>,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AppIcon(item.packageName, item.appName, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "즐겨찾기",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = Formatters.listTime(item.postedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.title.isNotBlank()) {
                    Text(
                        text = highlight(item.title, highlightTerms, highlightColor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (item.text.isNotBlank()) highlight(item.text, highlightTerms, highlightColor)
                    else highlight(EMPTY_CONTENT, emptyList(), highlightColor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (item.isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가") },
                leadingIcon = { Icon(if (item.isFavorite) Icons.Outlined.Star else Icons.Filled.Star, null) },
                onClick = { menuOpen = false; onToggleFavorite() },
            )
            DropdownMenuItem(
                text = { Text("삭제") },
                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}

/** 내용을 추출할 수 없는 알림(민감한 내용 숨김, 커스텀 뷰 등)에 표시할 문구 */
const val EMPTY_CONTENT = "(표시할 내용이 없는 알림)"

/**
 * Paging 기반 무한 스크롤 알림 목록.
 * header 에는 권한 안내 카드나 필터 칩 등을 넣는다.
 */
@Composable
fun PagedNotificationList(
    pagingItems: LazyPagingItems<NotificationItem>,
    highlightTerms: List<String>,
    emptyMessage: String,
    onClick: (NotificationItem) -> Unit,
    onToggleFavorite: (NotificationItem) -> Unit,
    onDelete: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
    header: LazyListScope.() -> Unit = {},
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        header()

        val refresh = pagingItems.loadState.refresh
        when {
            refresh is LoadState.Loading && pagingItems.itemCount == 0 -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            refresh is LoadState.Error -> item(key = "error") {
                CenterMessage("목록을 불러오지 못했습니다: ${refresh.error.message.orEmpty()}")
            }
            refresh is LoadState.NotLoading && pagingItems.itemCount == 0 -> item(key = "empty") {
                CenterMessage(emptyMessage)
            }
        }

        items(count = pagingItems.itemCount, key = pagingItems.itemKey { it.id }) { index ->
            val item = pagingItems[index] ?: return@items
            NotificationRow(
                item = item,
                highlightTerms = highlightTerms,
                onClick = { onClick(item) },
                onToggleFavorite = { onToggleFavorite(item) },
                onDelete = { onDelete(item) },
            )
            HorizontalDivider(Modifier.padding(start = 68.dp))
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item(key = "append") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun CenterMessage(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

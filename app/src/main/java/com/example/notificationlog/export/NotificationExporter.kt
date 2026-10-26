package com.example.notificationlog.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.notificationlog.data.repository.NotificationRepository
import com.example.notificationlog.domain.NotificationCategories
import com.example.notificationlog.domain.NotificationItem
import com.example.notificationlog.domain.NotificationQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExportResult(val count: Int, val location: String)

/**
 * 현재 조회 조건의 알림을 사용자가 SAF 로 선택한 위치(Uri)에 XLSX 로 저장한다.
 * 모든 작업은 IO 디스패처에서 수행되어 UI 가 멈추지 않는다.
 */
class NotificationExporter(
    private val context: Context,
    private val repository: NotificationRepository,
) {
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun export(uri: Uri, query: NotificationQuery): ExportResult = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("파일을 열 수 없습니다")
        val count = output.use { stream ->
            XlsxExporter.newWriter(stream).use { writer ->
                repository.forEachChunk(query) { chunk ->
                    currentCoroutineContext().ensureActive()
                    chunk.forEach { writer.writeRow(toCells(it)) }
                }
            }
        }
        ExportResult(count, describeLocation(uri))
    }

    private fun toCells(item: NotificationItem): List<String> {
        val zone = ZoneId.systemDefault()
        return listOf(
            timeFormat.format(Instant.ofEpochMilli(item.postedAt).atZone(zone)),
            item.appName,
            item.packageName,
            item.title,
            item.text,
            NotificationCategories.label(item.category, item.channelId),
            if (item.isFavorite) "Y" else "N",
        )
    }

    /** 사용자에게 보여줄 저장 위치 (예: "내장 저장공간/Download/notification_log_….xlsx") */
    private fun describeLocation(uri: Uri): String {
        val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        if (docId != null) {
            when {
                docId.startsWith("primary:") -> return "내장 저장공간/" + docId.removePrefix("primary:")
                docId.startsWith("raw:") -> return docId.removePrefix("raw:")
            }
        }
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        val provider = uri.authority?.let { authority ->
            when {
                authority.contains("downloads") -> "다운로드"
                authority.contains("google.android.apps.docs") -> "Google Drive"
                else -> null
            }
        }
        return listOfNotNull(provider, name).joinToString("/").ifEmpty { uri.toString() }
    }
}

package com.example.notificationlog.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.notificationlog.data.local.NotificationDao
import com.example.notificationlog.data.local.NotificationEntity
import com.example.notificationlog.domain.CapturedNotification
import com.example.notificationlog.domain.NotificationItem
import com.example.notificationlog.domain.NotificationQuery
import com.example.notificationlog.domain.RecordedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class NotificationRepository(
    private val dao: NotificationDao,
    private val settings: SettingsRepository,
    private val appRules: AppRuleRepository,
) {
    /** 마지막으로 삭제한 알림 (실행 취소용) */
    @Volatile
    private var lastDeleted: NotificationEntity? = null

    fun pagedItems(query: NotificationQuery): Flow<PagingData<NotificationItem>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
            dao.pagingSource(NotificationQuerySql.select(query))
        }.flow.map { paging -> paging.map { it.toDomain() } }

    fun observeCount(query: NotificationQuery): Flow<Int> = dao.observeCount(NotificationQuerySql.count(query))

    fun observeItem(id: Long): Flow<NotificationItem?> = dao.observeById(id).map { it?.toDomain() }

    fun observeRecordedApps(): Flow<List<RecordedApp>> =
        dao.observeRecordedApps().map { rows -> rows.map { RecordedApp(it.packageName, it.appName, it.count) } }

    suspend fun recordedApps(): List<RecordedApp> = observeRecordedApps().first()

    /**
     * 수신한 알림을 저장한다.
     * 1) 진행 중 알림 제외 설정, 2) 앱별 기록 규칙, 3) 짧은 시간 내 동일 내용 재게시, 4) 해시 중복
     * 순서로 검사한 뒤 저장하며, 실제로 저장되면 true 를 반환한다.
     */
    suspend fun record(captured: CapturedNotification): Boolean {
        if (captured.isOngoing && settings.excludeOngoingNow()) return false
        if (!appRules.shouldRecord(captured.packageName)) return false

        val since = captured.postedAt - DUPLICATE_WINDOW_MS
        if (dao.countRecentSame(captured.packageName, captured.title, captured.text, since) > 0) return false

        val entity = NotificationEntity(
            postedAt = captured.postedAt,
            packageName = captured.packageName,
            appName = captured.appName,
            title = captured.title,
            text = captured.text,
            category = captured.category,
            channelId = captured.channelId,
            hash = hashOf(captured),
        )
        // hash 고유 인덱스 충돌 시 IGNORE → -1
        return dao.insert(entity) != -1L
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun delete(id: Long) {
        lastDeleted = dao.getById(id)
        dao.deleteById(id)
    }

    suspend fun undoLastDelete() {
        lastDeleted?.let { dao.restore(it) }
        lastDeleted = null
    }

    suspend fun deleteAll() = dao.deleteAll()

    /** 보관 기간이 지난 알림을 삭제한다. 무기한이면 아무것도 하지 않는다. */
    suspend fun cleanupExpired(now: Long = System.currentTimeMillis()): Int {
        val retention = settings.retentionNow()
        if (retention.isForever) return 0
        val cutoff = now - TimeUnit.DAYS.toMillis(retention.days.toLong())
        return dao.deleteOlderThan(cutoff)
    }

    /**
     * 조건에 맞는 알림을 최신순으로 청크 단위로 전달한다 (대량 내보내기 시 메모리 사용 최소화).
     * @return 전달한 총 건수
     */
    suspend fun forEachChunk(
        query: NotificationQuery,
        chunkSize: Int = EXPORT_CHUNK_SIZE,
        block: suspend (List<NotificationItem>) -> Unit,
    ): Int {
        val maxId = dao.maxId()
        var offset = 0
        while (true) {
            val chunk = dao.queryList(NotificationQuerySql.selectChunk(query, maxId, chunkSize, offset))
            if (chunk.isEmpty()) break
            block(chunk.map { it.toDomain() })
            offset += chunk.size
            if (chunk.size < chunkSize) break
        }
        return offset
    }

    private fun NotificationEntity.toDomain() = NotificationItem(
        id = id,
        postedAt = postedAt,
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        category = category,
        channelId = channelId,
        isFavorite = isFavorite,
    )

    companion object {
        private const val PAGE_SIZE = 50
        private const val EXPORT_CHUNK_SIZE = 500

        /** 같은 앱·제목·본문이 이 시간 안에 다시 들어오면 재게시로 보고 저장하지 않는다. */
        private const val DUPLICATE_WINDOW_MS = 2_000L

        /**
         * 중복 판별 해시: 패키지명 + 제목 + 본문 + 알림 시각(초 단위).
         * 알림 시각은 Notification.when (없으면 수신 시각)을 사용해, 같은 알림이 업데이트되어
         * 다시 게시되어도 동일한 해시가 나오도록 한다.
         */
        fun hashOf(c: CapturedNotification): String {
            val time = (if (c.whenTime > 0) c.whenTime else c.postedAt) / 1000
            val source = listOf(c.packageName, c.title, c.text, time.toString()).joinToString("\u0000")
            val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

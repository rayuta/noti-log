package com.example.notificationlog.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    /** 해시가 같은 알림은 무시한다. 무시된 경우 -1 을 반환한다. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationEntity): Long

    /** 삭제 취소 시 원래 id 그대로 복원한다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restore(entity: NotificationEntity)

    /** 동적 조건(앱/기간/키워드/필터) 목록 - Paging 으로 무한 스크롤 */
    @RawQuery(observedEntities = [NotificationEntity::class])
    fun pagingSource(query: SupportSQLiteQuery): PagingSource<Int, NotificationEntity>

    /** 내보내기용 청크 조회 */
    @RawQuery
    suspend fun queryList(query: SupportSQLiteQuery): List<NotificationEntity>

    @RawQuery(observedEntities = [NotificationEntity::class])
    fun observeCount(query: SupportSQLiteQuery): Flow<Int>

    @Query("SELECT * FROM notifications WHERE id = :id")
    fun observeById(id: Long): Flow<NotificationEntity?>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getById(id: Long): NotificationEntity?

    @Query("SELECT IFNULL(MAX(id), 0) FROM notifications")
    suspend fun maxId(): Long

    /** 짧은 시간 내 동일 내용이 다시 게시된 경우를 찾는다 ((packageName, postedAt) 인덱스 사용). */
    @Query(
        "SELECT COUNT(*) FROM notifications WHERE packageName = :packageName " +
            "AND postedAt >= :since AND title = :title AND text = :text"
    )
    suspend fun countRecentSame(packageName: String, title: String, text: String, since: Long): Int

    @Query("UPDATE notifications SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 보관 기간 만료 삭제. 즐겨찾기한 알림은 보존한다. */
    @Query("DELETE FROM notifications WHERE postedAt < :cutoff AND isFavorite = 0")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query(
        "SELECT packageName, MAX(appName) AS appName, COUNT(*) AS cnt FROM notifications " +
            "GROUP BY packageName ORDER BY appName COLLATE NOCASE"
    )
    fun observeRecordedApps(): Flow<List<AppCountRow>>
}

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules WHERE listType = :listType")
    fun observeByType(listType: String): Flow<List<AppRuleEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM app_rules WHERE packageName = :packageName AND listType = :listType)")
    suspend fun exists(packageName: String, listType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AppRuleEntity)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName AND listType = :listType")
    suspend fun delete(packageName: String, listType: String)

    @Query("DELETE FROM app_rules WHERE listType = :listType")
    suspend fun clear(listType: String)
}

@Dao
interface KeywordFilterDao {
    @Query("SELECT * FROM keyword_filters ORDER BY createdAt, id")
    fun observeAll(): Flow<List<KeywordFilterEntity>>

    @Query("SELECT * FROM keyword_filters WHERE id = :id")
    fun observeById(id: Long): Flow<KeywordFilterEntity?>

    @Insert
    suspend fun insert(filter: KeywordFilterEntity): Long

    /** 수정 시 createdAt(정렬 순서)은 유지한다. */
    @Query(
        "UPDATE keyword_filters SET name = :name, keywords = :keywords, " +
            "targetPackages = :targetPackages, enabled = :enabled WHERE id = :id"
    )
    suspend fun update(id: Long, name: String, keywords: String, targetPackages: String, enabled: Boolean)

    @Query("UPDATE keyword_filters SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM keyword_filters WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE name = :name")
    fun observe(name: String): Flow<String?>

    @Query("SELECT value FROM settings WHERE name = :name")
    suspend fun get(name: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: SettingEntity)
}

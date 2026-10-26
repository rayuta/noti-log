package com.example.notificationlog.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.notificationlog.domain.NotificationQuery

/**
 * NotificationQuery → SQL 변환기.
 *
 * 한국어는 단어 중간 일치(예: "롯데택배" 에서 "택배")가 중요하지만 SQLite FTS 기본 토크나이저는
 * 이를 지원하지 않으므로 LIKE '%키워드%' 를 사용한다. 대신 앱/기간/즐겨찾기 조건과 최신순 정렬은
 * 인덱스(postedAt, packageName+postedAt, isFavorite+postedAt)를 타도록 구성해 탐색 범위를 줄인다.
 */
internal object NotificationQuerySql {

    private const val LIKE_CLAUSE = "(title LIKE ? ESCAPE '\\' OR text LIKE ? ESCAPE '\\')"
    private const val ORDER = " ORDER BY postedAt DESC, id DESC"

    fun select(query: NotificationQuery): SimpleSQLiteQuery {
        val (where, args) = where(query)
        return SimpleSQLiteQuery("SELECT * FROM notifications$where$ORDER", args.toTypedArray())
    }

    /** 내보내기용: maxId 이하만 조회해 내보내는 도중 새로 들어온 알림 때문에 페이지가 밀리지 않게 한다. */
    fun selectChunk(query: NotificationQuery, maxId: Long, limit: Int, offset: Int): SimpleSQLiteQuery {
        val (where, args) = where(query, maxId)
        args += limit
        args += offset
        return SimpleSQLiteQuery(
            "SELECT * FROM notifications$where$ORDER LIMIT ? OFFSET ?",
            args.toTypedArray(),
        )
    }

    fun count(query: NotificationQuery): SimpleSQLiteQuery {
        val (where, args) = where(query)
        return SimpleSQLiteQuery("SELECT COUNT(*) FROM notifications$where", args.toTypedArray())
    }

    private fun where(query: NotificationQuery, maxId: Long? = null): Pair<String, MutableList<Any>> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (query.packages.isNotEmpty()) {
            clauses += "packageName IN (${query.packages.joinToString(",") { "?" }})"
            args.addAll(query.packages)
        }
        query.fromMillis?.let { clauses += "postedAt >= ?"; args += it }
        query.toMillis?.let { clauses += "postedAt < ?"; args += it }
        if (query.favoritesOnly) clauses += "isFavorite = 1"
        maxId?.let { clauses += "id <= ?"; args += it }

        // 검색어: 모든 단어가 제목 또는 본문에 포함되어야 한다 (AND)
        query.searchTerms.forEach { term ->
            clauses += LIKE_CLAUSE
            val pattern = likePattern(term)
            args += pattern
            args += pattern
        }

        // 사용자 필터: 키워드 중 하나라도 포함되면 일치 (OR) + 대상 앱 제한
        query.filter?.let { filter ->
            val keywords = filter.keywords.filter { it.isNotBlank() }
            if (keywords.isNotEmpty()) {
                clauses += keywords.joinToString(" OR ", prefix = "(", postfix = ")") { LIKE_CLAUSE }
                keywords.forEach {
                    val pattern = likePattern(it)
                    args += pattern
                    args += pattern
                }
            }
            if (filter.targetPackages.isNotEmpty()) {
                clauses += "packageName IN (${filter.targetPackages.joinToString(",") { "?" }})"
                args.addAll(filter.targetPackages)
            }
        }

        val where = if (clauses.isEmpty()) "" else " WHERE " + clauses.joinToString(" AND ")
        return where to args
    }

    /** LIKE 특수문자(%, _, \)를 이스케이프한다. */
    private fun likePattern(term: String): String {
        val escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }
}

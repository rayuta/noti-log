package com.example.notificationlog.data.repository

import com.example.notificationlog.data.local.KeywordFilterDao
import com.example.notificationlog.data.local.KeywordFilterEntity
import com.example.notificationlog.domain.KeywordFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 사용자 키워드 필터 관리 */
class FilterRepository(private val dao: KeywordFilterDao) {

    fun observeAll(): Flow<List<KeywordFilter>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observe(id: Long): Flow<KeywordFilter?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun save(filter: KeywordFilter) {
        val name = filter.name.trim()
        val keywords = filter.keywords.joinToString(SEPARATOR)
        val targets = filter.targetPackages.joinToString(SEPARATOR)
        if (filter.id == 0L) {
            dao.insert(
                KeywordFilterEntity(
                    name = name,
                    keywords = keywords,
                    targetPackages = targets,
                    enabled = filter.enabled,
                    createdAt = System.currentTimeMillis(),
                )
            )
        } else {
            dao.update(filter.id, name, keywords, targets, filter.enabled)
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.delete(id)

    private fun KeywordFilterEntity.toDomain(): KeywordFilter {
        return KeywordFilter(
            id = id,
            name = name,
            keywords = parseKeywords(keywords),
            targetPackages = targetPackages.split(SEPARATOR).filter { it.isNotBlank() }.toSet(),
            enabled = enabled,
        )
    }

    companion object {
        private const val SEPARATOR = "\n"

        /** "배송, 도착" 처럼 쉼표 또는 줄바꿈으로 구분된 키워드를 분리한다. */
        fun parseKeywords(raw: String): List<String> =
            raw.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }
}

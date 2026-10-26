package com.example.notificationlog.data.repository

import com.example.notificationlog.data.local.AppRuleDao
import com.example.notificationlog.data.local.AppRuleEntity
import com.example.notificationlog.domain.AppInfo
import com.example.notificationlog.domain.RecordMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** 앱별 기록 대상(허용/제외 목록) 관리 */
class AppRuleRepository(
    private val dao: AppRuleDao,
    private val settings: SettingsRepository,
) {
    /** 해당 모드에서 선택된 패키지 목록 */
    fun observeSelected(mode: RecordMode): Flow<Set<String>> {
        val type = listType(mode) ?: return flowOf(emptySet())
        return dao.observeByType(type).map { rules -> rules.map { it.packageName }.toSet() }
    }

    suspend fun setSelected(mode: RecordMode, app: AppInfo, selected: Boolean) {
        val type = listType(mode) ?: return
        if (selected) dao.insert(AppRuleEntity(app.packageName, type, app.label))
        else dao.delete(app.packageName, type)
    }

    suspend fun clear(mode: RecordMode) {
        listType(mode)?.let { dao.clear(it) }
    }

    /** 알림 수신 시 현재 모드와 목록에 따라 기록 여부를 결정한다. */
    suspend fun shouldRecord(packageName: String): Boolean = when (settings.recordModeNow()) {
        RecordMode.ALL -> true
        RecordMode.ALLOW_LIST -> dao.exists(packageName, TYPE_ALLOW)
        RecordMode.BLOCK_LIST -> !dao.exists(packageName, TYPE_BLOCK)
    }

    private fun listType(mode: RecordMode): String? = when (mode) {
        RecordMode.ALL -> null
        RecordMode.ALLOW_LIST -> TYPE_ALLOW
        RecordMode.BLOCK_LIST -> TYPE_BLOCK
    }

    private companion object {
        const val TYPE_ALLOW = "ALLOW"
        const val TYPE_BLOCK = "BLOCK"
    }
}

package com.example.notificationlog.data.repository

import com.example.notificationlog.data.local.SettingDao
import com.example.notificationlog.data.local.SettingEntity
import com.example.notificationlog.domain.RecordMode
import com.example.notificationlog.domain.RetentionPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room 의 settings 테이블에 저장되는 앱 설정. 재부팅/재설치 전까지 유지된다. */
class SettingsRepository(private val dao: SettingDao) {

    val retention: Flow<RetentionPeriod> =
        dao.observe(KEY_RETENTION).map { RetentionPeriod.fromDays(it?.toIntOrNull()) }

    val recordMode: Flow<RecordMode> = dao.observe(KEY_RECORD_MODE).map { RecordMode.fromName(it) }

    /** 음악 재생·다운로드 진행률 등 "진행 중(고정)" 알림 제외 여부. 기본값: 제외 */
    val excludeOngoing: Flow<Boolean> = dao.observe(KEY_EXCLUDE_ONGOING).map { it?.toBooleanStrictOrNull() ?: true }

    /** 첫 실행 개인정보 안내를 확인했는지 여부 */
    val onboardingDone: Flow<Boolean> = dao.observe(KEY_ONBOARDING_DONE).map { it == "true" }

    suspend fun retentionNow(): RetentionPeriod = RetentionPeriod.fromDays(dao.get(KEY_RETENTION)?.toIntOrNull())
    suspend fun recordModeNow(): RecordMode = RecordMode.fromName(dao.get(KEY_RECORD_MODE))
    suspend fun excludeOngoingNow(): Boolean = dao.get(KEY_EXCLUDE_ONGOING)?.toBooleanStrictOrNull() ?: true

    suspend fun setRetention(period: RetentionPeriod) = put(KEY_RETENTION, period.days.toString())
    suspend fun setRecordMode(mode: RecordMode) = put(KEY_RECORD_MODE, mode.name)
    suspend fun setExcludeOngoing(exclude: Boolean) = put(KEY_EXCLUDE_ONGOING, exclude.toString())
    suspend fun setOnboardingDone() = put(KEY_ONBOARDING_DONE, "true")

    private suspend fun put(key: String, value: String) = dao.put(SettingEntity(key, value))

    private companion object {
        const val KEY_RETENTION = "retention_days"
        const val KEY_RECORD_MODE = "record_mode"
        const val KEY_EXCLUDE_ONGOING = "exclude_ongoing"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}

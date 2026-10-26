package com.example.notificationlog.domain

/** 저장된 알림 한 건 (UI/내보내기에서 사용하는 도메인 모델). */
data class NotificationItem(
    val id: Long,
    val postedAt: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val category: String?,
    val channelId: String?,
    val isFavorite: Boolean,
)

/** NotificationListenerService 에서 추출한 원본 알림 정보 (저장 전 단계). */
data class CapturedNotification(
    val postedAt: Long,
    /** Notification.when 값. 같은 알림이 다시 게시될 때도 유지되므로 중복 판별에 사용한다. */
    val whenTime: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val category: String?,
    val channelId: String?,
    val isOngoing: Boolean,
)

/** 사용자 키워드 필터. keywords 중 하나라도 제목/본문에 포함되면 일치한다. */
data class KeywordFilter(
    val id: Long = 0,
    val name: String,
    val keywords: List<String>,
    /** 비어 있으면 모든 앱 대상 */
    val targetPackages: Set<String> = emptySet(),
    val enabled: Boolean = true,
)

/** 설치된 앱 정보 */
data class AppInfo(val packageName: String, val label: String)

/** 알림이 저장된 적이 있는 앱과 저장 건수 */
data class RecordedApp(val packageName: String, val appName: String, val count: Int)

/** 앱별 기록 대상 모드 */
enum class RecordMode(val label: String, val description: String) {
    ALL("모든 앱 기록", "모든 앱의 알림을 기록합니다"),
    ALLOW_LIST("선택한 앱만 기록", "선택한 앱의 알림만 기록됩니다"),
    BLOCK_LIST("선택한 앱 제외", "선택한 앱의 알림은 기록되지 않습니다");

    companion object {
        fun fromName(name: String?): RecordMode = entries.firstOrNull { it.name == name } ?: ALL
    }
}

/** 보관 기간. days < 0 이면 무기한 */
enum class RetentionPeriod(val days: Int, val label: String) {
    DAYS_7(7, "7일"),
    DAYS_30(30, "30일"),
    DAYS_90(90, "90일"),
    MONTHS_6(180, "6개월"),
    YEAR_1(365, "1년"),
    FOREVER(-1, "무기한");

    val isForever: Boolean get() = days < 0

    companion object {
        val DEFAULT = DAYS_30
        fun fromDays(days: Int?): RetentionPeriod = entries.firstOrNull { it.days == days } ?: DEFAULT
    }
}

/**
 * 목록/검색/내보내기에서 공통으로 사용하는 조회 조건.
 * 화면에 보이는 조건 그대로 XLSX 로 내보낼 수 있도록 하나의 모델로 관리한다.
 */
data class NotificationQuery(
    val keyword: String = "",
    val packages: Set<String> = emptySet(),
    /** 포함 (>=) */
    val fromMillis: Long? = null,
    /** 미포함 (<) */
    val toMillis: Long? = null,
    val favoritesOnly: Boolean = false,
    val filter: KeywordFilter? = null,
) {
    /** 공백으로 구분된 검색어. 모든 단어가 포함되어야 한다 (AND). */
    val searchTerms: List<String>
        get() = keyword.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    /** 화면에서 강조 표시할 단어 목록 */
    val highlightTerms: List<String>
        get() = (searchTerms + filter?.keywords.orEmpty()).filter { it.isNotBlank() }.distinct()
}

/** Notification.category 값을 한국어로 표시한다. */
object NotificationCategories {
    private val labels = mapOf(
        "msg" to "메시지",
        "email" to "이메일",
        "call" to "전화",
        "event" to "일정",
        "promo" to "프로모션",
        "alarm" to "알람",
        "progress" to "진행 상태",
        "social" to "소셜",
        "err" to "오류",
        "transport" to "미디어",
        "sys" to "시스템",
        "service" to "서비스",
        "reminder" to "리마인더",
        "recommendation" to "추천",
        "status" to "상태",
        "navigation" to "내비게이션",
        "location_sharing" to "위치 공유",
        "stopwatch" to "스톱워치",
        "workout" to "운동",
        "missed_call" to "부재중 전화",
        "voicemail" to "음성 메시지",
    )

    fun label(category: String?, channelId: String?): String {
        val cat = category?.let { labels[it] ?: it }
        return when {
            cat != null && !channelId.isNullOrBlank() -> "$cat (채널: $channelId)"
            cat != null -> cat
            !channelId.isNullOrBlank() -> "채널: $channelId"
            else -> ""
        }
    }
}

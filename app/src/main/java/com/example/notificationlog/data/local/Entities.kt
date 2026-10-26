package com.example.notificationlog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 저장된 알림.
 * - hash: 중복 저장 방지용 고유 인덱스
 * - postedAt / (packageName, postedAt) / (isFavorite, postedAt): 최신순 정렬과 앱·기간·즐겨찾기 조건 조회를 빠르게 하기 위한 인덱스
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["hash"], unique = true),
        Index(value = ["postedAt"]),
        Index(value = ["packageName", "postedAt"]),
        Index(value = ["isFavorite", "postedAt"]),
    ],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postedAt: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val category: String?,
    val channelId: String?,
    val isFavorite: Boolean = false,
    val hash: String,
)

/**
 * 앱별 기록 규칙. 허용 목록(ALLOW)과 제외 목록(BLOCK)을 별도로 보관해
 * 사용자가 모드를 바꿔도 각각의 선택이 유지되도록 한다.
 */
@Entity(tableName = "app_rules", primaryKeys = ["packageName", "listType"])
data class AppRuleEntity(
    val packageName: String,
    val listType: String,
    val appName: String,
)

/** 사용자 키워드 필터. 키워드/대상 앱은 줄바꿈으로 구분해 저장한다. */
@Entity(tableName = "keyword_filters")
data class KeywordFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val keywords: String,
    val targetPackages: String,
    val enabled: Boolean,
    val createdAt: Long,
)

/** 단순 키-값 설정 (보관 기간, 기록 모드 등) */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val name: String,
    val value: String,
)

/** 앱별 저장 건수 집계 결과 */
data class AppCountRow(
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "appName") val appName: String,
    @ColumnInfo(name = "cnt") val count: Int,
)

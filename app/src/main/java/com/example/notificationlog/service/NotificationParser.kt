package com.example.notificationlog.service

import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.BundleCompat
import com.example.notificationlog.domain.CapturedNotification
import java.io.File
import java.io.FileOutputStream

/**
 * StatusBarNotification 에서 저장할 정보를 추출한다.
 *
 * 앱마다 알림 구성 방식이 달라 제목/본문이 비어 있거나(민감 정보 숨김, 커스텀 뷰 등)
 * 다른 extra 에 들어 있는 경우가 많으므로 여러 후보를 순서대로 확인한다.
 * 아무것도 추출하지 못해도 앱 이름과 수신 시각은 항상 기록된다.
 */
object NotificationParser {

    /** 비정상적으로 긴 본문으로 DB 가 커지는 것을 막기 위한 상한 */
    private const val MAX_TEXT_LENGTH = 10_000

    /** Notification.EXTRA_BUILDER_APPLICATION_INFO (숨김 상수) - 알림을 보낸 앱의 ApplicationInfo */
    private const val EXTRA_APP_INFO = "android.appInfo"

    fun parse(context: Context, sbn: StatusBarNotification): CapturedNotification {
        val notification = sbn.notification
        val extras: Bundle = notification.extras ?: Bundle.EMPTY

        val title = firstNotBlank(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG),
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
        )

        // 본문: 펼친 텍스트(BigText) → 기본 텍스트 → 받은편지함 스타일 줄 → 메시지 스타일 → 부가 텍스트 → 티커
        val text = firstNotBlank(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.filterNotNull()?.joinToString("\n"),
            lastMessageText(notification),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
            notification.tickerText,
        )

        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0

        return CapturedNotification(
            postedAt = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
            whenTime = notification.`when`,
            packageName = sbn.packageName,
            appName = resolveAppName(context, sbn.packageName, extras),
            title = title.take(MAX_TEXT_LENGTH),
            text = text.take(MAX_TEXT_LENGTH),
            category = notification.category,
            channelId = notification.channelId,
            isOngoing = isOngoing,
        )
    }

    private fun firstNotBlank(vararg candidates: CharSequence?): String =
        candidates.firstOrNull { !it.isNullOrBlank() }?.toString()?.trim().orEmpty()

    private fun lastMessageText(notification: Notification): CharSequence? = try {
        NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)
            ?.messages?.lastOrNull()?.text
    } catch (_: Exception) {
        null
    }

    private fun appInfoFromExtras(extras: Bundle): ApplicationInfo? = try {
        BundleCompat.getParcelable(extras, EXTRA_APP_INFO, ApplicationInfo::class.java)
    } catch (_: Exception) {
        null
    }

    /** 앱 이름: PackageManager → 알림 extras 의 ApplicationInfo → 패키지명 순으로 결정 */
    private fun resolveAppName(context: Context, packageName: String, extras: Bundle): String {
        val pm = context.packageManager
        try {
            @Suppress("DEPRECATION")
            val info = pm.getApplicationInfo(packageName, 0)
            return pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            // 패키지 공개 범위 제한으로 조회되지 않는 앱
        }
        appInfoFromExtras(extras)?.let { info ->
            try {
                return pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
            }
        }
        return packageName
    }

    /**
     * PackageManager 로 아이콘을 조회할 수 없는 앱(런처에 없는 시스템 앱 등)을 위해
     * 알림에 포함된 ApplicationInfo 로 아이콘을 한 번만 앱 내부 저장소(filesDir)에 캐시한다.
     */
    fun cacheIconIfNeeded(context: Context, sbn: StatusBarNotification) {
        val file = AppIconFiles.fileFor(context, sbn.packageName)
        if (file.exists()) return
        val pm = context.packageManager
        val visible = try {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(sbn.packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
        if (visible) return
        val info = appInfoFromExtras(sbn.notification.extras ?: return) ?: return
        try {
            val bitmap = pm.getApplicationIcon(info).toBitmap(AppIconFiles.SIZE_PX, AppIconFiles.SIZE_PX)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (_: Exception) {
        }
    }
}

/** 캐시된 앱 아이콘 파일 위치 (앱 내부 저장소, 외부 공개되지 않음) */
object AppIconFiles {
    const val SIZE_PX = 128
    fun fileFor(context: Context, packageName: String): File =
        File(File(context.filesDir, "app_icons"), "$packageName.png")
}

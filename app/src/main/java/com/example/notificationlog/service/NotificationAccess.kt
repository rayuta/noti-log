package com.example.notificationlog.service

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "알림 접근" 권한 상태 관리.
 *
 * Android 13+ 의 POST_NOTIFICATIONS(알림 "표시" 권한)와는 별개의 특별 권한이며,
 * 사용자가 시스템 설정에서 직접 허용해야 한다. 이 앱은 알림을 표시하지 않으므로 POST_NOTIFICATIONS 는 요청하지 않는다.
 */
object NotificationAccess {

    private val _granted = MutableStateFlow(false)
    /** 권한 허용 여부. 화면 복귀(onResume)와 서비스 연결/해제 시 갱신된다. */
    val granted: StateFlow<Boolean> = _granted.asStateFlow()

    private val _connected = MutableStateFlow(false)
    /** 리스너 서비스가 현재 시스템에 연결되어 있는지 여부 */
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun refresh(context: Context) {
        _granted.value = isGranted(context)
    }

    internal fun onServiceConnectionChanged(context: Context, connected: Boolean) {
        _connected.value = connected
        refresh(context)
    }

    private fun componentName(context: Context) =
        ComponentName(context, NotificationLogListenerService::class.java)

    /**
     * 권한은 있는데 서비스가 연결되지 않은 경우(앱 강제 종료·업데이트 직후 등) 재연결을 요청한다.
     * 시스템 동작을 우회하는 것이 아니라 공식 API(requestRebind)를 사용한다.
     */
    fun requestRebindIfNeeded(context: Context) {
        if (isGranted(context) && !_connected.value) {
            try {
                NotificationListenerService.requestRebind(componentName(context))
            } catch (_: Exception) {
                // 일부 기기에서 실패할 수 있으나 치명적이지 않다.
            }
        }
    }

    /** 시스템의 "알림 접근 허용" 설정 화면을 연다. */
    fun openSettings(context: Context) {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: 이 앱의 상세 토글 화면으로 바로 이동
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    componentName(context).flattenToString(),
                )
                .addFlags(flags)
            try {
                context.startActivity(detail)
                return
            } catch (_: ActivityNotFoundException) {
                // 일부 제조사 기기에서는 상세 화면이 없으므로 목록 화면으로 대체
            }
        }
        try {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(flags))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(flags))
        }
    }
}

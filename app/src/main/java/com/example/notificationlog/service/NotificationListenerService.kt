package com.example.notificationlog.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.notificationlog.NotificationLogApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 기기에 게시되는 알림을 수신해 로컬 DB 에 저장하는 서비스.
 *
 * - 시스템이 직접 바인딩/수명 관리를 하므로 별도의 포그라운드 서비스가 필요 없다.
 * - 사용자가 "알림 접근"을 허용해야만 동작한다.
 * - 수신한 내용은 Room DB(기기 내부)에만 저장되며 외부로 전송하지 않는다.
 */
class NotificationLogListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationAccess.onServiceConnectionChanged(this, connected = true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationAccess.onServiceConnectionChanged(this, connected = false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 자기 자신의 알림은 기록하지 않는다.
        if (sbn.packageName == packageName) return
        // 그룹 요약 알림은 개별 알림과 내용이 중복되므로 제외한다.
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val container = (application as NotificationLogApp).container
        scope.launch {
            try {
                val captured = NotificationParser.parse(this@NotificationLogListenerService, sbn)
                val saved = container.notificationRepository.record(captured)
                if (saved) NotificationParser.cacheIconIfNeeded(this@NotificationLogListenerService, sbn)
            } catch (e: Exception) {
                // 한 건 처리 실패가 서비스 전체를 중단시키지 않도록 로그만 남긴다 (알림 내용은 로그에 남기지 않음).
                Log.w(TAG, "알림 저장 실패: ${sbn.packageName}", e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "NotificationLog"
    }
}

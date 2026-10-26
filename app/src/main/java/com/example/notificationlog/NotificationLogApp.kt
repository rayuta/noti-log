package com.example.notificationlog

import android.app.Application
import com.example.notificationlog.worker.CleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationLogApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // 하루 1회 정리 작업 등록 + 앱 실행 시 즉시 한 번 정리
        CleanupWorker.schedule(this)
        appScope.launch {
            runCatching { container.notificationRepository.cleanupExpired() }
        }
    }
}

package com.example.notificationlog.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.notificationlog.NotificationLogApp
import java.util.concurrent.TimeUnit

/** 하루 한 번 보관 기간이 지난 알림을 삭제하는 작업 (무기한이면 아무것도 삭제하지 않는다). */
class CleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val repository = (applicationContext as NotificationLogApp).container.notificationRepository
        repository.cleanupExpired()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "notification_cleanup"

        /** 이미 등록되어 있으면 유지(KEEP)하므로 앱 실행마다 호출해도 안전하다. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

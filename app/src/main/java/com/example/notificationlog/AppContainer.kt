package com.example.notificationlog

import android.content.Context
import com.example.notificationlog.data.local.AppDatabase
import com.example.notificationlog.data.repository.AppRuleRepository
import com.example.notificationlog.data.repository.FilterRepository
import com.example.notificationlog.data.repository.InstalledAppsRepository
import com.example.notificationlog.data.repository.NotificationRepository
import com.example.notificationlog.data.repository.SettingsRepository
import com.example.notificationlog.export.NotificationExporter

/**
 * 간단한 수동 의존성 주입 컨테이너.
 * 앱 규모에 비해 Hilt 는 과도하므로 Application 에서 한 번만 생성해 공유한다.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database = AppDatabase.build(appContext)

    val settingsRepository = SettingsRepository(database.settingDao())
    val appRuleRepository = AppRuleRepository(database.appRuleDao(), settingsRepository)
    val notificationRepository = NotificationRepository(
        database.notificationDao(),
        settingsRepository,
        appRuleRepository,
    )
    val filterRepository = FilterRepository(database.keywordFilterDao())
    val installedAppsRepository = InstalledAppsRepository(appContext)
    val notificationExporter = NotificationExporter(appContext, notificationRepository)
}

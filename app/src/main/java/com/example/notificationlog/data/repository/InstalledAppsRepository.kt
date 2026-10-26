package com.example.notificationlog.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.notificationlog.domain.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * 설치된 앱 조회.
 * QUERY_ALL_PACKAGES 같은 넓은 권한 대신 매니페스트 <queries> 에 선언한 "런처 앱"만 조회한다.
 */
class InstalledAppsRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    suspend fun launchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        val collator = Collator.getInstance(Locale.KOREAN)
        resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                AppInfo(pkg, info.loadLabel(pm).toString())
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** 패키지명으로 앱 이름 조회. 조회할 수 없으면 null */
    fun labelOf(packageName: String): String? =
        applicationInfoOf(packageName)?.let { pm.getApplicationLabel(it).toString() }

    fun applicationInfoOf(packageName: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

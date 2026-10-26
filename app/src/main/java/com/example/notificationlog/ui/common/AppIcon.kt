package com.example.notificationlog.ui.common

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.notificationlog.service.AppIconFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 앱 아이콘 메모리 캐시. DB 에는 패키지명(아이콘 식별 정보)만 저장하고 아이콘은 표시 시점에 불러온다. */
private object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(200)

    fun get(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(context: Context, packageName: String, sizePx: Int): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(sizePx, sizePx).asImageBitmap()
            } catch (_: Exception) {
                // PackageManager 로 조회되지 않으면 수신 시 캐시해 둔 아이콘 파일을 사용
                val file = AppIconFiles.fileFor(context, packageName)
                if (file.exists()) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null
            }
        }
        if (bitmap != null) cache.put(packageName, bitmap)
        return bitmap
    }
}

@Composable
fun AppIcon(packageName: String, appName: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState(initialValue = AppIconCache.get(packageName), packageName) {
        if (value == null) value = AppIconCache.load(context, packageName, sizePx)
    }
    val image = bitmap
    if (image != null) {
        Image(bitmap = image, contentDescription = appName, modifier = modifier.size(size))
    } else {
        // 아이콘을 찾을 수 없으면 앱 이름 첫 글자로 대체
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = appName.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = (size.value * 0.45f).sp,
            )
        }
    }
}

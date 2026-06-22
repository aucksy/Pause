package com.pause.app.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pause.app.domain.model.AppDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Shows the real installed launcher icon for an app, falling back to the brand-coloured monogram
 * tile when the app isn't installed (or its icon can't be read). Icons are loaded off the main
 * thread and cached process-wide so repeated rows don't re-hit PackageManager.
 *
 * Note: reading another app's icon requires that package to be visible — the manifest declares a
 * <queries> entry for each catalogued package so this works on Android 11+.
 */
@Composable
fun AppIcon(
    definition: AppDefinition,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, definition.packageName) {
        value = AppIconLoader.load(context, definition.packageName)
    }

    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = definition.label,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(percent = 28)),
        )
    } else {
        AppMonogram(definition = definition, size = size, modifier = modifier)
    }
}

private object AppIconLoader {
    private const val SIZE_PX = 144
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val notInstalled: MutableSet<String> = ConcurrentHashMap.newKeySet()

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        cache[packageName]?.let { return it }
        if (notInstalled.contains(packageName)) return null
        return withContext(Dispatchers.IO) {
            val loaded = runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(SIZE_PX, SIZE_PX)
                    .asImageBitmap()
            }.getOrNull()
            if (loaded != null) {
                cache[packageName] = loaded
            } else {
                notInstalled.add(packageName)
            }
            loaded
        }
    }
}

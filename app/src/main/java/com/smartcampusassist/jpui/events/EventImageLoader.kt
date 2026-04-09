package com.smartcampusassist.jpui.events

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

@Composable
internal fun RemoteEventImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, imageUrl) {
        value = EventImageMemoryCache[imageUrl] ?: loadEventImageBitmap(imageUrl)?.also { bitmap ->
            EventImageMemoryCache[imageUrl] = bitmap
        }
    }

    val resolvedBitmap = imageBitmap ?: return
    Image(
        bitmap = resolvedBitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

private object EventImageMemoryCache {
    private val bitmaps = ConcurrentHashMap<String, ImageBitmap>()

    operator fun get(url: String): ImageBitmap? = bitmaps[url]

    operator fun set(url: String, bitmap: ImageBitmap) {
        bitmaps[url] = bitmap
    }
}

private suspend fun loadEventImageBitmap(imageUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        URL(imageUrl).openStream().use { input ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inSampleSize = 2
            }
            BitmapFactory.decodeStream(input, null, options)
                ?.asImageBitmap()
        }
    }.getOrNull()
}

package com.arroom.characters.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * AR-сцена рисуется на SurfaceView, поэтому обычный view.draw(canvas)
 * вернёт чёрный кадр — нужен PixelCopy.
 */
suspend fun captureArView(view: View): Bitmap? = suspendCancellableCoroutine { cont ->
    if (view.width <= 0 || view.height <= 0) {
        cont.resume(null); return@suspendCancellableCoroutine
    }
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val handler = Handler(Looper.getMainLooper())

    if (view is SurfaceView) {
        PixelCopy.request(view, bitmap, { result ->
            cont.resume(if (result == PixelCopy.SUCCESS) bitmap else null)
        }, handler)
    } else {
        val window = (view.context as? android.app.Activity)?.window
        if (window == null) {
            cont.resume(null)
        } else {
            val loc = IntArray(2).also { view.getLocationInWindow(it) }
            PixelCopy.request(
                window,
                android.graphics.Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height),
                bitmap,
                { result -> cont.resume(if (result == PixelCopy.SUCCESS) bitmap else null) },
                handler
            )
        }
    }
}

/** Сохранение в общую галерею через MediaStore — разрешения не нужны с API 29+. */
suspend fun saveToGallery(
    context: Context,
    bitmap: Bitmap,
    albumName: String = "AR Room"
): Uri? = withContext(Dispatchers.IO) {
    val name = "ARRoom_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$albumName")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext null

    runCatching {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    }.getOrElse {
        resolver.delete(uri, null, null)
        null
    }
}

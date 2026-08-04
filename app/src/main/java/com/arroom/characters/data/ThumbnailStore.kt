package com.arroom.characters.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Миниатюры персонажей для карусели.
 *
 * Снимаются кадром сцены сразу после того, как персонаж встал на место:
 * кадрируем квадрат вокруг точки касания и ужимаем до 144 px.
 *
 * Почему не офскрин-рендер модели: полноценный рендер в текстуру через
 * Filament — это отдельный пайплайн с RenderTarget и readPixels, десятки
 * строк хрупкого кода поверх API, которое меняется от версии к версии.
 * Кадрирование живой сцены использует только то, что уже работает,
 * и даёт узнаваемую картинку с первого размещения. Минус честный:
 * в миниатюру попадает кусок комнаты позади персонажа.
 */
class ThumbnailStore(context: Context) {

    private val dir = File(context.filesDir, "thumbs").apply { mkdirs() }

    fun pathFor(characterId: String): String? =
        fileFor(characterId).takeIf { it.exists() && it.length() > 0 }?.absolutePath

    fun has(characterId: String): Boolean = pathFor(characterId) != null

    /**
     * @param centerX,[centerY] точка касания в пикселях — вокруг неё и кадрируем.
     */
    suspend fun save(
        characterId: String,
        frame: Bitmap,
        centerX: Float,
        centerY: Float
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val side = (min(frame.width, frame.height) / 2.6f).toInt().coerceAtLeast(64)

            // Персонаж стоит НАД точкой касания, поэтому центр кадра
            // поднимаем на четверть стороны — иначе в миниатюру попадёт
            // в основном пол под ним.
            val left = (centerX - side / 2f).toInt().coerceIn(0, max(0, frame.width - side))
            val top = (centerY - side * 0.75f).toInt().coerceIn(0, max(0, frame.height - side))

            val crop = Bitmap.createBitmap(frame, left, top, side, side)
            val scaled = Bitmap.createScaledBitmap(crop, SIZE, SIZE, true)

            fileFor(characterId).outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            if (crop != scaled) crop.recycle()
            scaled.recycle()
            true
        }.getOrDefault(false)
    }

    fun remove(characterId: String) {
        fileFor(characterId).delete()
    }

    private fun fileFor(characterId: String) =
        File(dir, characterId.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".png")

    private companion object {
        const val SIZE = 144
    }
}

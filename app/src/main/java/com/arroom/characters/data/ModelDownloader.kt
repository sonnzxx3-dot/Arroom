package com.arroom.characters.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Скачивает удалённые модели один раз и держит их на диске.
 *
 * Зачем свой загрузчик вместо того, чтобы отдать URL напрямую в Filament:
 * во-первых, повторное размещение того же персонажа перестаёт лезть в сеть;
 * во-вторых, приложение начинает работать офлайн после первого запуска;
 * в-третьих, появляется прогресс — без него человек десять секунд смотрит
 * на крутилку и решает, что приложение зависло.
 */
class ModelDownloader(context: Context) {

    private val cacheDir = File(context.cacheDir, "models").apply { mkdirs() }

    /**
     * @return абсолютный путь к локальному файлу или null, если скачать не вышло.
     */
    suspend fun ensureLocal(
        url: String,
        onProgress: (Float) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        val target = File(cacheDir, keyOf(url))

        if (target.exists() && target.length() > 0) {
            target.setLastModified(System.currentTimeMillis())
            return@withContext target.absolutePath
        }

        val part = File(cacheDir, "${target.name}.part")
        val ok = runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 20000
                instanceFollowRedirects = true
            }
            if (connection.responseCode !in 200..299) return@runCatching false

            val total = connection.contentLengthLong
            var written = 0L

            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        // Длина неизвестна у chunked-ответов — тогда прогресса нет
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            written > 0
        }.getOrDefault(false)

        if (!ok) {
            part.delete()
            return@withContext null
        }

        part.renameTo(target)
        trimCache()
        target.absolutePath
    }

    fun isCached(url: String): Boolean =
        File(cacheDir, keyOf(url)).let { it.exists() && it.length() > 0 }

    /** Держим кеш в разумных рамках: самые старые файлы уходят первыми. */
    private fun trimCache() {
        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return

        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_CACHE_BYTES) return
            total -= file.length()
            file.delete()
        }
    }

    private fun keyOf(url: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".glb"
    }

    private companion object {
        const val MAX_CACHE_BYTES = 400L * 1024 * 1024
    }
}

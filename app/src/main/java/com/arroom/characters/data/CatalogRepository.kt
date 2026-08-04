package com.arroom.characters.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Каталог персонажей тянется с сервера, чтобы добавлять контент
 * без обновления приложения в сторе.
 *
 * Стратегия: сеть -> кеш на диске -> вшитый BuiltInCatalog.
 * Пользователь никогда не видит пустой экран.
 *
 * Формат JSON:
 * {
 *   "version": 1,
 *   "characters": [
 *     { "id": "fox", "title": "Лис", "url": "https://cdn/fox.glb", "scale": 0.7 }
 *   ]
 * }
 */
class CatalogRepository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "catalog.json")

    /** Замените на свой endpoint. Пустая строка = работать только на вшитом каталоге. */
    private val remoteUrl: String = ""

    suspend fun load(): List<CharacterItem> = withContext(Dispatchers.IO) {
        fetchRemote()?.let { json ->
            runCatching { cacheFile.writeText(json) }
            parse(json)?.let { return@withContext it }
        }
        if (cacheFile.exists()) {
            parse(cacheFile.readText())?.let { return@withContext it }
        }
        BuiltInCatalog.items(context)
    }

    private fun fetchRemote(): String? {
        if (remoteUrl.isBlank()) return null
        return runCatching {
            (URL(remoteUrl).openConnection() as HttpURLConnection).run {
                connectTimeout = 4000
                readTimeout = 6000
                requestMethod = "GET"
                if (responseCode != 200) return null
                inputStream.bufferedReader().use { it.readText() }
            }
        }.getOrNull()
    }

    private fun parse(json: String): List<CharacterItem>? = runCatching {
        val arr = JSONObject(json).getJSONArray("characters")
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val url = o.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
            CharacterItem(
                id = o.getString("id"),
                title = o.optString("title", "Character"),
                source = ModelSource.Remote(url),
                scaleToUnits = o.optDouble("scale", 0.8).toFloat()
            )
        }.takeIf { it.isNotEmpty() }
    }.getOrNull()
}

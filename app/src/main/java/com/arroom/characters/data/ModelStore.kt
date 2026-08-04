package com.arroom.characters.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import com.arroom.characters.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Демо-персонажи из публичной библиотеки Khronos glTF-Sample-Assets.
 * Лицензии: Fox — CC0, CesiumMan / BrainStem — CC-BY 4.0 (см. README).
 *
 * ДЛЯ РЕЛИЗА: замените на свои модели в assets/models/ или на свой CDN,
 * чтобы приложение работало без интернета и без чужих лицензий.
 */
object BuiltInCatalog {

    private const val BASE =
        "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models"

    fun items(context: Context): List<CharacterItem> = listOf(
        CharacterItem(
            id = "fox",
            title = context.getString(R.string.char_fox),
            source = ModelSource.Remote("$BASE/Fox/glTF-Binary/Fox.glb"),
            scaleToUnits = 0.7f
        ),
        CharacterItem(
            id = "cesium_man",
            title = context.getString(R.string.char_human),
            source = ModelSource.Remote("$BASE/CesiumMan/glTF-Binary/CesiumMan.glb"),
            scaleToUnits = 1.7f
        ),
        CharacterItem(
            id = "brainstem",
            title = context.getString(R.string.char_robot),
            source = ModelSource.Remote("$BASE/BrainStem/glTF-Binary/BrainStem.glb"),
            scaleToUnits = 1.4f
        ),
        CharacterItem(
            id = "duck",
            title = context.getString(R.string.char_duck),
            source = ModelSource.Remote("$BASE/Duck/glTF-Binary/Duck.glb"),
            scaleToUnits = 0.4f
        )
    )
}

/** Результат импорта: либо готовый персонаж, либо понятная причина отказа. */
sealed interface ImportResult {
    data class Success(val item: CharacterItem) : ImportResult
    data class Failure(@StringRes val messageRes: Int, val arg: Any? = null) : ImportResult
}

/**
 * Импорт и хранение пользовательских моделей.
 * Файл копируется из SAF-Uri во внутреннюю память, чтобы Filament мог
 * читать его напрямую после перезапуска приложения.
 */
class ModelStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("model_store", Context.MODE_PRIVATE)
    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }

    fun loadImported(): List<CharacterItem> = runCatching {
        val json = JSONArray(prefs.getString(KEY, "[]"))
        (0 until json.length()).mapNotNull { i ->
            val o = json.getJSONObject(i)
            val path = o.getString("path")
            if (!File(path).exists()) return@mapNotNull null
            CharacterItem(
                id = o.getString("id"),
                title = o.getString("title"),
                source = ModelSource.Local(path),
                scaleToUnits = o.optDouble("scale", 0.8).toFloat(),
                isUserImported = true
            )
        }
    }.getOrDefault(emptyList())

    /**
     * Копирует выбранный пользователем .glb/.gltf в приватную папку приложения.
     *
     * Валидация здесь не формальность: пользователь через системный выбор
     * файлов может подсунуть что угодно — FBX, архив, гигабайтную сцену.
     * Без проверки Filament молча вернёт null, и человек увидит только
     * «модель не загрузилась», не понимая, что именно не так.
     */
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val displayName = queryName(uri) ?: "model_${System.currentTimeMillis()}.glb"
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val temp = File(modelsDir, ".tmp_$safeName")

        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)

        if (!copied) {
            temp.delete()
            return@withContext ImportResult.Failure(R.string.import_err_read)
        }

        validate(temp)?.let { failure ->
            temp.delete()
            return@withContext failure
        }

        val target = File(modelsDir, "${UUID.randomUUID().toString().take(8)}_$safeName")
        if (!temp.renameTo(target)) {
            temp.delete()
            return@withContext ImportResult.Failure(R.string.import_err_read)
        }

        val item = CharacterItem(
            id = target.name,
            title = displayName.substringBeforeLast('.').take(18),
            source = ModelSource.Local(target.absolutePath),
            scaleToUnits = 0.8f,
            isUserImported = true
        )
        persist(loadImported() + item)
        ImportResult.Success(item)
    }

    /** null = файл в порядке. */
    private fun validate(file: File): ImportResult.Failure? {
        val head = runCatching {
            file.inputStream().use { input ->
                val buffer = ByteArray(GltfValidator.HEAD_SCAN_BYTES)
                val read = input.read(buffer)
                if (read <= 0) ByteArray(0) else buffer.copyOf(read)
            }
        }.getOrDefault(ByteArray(0))

        return when (val verdict = GltfValidator.check(file.length(), head)) {
            GltfValidator.Verdict.Ok -> null
            is GltfValidator.Verdict.TooBig ->
                ImportResult.Failure(R.string.import_err_too_big, verdict.limitMb)
            GltfValidator.Verdict.NotGltf ->
                ImportResult.Failure(R.string.import_err_not_gltf)
            is GltfValidator.Verdict.Compressed ->
                ImportResult.Failure(R.string.import_err_compressed, verdict.label)
            GltfValidator.Verdict.Unreadable ->
                ImportResult.Failure(R.string.import_err_read)
        }
    }

    fun remove(item: CharacterItem) {
        (item.source as? ModelSource.Local)?.let { File(it.filePath).delete() }
        persist(loadImported().filterNot { it.id == item.id })
    }

    private fun persist(items: List<CharacterItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val path = (item.source as? ModelSource.Local)?.filePath ?: return@forEach
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("path", path)
                    .put("scale", item.scaleToUnits.toDouble())
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun queryName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }

    private companion object {
        const val KEY = "imported_models"
    }
}

package com.arroom.characters.data

/**
 * Проверка файла модели без единой зависимости от Android — это позволяет
 * покрыть её обычными JVM-тестами, которые гоняются в CI за секунды.
 *
 * Всё, что нужно для вердикта, есть в первых сотнях килобайт: у .glb
 * там сигнатура и JSON-чанк, у .gltf — сам JSON целиком начинается там же.
 */
object GltfValidator {

    const val MAX_SIZE_MB = 80
    const val MAX_SIZE_BYTES = MAX_SIZE_MB * 1024L * 1024L

    /** JSON-чанк glTF всегда идёт до бинарных данных, 256 КБ хватает с запасом. */
    const val HEAD_SCAN_BYTES = 256 * 1024

    private val UNSUPPORTED = listOf(
        "KHR_draco_mesh_compression" to "Draco",
        "EXT_meshopt_compression" to "Meshopt"
    )

    sealed interface Verdict {
        data object Ok : Verdict
        data class TooBig(val limitMb: Int) : Verdict
        data object NotGltf : Verdict
        data class Compressed(val label: String) : Verdict
        data object Unreadable : Verdict
    }

    fun check(sizeBytes: Long, head: ByteArray): Verdict {
        if (sizeBytes > MAX_SIZE_BYTES) return Verdict.TooBig(MAX_SIZE_MB)
        if (head.size < 4) return Verdict.Unreadable

        // Бинарный .glb: магия "glTF" в первых четырёх байтах
        val isGlb = head[0] == 0x67.toByte() && head[1] == 0x6C.toByte() &&
            head[2] == 0x54.toByte() && head[3] == 0x46.toByte()

        // Текстовый .gltf: это JSON, первый значимый символ — открывающая скобка
        val isGltfJson = head.take(64)
            .map { it.toInt().toChar() }
            .firstOrNull { !it.isWhitespace() } == '{'

        if (!isGlb && !isGltfJson) return Verdict.NotGltf

        // ISO_8859_1 — байт в символ один к одному, поэтому поиск подстроки
        // не сломается на произвольных бинарных данных после JSON-чанка
        val text = String(head, Charsets.ISO_8859_1)
        UNSUPPORTED.forEach { (marker, label) ->
            if (text.contains(marker)) return Verdict.Compressed(label)
        }
        return Verdict.Ok
    }
}

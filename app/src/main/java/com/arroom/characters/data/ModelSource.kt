package com.arroom.characters.data

/**
 * Откуда берётся 3D-модель. SceneView ModelLoader принимает строку-путь,
 * поэтому у каждого источника есть [location] в понятном ему формате.
 */
sealed interface ModelSource {

    val location: String

    /** Модель, вшитая в APK: app/src/main/assets/models/fox.glb -> Asset("models/fox.glb") */
    data class Asset(val path: String) : ModelSource {
        override val location: String get() = path
    }

    /** Модель, скачиваемая по сети (кешируется ModelLoader'ом) */
    data class Remote(val url: String) : ModelSource {
        override val location: String get() = url
    }

    /** Модель, импортированная пользователем и скопированная в files/models/ */
    data class Local(val filePath: String) : ModelSource {
        override val location: String get() = "file://$filePath"
    }
}

/**
 * Один персонаж в карусели.
 *
 * @param scaleToUnits высота модели в метрах после нормализации.
 *        0.5 = полметра. Пользователь потом меняет размер щипком.
 */
data class CharacterItem(
    val id: String,
    val title: String,
    val source: ModelSource,
    val previewUrl: String? = null,
    val scaleToUnits: Float = 0.6f,
    val isUserImported: Boolean = false
)

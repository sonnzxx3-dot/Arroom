package com.arroom.characters.ar

import com.google.android.filament.View

/**
 * Настройки рендера, которые сильнее всего влияют на нагрев и плавность.
 *
 * Тени и screen-space отражения здесь НЕ трогаем: соответствующие свойства
 * Filament закрыты на уровне пакета и из приложения недоступны. Тенями
 * управляет сам SceneView через окружение AR-сцены.
 *
 * Динамическое разрешение — компромисс с нагревом: при просадке FPS
 * Filament сам снижает внутреннее разрешение вместо того, чтобы
 * ронять частоту кадров.
 */
object RenderQuality {

    fun apply(view: View, highEnd: Boolean = true) {
        runCatching {
            view.antiAliasing = View.AntiAliasing.FXAA
        }

        runCatching {
            view.dynamicResolutionOptions = View.DynamicResolutionOptions().apply {
                enabled = true
                quality = if (highEnd) View.QualityLevel.MEDIUM else View.QualityLevel.LOW
                // При нагреве разрешаем падать глубже: лучше мягкая картинка,
                // чем троттлинг и потеря трекинга посреди записи
                minScale = if (highEnd) 0.6f else 0.45f
                maxScale = 1.0f
            }
        }

        // Постобработку не выключаем: без неё ломается тонемаппинг
        // и модель выглядит пересвеченной относительно кадра камеры.
    }
}

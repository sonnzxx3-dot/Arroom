package com.arroom.characters.ar

import com.google.android.filament.View

/**
 * Настройки рендера, которые сильнее всего влияют на ощущение
 * «объект реально стоит в комнате», а не наклеен поверх видео.
 *
 * Главное здесь — мягкие тени. Без них мозг не привязывает модель к полу,
 * и вся сцена читается как стикер, каким бы качественным ни был меш.
 *
 * Динамическое разрешение — компромисс с нагревом: при просадке FPS
 * Filament сам снижает внутреннее разрешение вместо того, чтобы
 * ронять частоту кадров.
 */
object RenderQuality {

    fun apply(view: View, highEnd: Boolean = true) {
        runCatching {
            view.isShadowingEnabled = true
            view.shadowType = if (highEnd) View.ShadowType.PCSS else View.ShadowType.PCF
        }

        runCatching {
            view.antiAliasing = View.AntiAliasing.FXAA
        }

        runCatching {
            view.dynamicResolutionOptions = View.DynamicResolutionOptions().apply {
                enabled = true
                quality = View.QualityLevel.MEDIUM
                minScale = 0.6f
                maxScale = 1.0f
            }
        }

        // Screen-space отражения в AR почти не видны, но стоят кадров
        runCatching { view.isScreenSpaceRefractionEnabled = false }

        // Постобработку не выключаем: без неё ломается тонемаппинг
        // и модель выглядит пересвеченной относительно кадра камеры.
    }
}

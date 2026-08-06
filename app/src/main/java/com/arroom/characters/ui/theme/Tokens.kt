package com.arroom.characters.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/**
 * Единственный источник правды по визуалу приложения.
 *
 * Раньше цвета, радиусы и полупрозрачности были рассыпаны по файлам как
 * сырые литералы (0xCC12121A тут, RoundedCornerShape(20.dp) там). Скилл
 * ui-ux-pro-max справедливо отмечает это как анти-паттерн: любое изменение
 * тона приходится ловить по всему коду, и значения неизбежно расходятся.
 *
 * Стиль — Modern Dark (Cinematic) поверх живого видео с камеры. Отсюда
 * два принципа, которые скилл называет критичными для этого стиля:
 *  1. Никакого чистого #000000 — на OLED он даёт смазывание в движении.
 *  2. Стекло должно быть достаточно плотным, чтобы текст поверх камеры
 *     сохранял контраст 4.5:1 в любой сцене, светлой или тёмной.
 */
object Tokens {

    // --- Акценты ---
    val Violet = Color(0xFF8B6CFF)
    val VioletDeep = Color(0xFF5C46BE)
    val Cyan = Color(0xFF3DDCFF)

    val AccentGradient = Brush.linearGradient(listOf(Violet, Cyan))

    // --- Поверхности (не чистый чёрный) ---
    val Ink = Color(0xFF0B0B12)          // фон вне AR (онбординг, ошибки)
    val Surface = Color(0xFF14141C)      // модальные листы, диалоги

    /**
     * Стекло поверх камеры. 0xE6 ≈ 90% непрозрачности — намеренно плотнее
     * «модного» полупрозрачного стекла: над ярким окном или белой стеной
     * более прозрачный фон уронил бы контраст текста ниже нормы WCAG.
     */
    val Glass = Color(0xE614141C)
    val GlassStroke = Color(0x1FFFFFFF)  // 12% белого — видимая граница в любой сцене
    val Scrim = Color(0x99070710)        // 60% — затемнение под модалками, порог из скилла

    // --- Текст ---
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB9B9C6) // ≥4.5:1 на Surface
    val TextTertiary = Color(0xFF8A8A99)  // только для крупного/неважного
    val Inactive = Color(0xFF2A2A36)      // неактивные точки-индикаторы, треки

    // --- Семантика ---
    val Recording = Color(0xFFE2504A)
    val Success = Cyan

    // --- Радиусы ---
    val RadiusPill = RoundedCornerShape(100)
    val RadiusLg = RoundedCornerShape(24.dp)
    val RadiusMd = RoundedCornerShape(20.dp)
    val RadiusSm = RoundedCornerShape(14.dp)

    // --- Ритм отступов (кратно 4dp, как требует скилл) ---
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp

    // --- Размеры целей касания (минимум 48dp по Material) ---
    val TouchMin = 48.dp
    val IconSm = 18.dp
    val IconMd = 22.dp
    val Shutter = 72.dp

    // --- Длительности анимаций (окно 150–300ms из чеклиста) ---
    const val DurFast = 130
    const val DurBase = 220
    const val DurSlow = 300

    // Прижатие кнопок — единое по всему приложению
    const val PressScale = 0.94f

    /** Мягкая тень под текстом поверх камеры: читаемость без плашки. */
    val TextOnCameraShadow = Shadow(
        color = Color(0xB3000000),
        offset = Offset(0f, 1f),
        blurRadius = 6f
    )
}

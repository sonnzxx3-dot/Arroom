package com.arroom.characters.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Единое прижатие для всех кнопок.
 *
 * Скилл требует три вещи разом: видимый отклик на нажатие за 80–150ms,
 * отклик, который НЕ двигает соседний контент (scale от центра, не сдвиг),
 * и цель касания не меньше 48dp даже если иконка мельче. Раньше каждая
 * кнопка решала это по-своему — где-то был ripple, где-то ничего.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    pressScale: Float = Tokens.PressScale
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f),
        label = "press"
    )
    return this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = rememberRipple(bounded = false),
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Круглая стеклянная кнопка-иконка. Визуальный размер и область касания
 * разведены: иконка 22dp, но тап ловится в 48dp — правило hitSlop из скилла.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = Tokens.TouchMin,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.35f,
        label = "enabledAlpha"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .sizeIn(minWidth = Tokens.TouchMin, minHeight = Tokens.TouchMin)
            .size(size)
            .clip(CircleShape)
            .background(Tokens.Glass)
            .border(1.dp, Tokens.GlassStroke, CircleShape)
            .pressable(onClick = onClick, enabled = enabled)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                Tokens.TextPrimary.copy(alpha = alpha)
        ) { content() }
    }
}

/** Стеклянная поверхность-контейнер с единой границей и радиусом. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = Tokens.RadiusPill,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Tokens.Glass)
            .border(1.dp, Tokens.GlassStroke, shape),
        contentAlignment = Alignment.Center
    ) { content() }
}

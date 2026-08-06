package com.arroom.characters.collection

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arroom.characters.R
import com.arroom.characters.ui.theme.LocalReducedMotion
import com.arroom.characters.ui.theme.Tokens
import com.arroom.characters.ui.theme.pressable

/** Список наборов с ценами. Клик открывает набор, если хватает монет. */
@Composable
fun PacksTab(
    coins: Int,
    rarityLabel: (Rarity) -> String,
    onOpen: (Pack) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(Tokens.Space4),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space3),
        modifier = Modifier.fillMaxSize()
    ) {
        items(Pack.values()) { pack ->
            PackRow(
                pack = pack,
                affordable = coins >= pack.price,
                onClick = { onOpen(pack) }
            )
        }
    }
}

@Composable
private fun PackRow(pack: Pack, affordable: Boolean, onClick: () -> Unit) {
    // Цвет коробки намекает на «уровень» набора
    val accent = when (pack) {
        Pack.BASIC -> Rarity.COMMON.color
        Pack.PREMIUM -> Rarity.RARE.color
        Pack.LEGENDARY -> Rarity.LEGENDARY.color
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Tokens.Surface)
            .border(1.dp, Tokens.GlassStroke, RoundedCornerShape(18.dp))
            .then(if (affordable) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(Tokens.Space4)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(accent.copy(0.3f), Color(0xFF12121A))))
        ) {
            Icon(Icons.Rounded.Inventory2, null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(Tokens.Space3))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(pack.titleRes),
                color = Tokens.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                packOdds(pack),
                color = Tokens.TextSecondary,
                fontSize = 11.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Paid,
                null,
                tint = if (affordable) Color(0xFFFFB020) else Tokens.TextTertiary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(Tokens.Space1))
            Text(
                "${pack.price}",
                color = if (affordable) Color(0xFFFFB020) else Tokens.TextTertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun packOdds(pack: Pack): String {
    // Короткая человекочитаемая сводка шансов на редкое-и-выше
    val total = pack.weights.sum()
    val rareUp = (pack.weights[1] + pack.weights[2] + pack.weights[3]) * 100 / total
    return "Rare+: $rareUp%"
}

/**
 * Полноэкранная анимация открытия набора.
 *
 * Драматургия важна: карта «прилетает» с переворотом, вспышка цвета
 * редкости, и только у новых карт — метка NEW. Скилл про reduced-motion
 * учтён: при отключённых анимациях карта просто появляется без флипа.
 */
@Composable
fun PackOpeningOverlay(
    result: PackResult,
    rarityLabel: String,
    onDone: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val rarityColor = result.rarity.color

    // Прогресс появления: 0 -> 1
    val reveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            reveal.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
    }

    // Медленное вращение лучей у высоких редкостей
    val transition = rememberInfiniteTransition(label = "burst")
    val spin by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "spin"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF0070710))
            .pressable(onClick = onDone),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(contentAlignment = Alignment.Center) {
                // Лучи-свечение за картой
                if (result.rarity.ordinal >= Rarity.RARE.ordinal && !reducedMotion) {
                    Canvas(
                        Modifier
                            .size(320.dp)
                            .rotate(spin)
                            .blur(30.dp)
                            .scale(reveal.value)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(rarityColor.copy(0.5f), Color.Transparent),
                                radius = size.minDimension / 2f
                            ),
                            radius = size.minDimension / 2f,
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    }
                }

                // Сама карта
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(0.5f + reveal.value * 0.5f)
                        .width(200.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Tokens.Surface)
                        .border(3.dp, rarityColor, RoundedCornerShape(22.dp))
                        .padding(Tokens.Space5)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(rarityColor.copy(0.2f), Color(0xFF12121A))
                                )
                            )
                    ) {
                        Text(result.item.title.take(1).uppercase(), fontSize = 56.sp, color = rarityColor)
                    }
                    Spacer(Modifier.height(Tokens.Space3))
                    Text(
                        result.item.title,
                        color = Tokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(rarityLabel, color = rarityColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (result.isNew) {
                Spacer(Modifier.height(Tokens.Space4))
                Text(
                    "✦ ${stringResource(R.string.pack_new_card)} ✦",
                    color = Tokens.Cyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(Tokens.Space6))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Tokens.AccentGradient)
                    .pressable(onClick = onDone)
                    .padding(horizontal = 40.dp, vertical = Tokens.Space3)
            ) {
                Text(
                    stringResource(R.string.pack_continue),
                    color = Tokens.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

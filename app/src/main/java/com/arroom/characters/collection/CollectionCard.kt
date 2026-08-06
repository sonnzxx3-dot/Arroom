package com.arroom.characters.collection

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arroom.characters.ui.theme.LocalReducedMotion
import com.arroom.characters.ui.theme.Tokens
import java.io.File

/**
 * Коллекционная карточка. Именно это имел в виду пользователь под
 * «система сама делает анимированные карточки»: превью персонажа в рамке,
 * цвет и свечение которой заданы редкостью, а у высоких редкостей рамка
 * ещё и переливается.
 *
 * Не пойманные персонажи показываются тёмным силуэтом с замком — это
 * классический коллекционный крючок «покажи, чего не хватает».
 */
@Composable
fun CollectionCard(
    card: Card,
    rarityLabel: String,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "card")

    // Переливание рамки только у EPIC и LEGENDARY и только если анимации включены
    val animatedBorder = card.isOwned &&
        card.rarity.ordinal >= Rarity.EPIC.ordinal &&
        !reducedMotion

    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "shimmer"
    )

    val shape = RoundedCornerShape(18.dp)
    val borderBrush = when {
        !card.isOwned -> Brush.linearGradient(
            listOf(Color.White.copy(0.10f), Color.White.copy(0.04f))
        )
        animatedBorder -> Brush.sweepGradient(
            listOf(
                card.rarity.color,
                card.rarity.color.copy(alpha = 0.3f),
                Tokens.Cyan,
                card.rarity.color
            )
        )
        else -> Brush.linearGradient(listOf(card.rarity.color, card.rarity.color.copy(0.5f)))
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(Tokens.Surface)
            .border(if (card.isOwned) 2.dp else 1.dp, borderBrush, shape)
            .padding(6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(13.dp))
                .background(rarityBackdrop(card.rarity, card.isOwned)),
            contentAlignment = Alignment.Center
        ) {
            // Мягкое свечение под превью у пойманных высоких редкостей
            if (card.isOwned && card.rarity.ordinal >= Rarity.RARE.ordinal) {
                Canvas(Modifier.fillMaxSize().blur(24.dp)) {
                    drawCircle(
                        color = card.rarity.glow,
                        radius = size.minDimension / 2.4f,
                        center = Offset(size.width / 2f, size.height * 0.6f)
                    )
                }
            }

            if (card.thumbnailPath != null) {
                AsyncImage(
                    model = File(card.thumbnailPath),
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    // Силуэт: не пойманную карту гасим в чёрный
                    colorFilter = if (card.isOwned) null else silhouetteFilter(),
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp))
                )
            }

            if (!card.isOwned) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(0.5f),
                    modifier = Modifier.size(26.dp)
                )
            }

            if (card.favorite && card.isOwned) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Tokens.Cyan,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp)
                )
            }

            if (card.hasDuplicates) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color.Black.copy(0.55f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text("×${card.ownedCount}", color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            if (card.isOwned) card.title else "???",
            color = if (card.isOwned) Tokens.TextPrimary else Tokens.TextTertiary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            rarityLabel,
            color = if (card.isOwned) card.rarity.color else Tokens.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        )
        // Тонкая линия того же цвета — микроакцент редкости
        Box(
            Modifier
                .padding(horizontal = 2.dp, vertical = 3.dp)
                .height(2.dp)
                .fillMaxWidth(if (card.isOwned) 1f else 0.3f)
                .clip(RoundedCornerShape(100))
                .background(
                    if (card.isOwned) card.rarity.color.copy(0.6f)
                    else Tokens.Inactive
                )
        )
    }
}

private fun rarityBackdrop(rarity: Rarity, owned: Boolean): Brush {
    if (!owned) return Brush.linearGradient(listOf(Color(0xFF161620), Color(0xFF0E0E16)))
    return Brush.linearGradient(
        listOf(rarity.color.copy(0.16f), Color(0xFF12121A))
    )
}

private fun silhouetteFilter(): ColorFilter =
    ColorFilter.colorMatrix(ColorMatrix().apply {
        setToScale(0f, 0f, 0f, 1f) // всё в чёрный, альфа сохраняется
    })

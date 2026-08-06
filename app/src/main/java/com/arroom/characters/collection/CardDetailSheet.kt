package com.arroom.characters.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arroom.characters.R
import com.arroom.characters.ui.theme.Tokens
import com.arroom.characters.ui.theme.pressable

/**
 * Крупный вид карточки с действиями. Что доступно, зависит от состояния:
 *  - не пойманная → можно купить за монеты
 *  - пойманная с дублями → можно продать лишние
 *  - пойманная → можно добавить в избранное
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailSheet(
    card: Card,
    rarityLabel: String,
    wallet: Wallet,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Tokens.Surface,
        scrimColor = Tokens.Scrim
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = Tokens.Space5, vertical = Tokens.Space2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CollectionCard(
                card = card,
                rarityLabel = rarityLabel,
                modifier = Modifier.width(180.dp)
            )

            Spacer(Modifier.height(Tokens.Space4))

            if (card.isOwned) {
                Text(
                    card.title,
                    color = Tokens.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.owned_count, card.ownedCount),
                    color = Tokens.TextSecondary,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    stringResource(R.string.card_locked),
                    color = Tokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.card_locked_hint),
                    color = Tokens.TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(Tokens.Space5))

            // --- Действия ---
            when {
                !card.isOwned -> {
                    val price = card.rarity.shopPrice
                    val canAfford = wallet.canAfford(price)
                    PrimaryAction(
                        text = stringResource(R.string.buy_for, price),
                        enabled = canAfford,
                        onClick = onBuy
                    )
                    if (!canAfford) {
                        Spacer(Modifier.height(Tokens.Space2))
                        Text(
                            stringResource(R.string.not_enough_coins),
                            color = Tokens.Recording,
                            fontSize = 12.sp
                        )
                    }
                }
                else -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Space3)
                    ) {
                        SecondaryAction(
                            icon = if (card.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            text = stringResource(
                                if (card.favorite) R.string.unfavorite else R.string.favorite
                            ),
                            onClick = onToggleFavorite,
                            modifier = Modifier.weight(1f)
                        )
                        if (card.hasDuplicates) {
                            SecondaryAction(
                                icon = Icons.Rounded.Star,
                                text = stringResource(R.string.sell_for, card.rarity.sellPrice),
                                onClick = onSell,
                                modifier = Modifier.weight(1f),
                                tint = Color(0xFFFFB020)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Tokens.Space6))
        }
    }
}

@Composable
private fun PrimaryAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Tokens.AccentGradient
                else androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Tokens.Inactive, Tokens.Inactive)
                )
            )
            .then(if (enabled) Modifier.pressable(onClick = onClick) else Modifier)
    ) {
        Text(
            text,
            color = if (enabled) Tokens.TextPrimary else Tokens.TextTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SecondaryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Tokens.TextPrimary
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Tokens.Glass)
            .pressable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Tokens.Space2))
        Text(text, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

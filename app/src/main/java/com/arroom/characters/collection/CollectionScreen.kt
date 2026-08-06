package com.arroom.characters.collection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arroom.characters.R
import com.arroom.characters.data.CharacterItem
import com.arroom.characters.ui.theme.Tokens
import com.arroom.characters.ui.theme.pressable

private enum class Tab { COLLECTION, SHOP, PACKS, ACHIEVEMENTS, TRADE }

@Composable
fun CollectionScreen(
    cards: List<Card>,
    wallet: Wallet,
    rarityLabel: (Rarity) -> String,
    dailyAvailable: Boolean,
    dailyAmount: Int,
    streak: Int,
    onClaimDaily: () -> Unit,
    onBuy: (Card) -> Unit,
    onSell: (Card) -> Unit,
    onToggleFavorite: (Card) -> Unit,
    onOpenPack: (Pack) -> Unit,
    achievements: List<AchievementRow>,
    onClaimAchievement: (Achievement) -> Unit,
    onClose: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.COLLECTION) }
    var selected by remember { mutableStateOf<Card?>(null) }

    val owned = cards.count { it.isOwned }
    val total = cards.size

    Column(
        Modifier
            .fillMaxSize()
            .background(Tokens.Ink)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Space4, vertical = Tokens.Space3)
        ) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Tokens.TextPrimary,
                modifier = Modifier
                    .size(Tokens.TouchMin)
                    .clip(RoundedCornerShape(100))
                    .pressable(onClick = onClose)
                    .padding(13.dp)
            )
            Spacer(Modifier.width(Tokens.Space2))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.collection_title),
                    color = Tokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.collection_progress, owned, total),
                    color = Tokens.TextSecondary,
                    fontSize = 12.sp
                )
            }
            CoinBadge(wallet.coins)
        }

        val pct by animateFloatAsState(
            if (total == 0) 0f else owned.toFloat() / total,
            label = "progress"
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Space4)
                .height(4.dp)
                .clip(RoundedCornerShape(100))
                .background(Tokens.Inactive)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100))
                    .background(Tokens.AccentGradient)
            )
        }

        if (dailyAvailable) {
            DailyRewardBanner(
                amount = dailyAmount,
                streak = streak,
                onClaim = onClaimDaily,
                modifier = Modifier.padding(horizontal = Tokens.Space4, vertical = Tokens.Space3)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = Tokens.Space4, vertical = Tokens.Space4),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space2),
            modifier = Modifier.fillMaxWidth()
        ) {
            item { TabChip(stringResource(R.string.tab_collection), tab == Tab.COLLECTION) { tab = Tab.COLLECTION } }
            item { TabChip(stringResource(R.string.tab_shop), tab == Tab.SHOP) { tab = Tab.SHOP } }
            item { TabChip(stringResource(R.string.tab_packs), tab == Tab.PACKS) { tab = Tab.PACKS } }
            item { TabChip(stringResource(R.string.tab_achievements), tab == Tab.ACHIEVEMENTS) { tab = Tab.ACHIEVEMENTS } }
            item { TabChip(stringResource(R.string.tab_trade), tab == Tab.TRADE) { tab = Tab.TRADE } }
        }

        when (tab) {
            Tab.COLLECTION, Tab.SHOP -> CardGrid(
                cards = if (tab == Tab.SHOP) cards.filter { !it.isOwned || it.rarity.ordinal >= Rarity.RARE.ordinal } else cards,
                rarityLabel = rarityLabel,
                onClick = { selected = it }
            )
            Tab.PACKS -> PacksTab(
                coins = wallet.coins,
                rarityLabel = rarityLabel,
                onOpen = onOpenPack
            )
            Tab.ACHIEVEMENTS -> AchievementsTab(
                rows = achievements,
                onClaim = onClaimAchievement
            )
            Tab.TRADE -> TradePlaceholder()
        }
    }

    selected?.let { card ->
        CardDetailSheet(
            card = card,
            rarityLabel = rarityLabel(card.rarity),
            wallet = wallet,
            onBuy = { onBuy(card) },
            onSell = { onSell(card) },
            onToggleFavorite = { onToggleFavorite(card) },
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun CardGrid(
    cards: List<Card>,
    rarityLabel: (Rarity) -> String,
    onClick: (Card) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(Tokens.Space4),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space3),
        modifier = Modifier.fillMaxSize()
    ) {
        items(cards, key = { it.characterId }) { card ->
            CollectionCard(
                card = card,
                rarityLabel = rarityLabel(card.rarity),
                modifier = Modifier.pressable(onClick = { onClick(card) })
            )
        }
    }
}

@Composable
private fun TabChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(100))
            .then(
                if (active) Modifier.background(Tokens.AccentGradient)
                else Modifier.background(Tokens.Surface)
            )
            .pressable(onClick = onClick)
            .padding(horizontal = Tokens.Space4, vertical = Tokens.Space2)
    ) {
        Text(
            label,
            color = if (active) Tokens.TextPrimary else Tokens.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CoinBadge(coins: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(Color(0x22FFB020))
            .border(1.dp, Color(0x55FFB020), RoundedCornerShape(100))
            .padding(horizontal = Tokens.Space3, vertical = Tokens.Space2)
    ) {
        Icon(
            Icons.Rounded.Paid,
            contentDescription = stringResource(R.string.coins),
            tint = Color(0xFFFFB020),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(Tokens.Space1))
        Text("$coins", color = Color(0xFFFFB020), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TradePlaceholder() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(Tokens.Space6),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.SwapHoriz,
            contentDescription = null,
            tint = Tokens.Violet,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(Tokens.Space4))
        Text(
            stringResource(R.string.trade_soon_title),
            color = Tokens.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(Tokens.Space2))
        Text(
            stringResource(R.string.trade_soon_body),
            color = Tokens.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun DailyRewardBanner(
    amount: Int,
    streak: Int,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(Color(0x33FFB020), Color(0x11FFB020))
                )
            )
            .border(1.dp, Color(0x44FFB020), RoundedCornerShape(16.dp))
            .padding(start = Tokens.Space4, end = Tokens.Space2, top = Tokens.Space2, bottom = Tokens.Space2)
    ) {
        Icon(
            Icons.Rounded.CardGiftcard,
            contentDescription = null,
            tint = Color(0xFFFFB020),
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(Tokens.Space3))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.daily_title),
                color = Tokens.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (streak > 0) {
                Text(
                    stringResource(R.string.daily_streak, streak),
                    color = Tokens.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .background(Color(0xFFFFB020))
                .pressable(onClick = onClaim)
                .padding(horizontal = Tokens.Space4, vertical = Tokens.Space2)
        ) {
            Text(
                stringResource(R.string.daily_claim, amount),
                color = Color(0xFF1A1206),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

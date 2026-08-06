package com.arroom.characters.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arroom.characters.R
import com.arroom.characters.ui.theme.Tokens
import com.arroom.characters.ui.theme.pressable

/** Состояние достижения для отрисовки строки. */
data class AchievementRow(
    val achievement: Achievement,
    val unlocked: Boolean,
    val claimed: Boolean
)

@Composable
fun AchievementsTab(
    rows: List<AchievementRow>,
    onClaim: (Achievement) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(Tokens.Space4),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space3),
        modifier = Modifier.fillMaxSize()
    ) {
        items(rows, key = { it.achievement.name }) { row ->
            AchievementItem(row, onClaim)
        }
    }
}

@Composable
private fun AchievementItem(row: AchievementRow, onClaim: (Achievement) -> Unit) {
    val a = row.achievement
    val canClaim = row.unlocked && !row.claimed

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tokens.Surface)
            .border(
                1.dp,
                if (canClaim) Color(0x55FFB020) else Tokens.GlassStroke,
                RoundedCornerShape(16.dp)
            )
            .padding(Tokens.Space4)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (row.unlocked) Color(0x22FFB020) else Tokens.Inactive.copy(0.4f)
                )
        ) {
            Icon(
                when {
                    row.claimed -> Icons.Rounded.CheckCircle
                    row.unlocked -> Icons.Rounded.EmojiEvents
                    else -> Icons.Rounded.Lock
                },
                contentDescription = null,
                tint = when {
                    row.claimed -> Tokens.Cyan
                    row.unlocked -> Color(0xFFFFB020)
                    else -> Tokens.TextTertiary
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(Tokens.Space3))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(a.titleRes),
                color = if (row.unlocked) Tokens.TextPrimary else Tokens.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(a.descRes),
                color = Tokens.TextSecondary,
                fontSize = 12.sp
            )
        }
        when {
            canClaim -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFFFFB020))
                    .pressable(onClick = { onClaim(a) })
                    .padding(horizontal = Tokens.Space3, vertical = Tokens.Space2)
            ) {
                Text(
                    stringResource(R.string.ach_claim, a.reward),
                    color = Color(0xFF1A1206),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            row.claimed -> Text(
                stringResource(R.string.ach_claimed),
                color = Tokens.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            else -> Text(
                "+${a.reward}",
                color = Tokens.TextTertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

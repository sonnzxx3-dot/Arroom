package com.arroom.characters.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arroom.characters.R
import com.arroom.characters.ui.theme.Glass
import com.arroom.characters.ui.theme.Tokens
import com.arroom.characters.util.openUrl

/**
 * Ссылка на политику конфиденциальности внутри приложения — рекомендация
 * Google Play, а не просто вежливость: карточку с ней проверяют
 * благосклоннее, и пользователю не нужно искать её в сторе.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    versionName: String,
    cacheSizeMb: Float,
    onClearCache: () -> Unit,
    onReplayCoach: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val policyUrl = stringResource(R.string.privacy_policy_url)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Tokens.Surface,
        scrimColor = Tokens.Scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Tokens.TextPrimary.copy(0.3f)) }
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = Tokens.Space2, end = Tokens.Space2, bottom = Tokens.Space4)
        ) {

            SettingsRow(
                icon = Icons.Rounded.PrivacyTip,
                title = stringResource(R.string.settings_privacy),
                subtitle = null,
                onClick = { openUrl(context, policyUrl) }
            )

            SettingsRow(
                icon = Icons.Rounded.School,
                title = stringResource(R.string.settings_replay_coach),
                subtitle = null,
                onClick = {
                    onReplayCoach()
                    onDismiss()
                }
            )

            SettingsRow(
                icon = Icons.Rounded.CleaningServices,
                title = stringResource(R.string.settings_clear_cache),
                subtitle = stringResource(R.string.settings_cache_size, cacheSizeMb),
                onClick = onClearCache
            )

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_version, versionName),
                color = Color.White.copy(0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(0.8f), modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(0.45f), fontSize = 12.sp)
            }
        }
    }
}

/** Кнопка вызова настроек. Держим её в углу, чтобы не загромождать нижнюю панель. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Glass)
            .clickable(onClick = onClick)
    ) {
        Icon(
            Icons.Rounded.Tune,
            contentDescription = stringResource(R.string.settings_title),
            tint = Color.White.copy(0.85f),
            modifier = Modifier.size(19.dp)
        )
    }
}

/** Показывается один раз после падения. Отправка — только по нажатию. */
@Composable
fun CrashReportDialog(onSend: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_title)) },
        text = { Text(stringResource(R.string.crash_body)) },
        confirmButton = {
            TextButton(onClick = onSend) { Text(stringResource(R.string.crash_send)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.crash_skip)) }
        }
    )
}

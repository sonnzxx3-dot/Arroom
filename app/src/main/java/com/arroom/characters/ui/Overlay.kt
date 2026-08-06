package com.arroom.characters.ui

import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arroom.characters.R
import com.arroom.characters.ar.CharacterNode
import com.arroom.characters.data.CharacterItem
import com.arroom.characters.ui.theme.AccentCyan
import com.arroom.characters.ui.theme.AccentViolet
import com.arroom.characters.ui.theme.GlassIconButton
import com.arroom.characters.ui.theme.GlassSurface
import com.arroom.characters.ui.theme.Glass
import com.arroom.characters.ui.theme.LocalReducedMotion
import com.arroom.characters.ui.theme.Tokens
import com.arroom.characters.ui.theme.pressable
import com.google.ar.core.TrackingFailureReason
import java.io.File

/** Единственная строка-подсказка сверху. Меняется по состоянию трекинга. */
@Composable
fun StatusHint(
    trackingFailure: TrackingFailureReason?,
    planesFound: Boolean,
    hasCharacters: Boolean,
    modifier: Modifier = Modifier
) {
    val text = stringResource(
        when {
            trackingFailure != null -> trackingFailure.hintRes()
            !planesFound -> R.string.hint_move_slowly
            !hasCharacters -> R.string.hint_aim
            else -> R.string.hint_gestures
        }
    )

    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
        label = "hint",
        modifier = modifier
    ) { value ->
        GlassSurface {
            Text(
                value,
                color = Tokens.TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = Tokens.Space5, vertical = Tokens.Space3)
            )
        }
    }
}

@StringRes
private fun TrackingFailureReason.hintRes(): Int = when (this) {
    TrackingFailureReason.NONE -> R.string.hint_ready
    TrackingFailureReason.BAD_STATE -> R.string.hint_bad_state
    TrackingFailureReason.INSUFFICIENT_LIGHT -> R.string.hint_dark
    TrackingFailureReason.EXCESSIVE_MOTION -> R.string.hint_slow_down
    TrackingFailureReason.INSUFFICIENT_FEATURES -> R.string.hint_features
    TrackingFailureReason.CAMERA_UNAVAILABLE -> R.string.hint_camera_busy
    else -> R.string.hint_default
}

@Composable
fun LoadingBubble(progress: Float? = null) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "loader")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(if (reducedMotion) 1f else pulse)
            .clip(Tokens.RadiusLg)
            .background(Tokens.Glass)
            .border(1.dp, Tokens.GlassStroke, Tokens.RadiusLg)
            .padding(horizontal = Tokens.Space6, vertical = Tokens.Space5)
    ) {
        // Определённый прогресс там, где сервер сообщил размер файла,
        // и бесконечная крутилка там, где нет: врать процентами хуже,
        // чем честно показать, что длительность неизвестна.
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress },
                color = Tokens.Violet,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        } else {
            CircularProgressIndicator(
                color = Tokens.Violet,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.loading_model), color = Color.White, fontSize = 13.sp)
        if (progress != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${(progress * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ControlPanel(
    catalog: List<CharacterItem>,
    selected: CharacterItem,
    onSelect: (CharacterItem) -> Unit,
    onImportClick: () -> Unit,
    onLongPressImported: (CharacterItem) -> Unit,
    thumbnailFor: (CharacterItem) -> String?,
    activeCharacter: CharacterNode?,
    onSwitchAnimation: (Int) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClearAll: () -> Unit,
    onCapture: () -> Unit,
    isRecording: Boolean,
    onToggleRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                )
            )
            .padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Переключатель встроенных анимаций выбранного персонажа
        val animCount = activeCharacter?.animationCount ?: 0
        AnimatedVisibility(visible = animCount > 1) {
            AnimationChips(
                count = animCount,
                nameOf = { i -> activeCharacter?.animationNameOrNull(i) },
                onPick = onSwitchAnimation
            )
        }

        Spacer(Modifier.height(10.dp))

        // Карусель персонажей
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ImportTile(onClick = onImportClick)
            }
            items(catalog, key = { it.id }) { item ->
                CharacterTile(
                    item = item,
                    isSelected = item.id == selected.id,
                    thumbnailPath = thumbnailFor(item),
                    onClick = { onSelect(item) },
                    onLongClick = { if (item.isUserImported) onLongPressImported(item) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Нижний ряд действий
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundAction(
                Icons.Rounded.Undo,
                stringResource(R.string.action_undo),
                enabled = canUndo,
                onClick = onUndo
            )
            RoundAction(
                Icons.Rounded.DeleteSweep,
                stringResource(R.string.action_clear),
                enabled = canUndo,
                onClick = onClearAll
            )
            ShutterButton(onClick = onCapture)
            RecordButton(isRecording = isRecording, onClick = onToggleRecord)
        }
    }
}

/**
 * Прицел в центре экрана. Пока ARCore не нашёл поверхность — тусклое
 * вращающееся кольцо; как только точка найдена, кольцо становится ярким
 * и появляется точка центра.
 *
 * Без прицела человек тапает наугад и половину попыток промахивается
 * мимо распознанной плоскости — это самая частая жалоба в AR-приложениях.
 */
@Composable
fun PlacementReticle(ready: Boolean, distanceMeters: Float?) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "reticle")
    val spinRaw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "spin"
    )
    val spin = if (reducedMotion) 0f else spinRaw
    val size by animateFloatAsState(
        targetValue = if (ready) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "reticleSize"
    )
    val color = if (ready) Tokens.Cyan else Tokens.TextPrimary.copy(alpha = 0.45f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(56.dp)
                .scale(size)
                .rotate(if (ready) 0f else spin)
        ) {
            val stroke = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round)
            if (ready) {
                drawCircle(color = color, radius = this.size.minDimension / 2.6f, style = stroke)
                drawCircle(color = color, radius = 3.dp.toPx())
            } else {
                // Четыре дуги вместо сплошного кольца — читается как «идёт поиск»
                repeat(4) { i ->
                    drawArc(
                        color = color,
                        startAngle = i * 90f + 12f,
                        sweepAngle = 46f,
                        useCenter = false,
                        style = stroke
                    )
                }
            }
        }
        AnimatedVisibility(visible = ready && distanceMeters != null) {
            Text(
                text = distanceMeters?.let { stringResource(R.string.distance_meters, it) } ?: "",
                color = Tokens.TextPrimary.copy(alpha = 0.75f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = Tokens.Space2)
            )
        }
    }
}

/** Сессия ARCore умерла — предлагаем перезапуск вместо молчаливого чёрного экрана. */
@Composable
fun SessionErrorOverlay(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Tokens.Ink.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Tokens.Space6 + Tokens.Space2)
        ) {
            Icon(
                Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = Tokens.Violet,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.session_error_title),
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = Tokens.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Tokens.Violet)
            ) {
                Text(stringResource(R.string.session_error_retry), color = Color.White)
            }
        }
    }
}

/**
 * Появляется на несколько секунд после съёмки. Сохранение в галерею —
 * ещё не публикация; без этой кнопки человек уходит в другое приложение
 * искать файл вручную, и половина не возвращается.
 */
@Composable
fun SharePrompt(onShare: () -> Unit) {
    val shareLabel = stringResource(R.string.action_share)
    GlassSurface {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Tokens.Space4, end = Tokens.Space1, top = Tokens.Space1, bottom = Tokens.Space1)
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Tokens.Success,
                modifier = Modifier.size(Tokens.IconSm)
            )
            Spacer(Modifier.width(Tokens.Space2))
            Text(stringResource(R.string.share_ready), color = Tokens.TextPrimary, fontSize = 12.sp)
            Spacer(Modifier.width(Tokens.Space3))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(Tokens.RadiusPill)
                    .background(Tokens.AccentGradient)
                    .semantics { contentDescription = shareLabel; role = Role.Button }
                    .pressable(onClick = onShare)
                    .padding(horizontal = Tokens.Space4, vertical = Tokens.Space2)
            ) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = Tokens.TextPrimary,
                    modifier = Modifier.size(Tokens.IconSm)
                )
                Spacer(Modifier.width(Tokens.Space2))
                Text(stringResource(R.string.action_share), color = Tokens.TextPrimary, fontSize = 12.sp)
            }
        }
    }
}

/** Возврат панели, когда она спрятана на время записи. */
@Composable
fun ShowUiButton(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(Glass)
            .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(100))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.show_panel), color = Color.White, fontSize = 12.sp)
    }
}

/** Красная точка «REC» с пульсацией — стандартный сигнал, что идёт запись. */
@Composable
fun RecordingBadge() {
    val transition = rememberInfiniteTransition(label = "rec")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "recAlpha"
    )
    GlassSurface {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Tokens.Space3, vertical = Tokens.Space2)
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Tokens.Recording.copy(alpha = alpha))
            )
            Spacer(Modifier.width(Tokens.Space2))
            Text(
                stringResource(R.string.badge_recording),
                color = Tokens.TextPrimary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    val size by animateFloatAsState(if (isRecording) 0.55f else 1f, label = "recShape")
    val label = stringResource(R.string.action_record)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .semantics { contentDescription = label; role = Role.Button }
            .size(Tokens.TouchMin)
            .clip(CircleShape)
            .background(Tokens.Glass)
            .border(1.dp, Tokens.GlassStroke, CircleShape)
            .pressable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(22.dp * size)
                .clip(if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                .background(Tokens.Recording)
        )
    }
}

@Composable
private fun AnimationChips(
    count: Int,
    nameOf: (Int) -> String?,
    onPick: (Int) -> Unit
) {
    var current by remember { mutableIntStateOf(0) }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items((0 until count).toList()) { index ->
            val active = index == current
            Box(
                Modifier
                    .clip(Tokens.RadiusPill)
                    .then(
                        if (active) Modifier.background(Tokens.AccentGradient)
                        else Modifier.background(Tokens.Glass).border(1.dp, Tokens.GlassStroke, Tokens.RadiusPill)
                    )
                    .pressable(onClick = { current = index; onPick(index) })
                    .padding(horizontal = Tokens.Space4, vertical = Tokens.Space2)
            ) {
                Text(
                    nameOf(index) ?: stringResource(R.string.animation_index, index + 1),
                    color = Tokens.TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterTile(
    item: CharacterItem,
    isSelected: Boolean,
    thumbnailPath: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1f else 0.92f, label = "tileScale")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Glass)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    brush = if (isSelected)
                        Tokens.AccentGradient
                    else Brush.linearGradient(
                        listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            if (thumbnailPath != null) {
                AsyncImage(
                    model = File(thumbnailPath),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                )
            } else {
                // До первого размещения превью нет — даём хотя бы стабильный
                // цвет из имени, чтобы свои модели не сливались в один кубик
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(tintFor(item.id).copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (item.isUserImported) Icons.Rounded.Category else Icons.Rounded.Face,
                        contentDescription = item.title,
                        tint = if (isSelected) Color.White else Color.White.copy(0.75f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            color = if (isSelected) Tokens.TextPrimary else Tokens.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(68.dp),
            textAlign = TextAlign.Center
        )
    }
}

/** Стабильный оттенок из идентификатора: одна и та же модель всегда одного цвета. */
private fun tintFor(id: String): Color {
    val hue = ((id.hashCode() % 360) + 360) % 360
    return Color.hsv(hue.toFloat(), 0.55f, 0.95f)
}

@Composable
private fun ImportTile(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Tokens.AccentGradient
                )
                .clickable(onClick = onClick)
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.action_import),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.action_import),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(68.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    GlassIconButton(onClick = onClick, contentDescription = label, enabled = enabled) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Tokens.IconMd))
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val label = stringResource(R.string.action_photo)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(Tokens.Shutter)
            .clip(CircleShape)
            .background(Tokens.AccentGradient)
            .semantics { contentDescription = label; role = Role.Button }
            .pressable(onClick = onClick, pressScale = 0.9f)
    ) {
        Icon(
            Icons.Rounded.PhotoCamera,
            contentDescription = null,
            tint = Tokens.TextPrimary,
            modifier = Modifier.size(30.dp)
        )
    }
}

/** Кнопка входа в коллекцию с бейджем монет. Ставится в шапке AR-экрана. */
@Composable
fun CollectionButton(coins: Int, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.pressable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Tokens.Space3, end = Tokens.Space3, top = 9.dp, bottom = 9.dp)
        ) {
            Icon(
                Icons.Rounded.Style,
                contentDescription = stringResource(R.string.collection_open),
                tint = Tokens.TextPrimary,
                modifier = Modifier.size(Tokens.IconSm)
            )
            Spacer(Modifier.width(Tokens.Space2))
            Icon(
                Icons.Rounded.Paid,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFFFFB020),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                "$coins",
                color = androidx.compose.ui.graphics.Color(0xFFFFB020),
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

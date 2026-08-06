package com.arroom.characters.ui

import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arroom.characters.R
import com.arroom.characters.ui.theme.AccentCyan
import com.arroom.characters.ui.theme.Tokens
import com.arroom.characters.ui.theme.AccentViolet

private data class Page(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int
)

private val pages = listOf(
    Page(Icons.Rounded.ViewInAr, R.string.onb1_title, R.string.onb1_body),
    Page(Icons.Rounded.UploadFile, R.string.onb2_title, R.string.onb2_body),
    Page(Icons.Rounded.Videocam, R.string.onb3_title, R.string.onb3_body)
)

/**
 * Без онбординга человек видит пустую камеру, не понимает, что делать,
 * и выходит. Три экрана поднимают конверсию в первое размещение персонажа
 * сильнее, чем любая другая правка интерфейса.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDone) {
                Text(
                    stringResource(R.string.onb_skip),
                    color = Tokens.TextTertiary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn(tween(280)))
                    .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(180)))
            },
            label = "page"
        ) { i ->
            val p = pages[i]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PulsingIcon(p.icon)
                Spacer(Modifier.height(34.dp))
                Text(
                    stringResource(p.title),
                    color = Tokens.TextPrimary,
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 31.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(p.body),
                    color = Tokens.TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { i ->
                val active = i == index
                val width by animateDpAsState(if (active) 22.dp else 7.dp, label = "dot")
                Box(
                    Modifier
                        .height(7.dp)
                        .width(width)
                        .clip(RoundedCornerShape(100))
                        .background(if (active) Tokens.Violet else Tokens.Inactive)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { if (index == pages.lastIndex) onDone() else index++ },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Tokens.Violet)
        ) {
            Text(
                stringResource(
                    if (index == pages.lastIndex) R.string.onb_open_camera else R.string.onb_next
                ),
                fontSize = 16.sp,
                color = Tokens.TextPrimary
            )
        }
    }
}

@Composable
private fun PulsingIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "iconPulse")
    val s by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(116.dp)
            .scale(s)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(AccentViolet.copy(alpha = 0.28f), AccentCyan.copy(alpha = 0.16f))
                )
            )
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
    }
}

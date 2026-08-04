package com.arroom.characters.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arroom.characters.R
import com.arroom.characters.ui.theme.AccentCyan
import com.arroom.characters.ui.theme.AccentViolet
import com.arroom.characters.ui.theme.Glass
import kotlin.math.sin

/**
 * Показывается один раз — после того, как человек поставил первого персонажа.
 *
 * Момент выбран не случайно: до размещения жесты не к чему применить,
 * и подсказка воспринимается как шум. Сразу после — есть объект на экране,
 * и человек пробует прямо на нём.
 */
@Composable
fun GestureCoach(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAA07070C))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Glass)
                .padding(horizontal = 26.dp, vertical = 28.dp)
        ) {
            Text(
                stringResource(R.string.coach_title),
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(22.dp))

            GestureRow(GestureDemo.DRAG, stringResource(R.string.coach_drag))
            Spacer(Modifier.height(18.dp))
            GestureRow(GestureDemo.PINCH, stringResource(R.string.coach_pinch))
            Spacer(Modifier.height(18.dp))
            GestureRow(GestureDemo.ROTATE, stringResource(R.string.coach_rotate))

            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.coach_got_it), color = Color.White)
            }
        }
    }
}

private enum class GestureDemo { DRAG, PINCH, ROTATE }

@Composable
private fun GestureRow(demo: GestureDemo, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GestureAnimation(demo)
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color(0xFFC9C9D6), fontSize = 14.sp)
    }
}

/**
 * Кружки-«пальцы» вместо иконки руки: анимация читается мгновенно
 * и не требует ни картинок в ресурсах, ни библиотеки Lottie.
 */
@Composable
private fun GestureAnimation(demo: GestureDemo) {
    val transition = rememberInfiniteTransition(label = "gesture")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1700, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "t"
    )

    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 8f
            val wave = sin(t * 2f * Math.PI).toFloat()

            when (demo) {
                GestureDemo.DRAG -> {
                    drawCircle(AccentCyan, r, c + Offset(wave * size.width / 5f, 0f))
                }

                GestureDemo.PINCH -> {
                    val gap = size.width / 5f * (0.45f + 0.55f * ((wave + 1f) / 2f))
                    drawCircle(AccentCyan, r, c + Offset(-gap, -gap * 0.5f))
                    drawCircle(AccentCyan, r, c + Offset(gap, gap * 0.5f))
                }

                GestureDemo.ROTATE -> {
                    val angle = t * 2f * Math.PI.toFloat()
                    val d = size.width / 5f
                    drawCircle(
                        AccentCyan, r,
                        c + Offset(
                            (kotlin.math.cos(angle) * d),
                            (kotlin.math.sin(angle) * d)
                        )
                    )
                    drawCircle(
                        AccentCyan, r,
                        c - Offset(
                            (kotlin.math.cos(angle) * d),
                            (kotlin.math.sin(angle) * d)
                        )
                    )
                }
            }
        }
    }
}

package com.arroom.characters.ar

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext

enum class ThermalLevel { NORMAL, WARM, HOT }

/**
 * AR — один из самых тяжёлых сценариев для телефона: камера, нейросетевой
 * трекинг и 3D-рендер работают одновременно. Через несколько минут съёмки
 * устройство начинает троттлить, FPS падает, ARCore теряет трекинг, и со
 * стороны это выглядит как «приложение сломалось».
 *
 * Лучше самим снизить нагрузку заранее, чем ждать, пока это сделает система
 * в самый неподходящий момент — посреди записи видео.
 *
 * До Android 10 API теплового состояния нет, там всегда NORMAL.
 */
@Composable
fun rememberThermalLevel(): State<ThermalLevel> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(ThermalLevel.NORMAL) }

    DisposableEffect(context) {
        if (Build.VERSION.SDK_INT < 29) return@DisposableEffect onDispose { }

        val power = context.getSystemService(PowerManager::class.java)
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            state.value = status.toLevel()
        }

        runCatching {
            state.value = power.currentThermalStatus.toLevel()
            power.addThermalStatusListener(listener)
        }

        onDispose {
            runCatching { power.removeThermalStatusListener(listener) }
        }
    }

    return state
}

private fun Int.toLevel(): ThermalLevel = when {
    this >= PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.HOT
    this >= PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.WARM
    else -> ThermalLevel.NORMAL
}

/** Сколько персонажей разумно держать в сцене при текущей температуре. */
fun ThermalLevel.characterLimit(default: Int): Int = when (this) {
    ThermalLevel.NORMAL -> default
    ThermalLevel.WARM -> minOf(default, 3)
    ThermalLevel.HOT -> 1
}

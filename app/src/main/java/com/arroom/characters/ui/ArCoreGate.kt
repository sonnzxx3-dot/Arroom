package com.arroom.characters.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arroom.characters.R
import com.arroom.characters.ui.theme.Tokens
import com.google.ar.core.ArCoreApk

private enum class ArState { CHECKING, READY, INSTALLING, UNSUPPORTED }

/**
 * Без этой проверки приложение просто падает на устройствах без ARCore
 * либо с устаревшим Google Play Services for AR — самая частая причина
 * однозвёздочных отзывов у AR-приложений.
 */
@Composable
fun ArCoreGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var state by remember { mutableStateOf(ArState.CHECKING) }
    var userRequestedInstall by remember { mutableStateOf(true) }

    fun check() {
        if (activity == null) { state = ArState.UNSUPPORTED; return }
        when (ArCoreApk.getInstance().checkAvailability(context)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> state = ArState.READY

            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                state = ArState.INSTALLING
                runCatching {
                    val result = ArCoreApk.getInstance()
                        .requestInstall(activity, userRequestedInstall)
                    when (result) {
                        ArCoreApk.InstallStatus.INSTALLED -> state = ArState.READY
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED ->
                            userRequestedInstall = false
                        else -> Unit
                    }
                }.onFailure { state = ArState.UNSUPPORTED }
            }

            ArCoreApk.Availability.UNKNOWN_CHECKING -> state = ArState.CHECKING
            else -> state = ArState.UNSUPPORTED
        }
    }

    // Проверяем при каждом возврате в приложение: пользователь мог
    // как раз доустановить ARCore из Play Маркета.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) check()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // UNKNOWN_CHECKING приходит асинхронно — опрашиваем ещё раз
    LaunchedEffect(state) {
        if (state == ArState.CHECKING) {
            kotlinx.coroutines.delay(220)
            check()
        }
    }

    when (state) {
        ArState.READY -> content()

        ArState.UNSUPPORTED -> FullScreenMessage(
            title = stringResource(R.string.ar_unsupported_title),
            body = stringResource(R.string.ar_unsupported_body)
        )

        else -> Box(
            Modifier.fillMaxSize().background(Tokens.Ink),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FullScreenMessage(title: String, body: String) {
    Box(
        Modifier.fillMaxSize().background(Tokens.Ink),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = Tokens.Violet,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Tokens.TextPrimary)
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = Tokens.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

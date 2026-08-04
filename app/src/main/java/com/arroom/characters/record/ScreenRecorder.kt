package com.arroom.characters.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arroom.characters.R
import kotlinx.coroutines.flow.collectLatest

class ScreenRecorderController internal constructor(
    val isRecording: Boolean,
    val toggle: () -> Unit
)

/**
 * Оборачивает три асинхронных шага (уведомления -> микрофон -> согласие
 * на захват экрана) в одну кнопку. Отказ от микрофона не блокирует запись —
 * просто получится видео без звука.
 */
@Composable
fun rememberScreenRecorder(onEvent: (RecEvent) -> Unit): ScreenRecorderController {
    val context = LocalContext.current
    val recording by RecorderService.isRecording.collectAsState()
    var withAudio by remember { mutableStateOf(false) }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            RecorderService.start(context, result.resultCode, data, withAudio)
        } else {
            onEvent(RecEvent.Failed(context.getString(R.string.rec_cancelled)))
        }
    }

    fun askProjection() {
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        withAudio = granted
        askProjection()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> requestAudioThenRecord(context, audioLauncher::launch) { withAudio = it; askProjection() } }

    LaunchedEffect(Unit) {
        RecorderService.events.collectLatest(onEvent)
    }

    return ScreenRecorderController(
        isRecording = recording,
        toggle = {
            if (recording) {
                RecorderService.stop(context)
            } else if (Build.VERSION.SDK_INT >= 33 &&
                !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                // Без уведомления нельзя запустить foreground-сервис на Android 13+
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestAudioThenRecord(context, audioLauncher::launch) {
                    withAudio = it; askProjection()
                }
            }
        }
    )
}

private fun requestAudioThenRecord(
    context: Context,
    requestAudio: (String) -> Unit,
    proceed: (Boolean) -> Unit
) {
    if (context.hasPermission(Manifest.permission.RECORD_AUDIO)) proceed(true)
    else requestAudio(Manifest.permission.RECORD_AUDIO)
}

private fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

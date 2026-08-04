package com.arroom.characters.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.arroom.characters.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

sealed interface RecEvent {
    data class Saved(val uri: String) : RecEvent
    data class Failed(val reason: String) : RecEvent
}

/**
 * Запись экрана через MediaProjection.
 *
 * Почему не рендер прямо из Filament: AR-кадр складывается из видеопотока
 * камеры и 3D-слоя уже на уровне композитора. Захват экрана — единственный
 * способ получить ровно ту картинку, которую видит пользователь, без
 * дублирования всего пайплайна рендера.
 *
 * Android 14+ требует, чтобы foreground-сервис с типом mediaProjection
 * стартовал ДО вызова getMediaProjection() — поэтому вся логика здесь.
 */
class RecorderService : Service() {

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopRecording()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundCompat()
                val code = intent.getIntExtra(EXTRA_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= 33)
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                else
                    @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)

                if (data == null) {
                    fail(getString(R.string.rec_no_permission))
                } else {
                    startRecording(code, data, intent.getBooleanExtra(EXTRA_AUDIO, false))
                }
            }

            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.rec_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(getString(R.string.rec_notif_title))
            .setContentText(getString(R.string.rec_notif_text))
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startRecording(resultCode: Int, data: Intent, withAudio: Boolean) {
        runCatching {
            val metrics = resources.displayMetrics
            // Ограничиваем 1080p по ширине: выше — лишний нагрев без выигрыша в качестве
            val scale = if (metrics.widthPixels > 1080) 1080f / metrics.widthPixels else 1f
            val width = (metrics.widthPixels * scale).toInt() and 1.inv()
            val height = (metrics.heightPixels * scale).toInt() and 1.inv()

            val file = File(cacheDir, "arroom_${System.currentTimeMillis()}.mp4")
            outputFile = file

            recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this)
            else @Suppress("DEPRECATION") MediaRecorder()).apply {
                if (withAudio) setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(file.absolutePath)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (withAudio) setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(width * height * 5)
                prepare()
            }

            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, data)?.also {
                it.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
            } ?: throw IllegalStateException("MediaProjection недоступна")

            virtualDisplay = projection!!.createVirtualDisplay(
                "ARRoomCapture",
                width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder!!.surface, null, null
            )

            recorder!!.start()
            isRecording.value = true
        }.onFailure {
            fail(getString(R.string.rec_start_failed))
        }
    }

    private fun stopRecording() {
        if (!isRecording.value) {
            cleanup(); stopSelf(); return
        }
        isRecording.value = false

        // stop() бросает исключение, если записано меньше ~1 секунды
        val ok = runCatching { recorder?.stop() }.isSuccess
        cleanup()

        val file = outputFile
        if (ok && file != null && file.length() > 0) {
            val uri = exportToGallery(file)
            events.tryEmit(
                if (uri != null) RecEvent.Saved(uri) else RecEvent.Failed(getString(R.string.rec_save_failed))
            )
        } else {
            events.tryEmit(RecEvent.Failed(getString(R.string.rec_too_short)))
        }
        file?.delete()
        outputFile = null
        stopSelf()
    }

    private fun exportToGallery(file: File): String? = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "ARRoom_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AR Room")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        uri.toString()
    }.getOrNull()

    private fun cleanup() {
        runCatching { virtualDisplay?.release() }
        runCatching { recorder?.reset(); recorder?.release() }
        runCatching { projection?.unregisterCallback(projectionCallback); projection?.stop() }
        virtualDisplay = null
        recorder = null
        projection = null
    }

    private fun fail(reason: String) {
        isRecording.value = false
        events.tryEmit(RecEvent.Failed(reason))
        cleanup()
        stopSelf()
    }

    override fun onDestroy() {
        cleanup()
        isRecording.value = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.arroom.characters.START_REC"
        const val ACTION_STOP = "com.arroom.characters.STOP_REC"
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        const val EXTRA_AUDIO = "audio"
        private const val CHANNEL = "rec_channel"
        private const val NOTIF_ID = 1337

        val isRecording = MutableStateFlow(false)
        val events = MutableSharedFlow<RecEvent>(extraBufferCapacity = 4)

        fun start(context: Context, resultCode: Int, data: Intent, withAudio: Boolean) {
            val intent = Intent(context, RecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
                putExtra(EXTRA_AUDIO, withAudio)
            }
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, RecorderService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}

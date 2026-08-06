package com.arroom.characters.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Локальные отчёты о падениях.
 *
 * Firebase Crashlytics требует проекта в Google Cloud и файла
 * google-services.json — лишняя зависимость на старте. Этот вариант
 * работает без единого внешнего сервиса: стектрейс пишется в файл,
 * при следующем запуске приложение предлагает отправить его почтой.
 *
 * Отчёт уходит только по явному нажатию пользователя. Ничего не
 * передаётся автоматически, поэтому декларацию Data safety менять не нужно.
 */
object CrashLogger {

    private const val DIR = "crashes"
    private const val MAX_KEPT = 3

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(appContext, thread, throwable) }
            // Обязательно передаём дальше: иначе процесс зависнет
            // вместо честного падения, и Android не покажет диалог
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Текст последнего необработанного падения или null. */
    fun pendingReport(context: Context): String? =
        dir(context).listFiles()
            ?.filter { it.isFile }
            ?.maxByOrNull { it.lastModified() }
            ?.takeIf { it.length() > 0 }
            ?.readText()

    fun clear(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stack = StringWriter().also { sw ->
            PrintWriter(sw).use { throwable.printStackTrace(it) }
        }.toString()

        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"

        val report = buildString {
            appendLine("AR Room crash report")
            appendLine("time: $stamp")
            appendLine("app version: $version")
            appendLine("android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("abi: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("thread: ${thread.name}")
            appendLine()
            append(stack)
        }

        val target = dir(context)
        File(target, "crash_${System.currentTimeMillis()}.txt").writeText(report)

        // Оставляем только несколько последних, чтобы папка не росла бесконечно
        target.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_KEPT)
            ?.forEach { it.delete() }
    }

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }
}

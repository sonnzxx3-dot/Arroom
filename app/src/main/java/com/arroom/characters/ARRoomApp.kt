package com.arroom.characters

import android.app.Application
import com.arroom.characters.util.CrashLogger

class ARRoomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ставим обработчик как можно раньше: падения при инициализации
        // Filament случаются до создания активности
        CrashLogger.install(this)
    }
}

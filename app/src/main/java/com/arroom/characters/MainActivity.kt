package com.arroom.characters

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.arroom.characters.data.AppPrefs
import com.arroom.characters.ui.ArCoreGate
import com.arroom.characters.ui.ArScreen
import com.arroom.characters.ui.CameraPermissionGate
import com.arroom.characters.ui.CrashReportDialog
import com.arroom.characters.ui.OnboardingScreen
import com.arroom.characters.ui.theme.ARRoomTheme
import com.arroom.characters.util.CrashLogger
import com.arroom.characters.util.sendCrashReport

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Обязательно до super.onCreate — иначе системный сплэш
        // не подхватится и мелькнёт чёрный кадр
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // AR-сессия без касаний экрана — иначе телефон гаснет посреди съёмки
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ARRoomTheme {
                val prefs = remember { AppPrefs(this) }
                var onboarded by remember { mutableStateOf(prefs.onboardingDone) }
                var crashReport by remember { mutableStateOf(CrashLogger.pendingReport(this)) }

                crashReport?.let { report ->
                    CrashReportDialog(
                        onSend = {
                            sendCrashReport(this, report)
                            CrashLogger.clear(this)
                            crashReport = null
                        },
                        onDismiss = {
                            CrashLogger.clear(this)
                            crashReport = null
                        }
                    )
                }

                AnimatedContent(
                    targetState = onboarded,
                    transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
                    label = "root"
                ) { done ->
                    if (!done) {
                        OnboardingScreen(onDone = {
                            prefs.onboardingDone = true
                            onboarded = true
                        })
                    } else {
                        ArCoreGate {
                            CameraPermissionGate {
                                ArScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

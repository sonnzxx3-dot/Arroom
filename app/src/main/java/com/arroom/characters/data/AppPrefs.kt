package com.arroom.characters.data

import android.content.Context

/**
 * Мелкие локальные настройки. DataStore здесь избыточен — одно булево значение.
 */
class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    /** Подсказку про жесты показываем один раз за всё время. */
    var gestureCoachShown: Boolean
        get() = prefs.getBoolean(KEY_COACH, false)
        set(value) = prefs.edit().putBoolean(KEY_COACH, value).apply()

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_COACH = "gesture_coach_shown"
    }
}

object Limits {
    /**
     * Больше 5 анимированных персонажей телефон средней ценовой категории
     * не тянет: FPS падает ниже 30, корпус греется, ARCore теряет трекинг.
     */
    const val MAX_CHARACTERS = 5
}

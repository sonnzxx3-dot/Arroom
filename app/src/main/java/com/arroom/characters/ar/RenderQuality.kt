package com.arroom.characters.ar

import com.google.android.filament.View

object RenderQuality {

    fun apply(view: View, highEnd: Boolean = true) {
        runCatching {
            view.antiAliasing = View.AntiAliasing.FXAA
        }

        runCatching {
            view.dynamicResolutionOptions = View.DynamicResolutionOptions().apply {
                enabled = true
                quality = if (highEnd) View.QualityLevel.MEDIUM else View.QualityLevel.LOW
                minScale = if (highEnd) 0.6f else 0.45f
                maxScale = 1.0f
            }
        }
    }
}

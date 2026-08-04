package com.arroom.characters.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.arroom.characters.R

/**
 * Основной сценарий приложения заканчивается не сохранением в галерею,
 * а публикацией. Каждый лишний шаг между «снял» и «выложил» съедает
 * заметную долю тех, кто дошёл до конца.
 *
 * Uri берутся из MediaStore, поэтому FileProvider не нужен —
 * они уже доступны другим приложениям.
 */
fun shareMedia(context: Context, uri: Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_chooser))
        )
    }
}

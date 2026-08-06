package com.arroom.characters.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.arroom.characters.R

/** Открывает почтовый клиент с готовым отчётом. Ничего не отправляется само. */
fun sendCrashReport(context: Context, report: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.support_email)))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_mail_subject))
        putExtra(Intent.EXTRA_TEXT, report)
    }
    runCatching { context.startActivity(intent) }
}

/** Открывает ссылку во внешнем браузере. */
fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

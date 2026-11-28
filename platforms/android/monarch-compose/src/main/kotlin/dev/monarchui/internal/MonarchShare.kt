package dev.monarchui.internal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Opens the system share sheet with [text]: the platform way to send, save or
 * print text from a component (recovery codes, install instructions, a link).
 *
 * @param title the title of the sheet and of the shared text, or `null` for the system's.
 * @return `false` when no app can receive text; keep another way out, such as a copy button.
 */
internal fun monarchShareText(context: Context, text: String, title: String? = null): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (title != null) putExtra(Intent.EXTRA_TITLE, title)
    }
    return try {
        context.startActivity(Intent.createChooser(send, title))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

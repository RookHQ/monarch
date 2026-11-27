package com.rookhq.monarch.components.actions.copybutton

import android.content.ClipData
import android.content.ClipDescription
import android.os.PersistableBundle
import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.theme.MonarchColors

/** Component tokens and the pure rules of MonarchCopyButton. */
internal object MonarchCopyButtonTokens {
    /**
     * `ClipDescription.EXTRA_IS_SENSITIVE`, written out because the constant
     * exists from Android 13 only; earlier versions ignore the extra.
     */
    const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    /** How long the copied state lasts by default, in milliseconds. */
    const val DEFAULT_RESET_MILLIS = 2000L

    /** The plain-text clip that is written. A sensitive clip is hidden in the system clipboard preview. */
    fun clip(label: String, value: String, sensitive: Boolean): ClipData =
        ClipData.newPlainText(label, value).apply {
            if (sensitive) {
                description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
            }
        }

    /** The colour of the icon-only button: no fill, the default text colour, the disabled role when disabled. */
    fun iconColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** Whether the copied state is shown: only after a copy, and only when the confirmation is shown in the app. */
    fun showsCopied(copied: Boolean, showConfirmation: Boolean): Boolean = copied && showConfirmation
}

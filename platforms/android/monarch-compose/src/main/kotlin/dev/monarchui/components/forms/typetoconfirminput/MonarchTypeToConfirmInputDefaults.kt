package dev.monarchui.components.forms.typetoconfirminput

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization

/** The matching rule and the fixed keyboard options of `MonarchTypeToConfirmInput`. */
public object MonarchTypeToConfirmInputDefaults {
    /** No autocorrection and no capitalisation, so the keyboard does not change the typed name. */
    public val KeyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
    )

    /**
     * Whether [text] matches [phrase]: case-sensitive, ignoring white space
     * around both. The same rule the web's `phraseMatches` uses.
     */
    public fun matches(text: String, phrase: String): Boolean = text.trim() == phrase.trim()

    /** Whether the error is shown: only when asked for and the text does not match. */
    internal fun showsError(matches: Boolean, showMismatch: Boolean): Boolean = showMismatch && !matches
}

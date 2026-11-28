package dev.monarchui.components.forms.inlineedittext

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchInlineEditText`. Every
 * `null` keeps the built-in string `monarch_inlineedittext_<key>`.
 *
 * @property editHint what TalkBack says a double tap on the text does ("Edit").
 * @property save the save button ("Save").
 * @property cancel the cancel button ("Cancel").
 * @property saving the state of the save button while saving ("Saving").
 * @property saveFailed the message of a failed save whose error has no message ("Couldn't save. Try again.").
 */
@Immutable
public class MonarchInlineEditTextLabels(
    public val editHint: String? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val saving: String? = null,
    public val saveFailed: String? = null,
)

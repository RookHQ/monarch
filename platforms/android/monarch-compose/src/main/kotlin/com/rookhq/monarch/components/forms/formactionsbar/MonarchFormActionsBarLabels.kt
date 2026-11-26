package com.rookhq.monarch.components.forms.formactionsbar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchFormActionsBar`. Every
 * `null` uses the built-in string named after it in `strings_formactionsbar.xml`.
 *
 * @property unsaved shown while there are unsaved changes: "You have unsaved changes".
 * @property save the save button: "Save changes".
 * @property saving the state of the save button while the save runs: "Saving".
 * @property discard the discard button: "Discard".
 * @property discardTitle the title of the discard confirmation: "Discard unsaved changes?".
 * @property discardDescription the text of the discard confirmation: "Your changes will be lost. This
 * cannot be undone.".
 * @property discardConfirm the confirming button of the discard confirmation: "Discard changes".
 * @property keepEditing the cancelling button of the discard confirmation: "Keep editing".
 */
@Immutable
public class MonarchFormActionsBarLabels(
    public val unsaved: String? = null,
    public val save: String? = null,
    public val saving: String? = null,
    public val discard: String? = null,
    public val discardTitle: String? = null,
    public val discardDescription: String? = null,
    public val discardConfirm: String? = null,
    public val keepEditing: String? = null,
)

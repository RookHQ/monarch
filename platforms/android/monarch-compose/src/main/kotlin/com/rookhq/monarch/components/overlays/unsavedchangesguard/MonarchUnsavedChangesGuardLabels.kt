package com.rookhq.monarch.components.overlays.unsavedchangesguard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchUnsavedChangesGuard`.
 * Every `null` keeps the built-in string `monarch_unsavedchangesguard_<key>`.
 *
 * @property title the question, "Discard unsaved changes?".
 * @property message what happens on leaving, "Your changes have not been saved. If you leave now, they will be
 * lost."
 * @property keepEditing the safe choice, "Keep editing".
 * @property discard the destructive choice, "Discard changes".
 * @property save the choice that saves first, "Save and leave". Shown only with an `onSave`.
 * @property saveFailed shown and announced when the save failed, "Your changes could not be saved. Try again."
 */
@Immutable
public class MonarchUnsavedChangesGuardLabels(
    public val title: String? = null,
    public val message: String? = null,
    public val keepEditing: String? = null,
    public val discard: String? = null,
    public val save: String? = null,
    public val saveFailed: String? = null,
)

package dev.monarchui.components.feedback.consequencelist

import androidx.compose.runtime.Immutable

/**
 * One consequence of an action, as shown by `MonarchConsequenceList`.
 *
 * @property kind what happens: removed, changed, a warning, or kept.
 * @property label what happens, in plain words, for example "tasks will be deleted".
 * @property detail a second, quieter line with specifics.
 * @property count how many things are affected. Shown before the label, formatted for the locale.
 * @property id a stable identity. The position in the list is used when it is `null`.
 */
@Immutable
public class MonarchConsequence(
    public val kind: MonarchConsequenceKind,
    public val label: String,
    public val detail: String? = null,
    public val count: Int? = null,
    public val id: String? = null,
)

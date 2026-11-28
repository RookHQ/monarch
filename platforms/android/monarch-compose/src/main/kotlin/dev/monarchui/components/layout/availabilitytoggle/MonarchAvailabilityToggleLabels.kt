package dev.monarchui.components.layout.availabilitytoggle

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAvailabilityToggle`.
 * Every `null` keeps the built-in string `monarch_availabilitytoggle_<key>`.
 *
 * @property label the name TalkBack reads for the toggle, "Available for new work".
 * @property available the visible status word while available, "Available". TalkBack reads it as the state.
 * @property away the visible status word while away, "Away". TalkBack reads it as the state.
 * @property availableHint the explanation shown with `showHint` while available.
 * @property awayHint the explanation shown with `showHint` while away.
 * @property pending what TalkBack adds to the state while a change is saved, "Saving".
 */
@Immutable
public class MonarchAvailabilityToggleLabels(
    public val label: String? = null,
    public val available: String? = null,
    public val away: String? = null,
    public val availableHint: String? = null,
    public val awayHint: String? = null,
    public val pending: String? = null,
)

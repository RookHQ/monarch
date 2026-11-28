package dev.monarchui.components.datadisplay.statusbadge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.monarchui.foundation.MonarchTone

/**
 * One status a `MonarchStatusBadge` can show: the words, the tone and an
 * optional icon.
 *
 * The label is always shown, so the tone is never the only signal. Give each
 * status of a set a distinct icon where you can, because the shape then
 * separates statuses of the same tone.
 *
 * @property label the words shown in the badge, already localised: "Partial outage", never the raw key.
 * @property tone the colour family of the badge.
 * @property icon a decorative icon before the label, for example a `MonarchIcon` of size `Sm` with a `null`
 * content description. Monarch bundles no icons, so the built-in statuses have none.
 * @property description one line that explains the status, for a legend the app draws. Not shown by the badge.
 */
@Immutable
public class MonarchStatusDefinition(
    public val label: String,
    public val tone: MonarchTone,
    public val icon: (@Composable () -> Unit)? = null,
    public val description: String? = null,
)

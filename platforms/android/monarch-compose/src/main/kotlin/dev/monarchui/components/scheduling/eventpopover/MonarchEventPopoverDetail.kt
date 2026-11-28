package dev.monarchui.components.scheduling.eventpopover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One labelled row of a `MonarchEventPopover`, such as the hosts, the place
 * or a reference.
 *
 * @property label what the row is, for example "Location". Shown, and read with the value.
 * @property value the value, already localised.
 * @property icon a decorative icon before the value, for example a `MonarchIcon` with a `null` content
 * description. Monarch bundles no icons.
 */
@Immutable
public class MonarchEventPopoverDetail(
    public val label: String,
    public val value: String,
    public val icon: (@Composable () -> Unit)? = null,
)

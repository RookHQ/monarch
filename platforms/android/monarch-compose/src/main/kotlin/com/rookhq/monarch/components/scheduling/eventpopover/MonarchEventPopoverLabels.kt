package com.rookhq.monarch.components.scheduling.eventpopover

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus

/**
 * Replacements for the built-in strings of a `MonarchEventPopover`. Every
 * `null` uses the built-in string in `strings_eventpopover.xml`.
 *
 * @property time the name TalkBack reads before the date and time: "Time".
 * @property allDay shown instead of times for an all-day event: "All day".
 * @property status the name of a status in its badge: "Confirmed", "Tentative", "Cancelled", "Declined".
 */
@Immutable
public class MonarchEventPopoverLabels(
    public val time: String? = null,
    public val allDay: String? = null,
    public val status: ((MonarchEventChipStatus) -> String)? = null,
)

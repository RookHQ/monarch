package dev.monarchui.components.scheduling.timegrid

import androidx.compose.runtime.Immutable

/**
 * A block of time drawn hatched in a `MonarchTimeGrid`, such as time off or a closure. Taps still reach the
 * empty slot under it.
 *
 * @property id a stable identifier, unique in the grid.
 * @property start when the block starts, in epoch milliseconds.
 * @property end when it ends, in epoch milliseconds (exclusive).
 * @property label a short text shown in the block and read by TalkBack with its times, such as "Closed".
 */
@Immutable
public class MonarchTimeGridUnavailable(
    public val id: String,
    public val start: Long,
    public val end: Long,
    public val label: String? = null,
)

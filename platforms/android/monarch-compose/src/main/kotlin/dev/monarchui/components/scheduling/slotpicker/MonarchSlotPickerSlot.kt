package dev.monarchui.components.scheduling.slotpicker

import androidx.compose.runtime.Immutable

/**
 * One bookable time of a `MonarchSlotPicker`.
 *
 * @property start the start instant in epoch milliseconds; also the slot's value.
 * @property end the end instant, when the product wants it back with the slot. Not shown.
 * @property seatsLeft places left on a group booking, shown under the time. `null` shows nothing.
 * @property enabled `false` shows the time but does not let it be chosen, for example a time just taken.
 */
@Immutable
public data class MonarchSlotPickerSlot(
    public val start: Long,
    public val end: Long? = null,
    public val seatsLeft: Int? = null,
    public val enabled: Boolean = true,
)

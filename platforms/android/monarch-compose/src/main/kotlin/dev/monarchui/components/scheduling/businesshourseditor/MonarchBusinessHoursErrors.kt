package dev.monarchui.components.scheduling.businesshourseditor

import androidx.compose.runtime.Immutable
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursError

/**
 * The problems [MonarchBusinessHoursValue.validate] found. Every field empty or `false` means the value is valid.
 *
 * @property nameRequired a name is required and missing.
 * @property hours weekly hours problems by ISO weekday, from `MonarchWeeklyHoursInterval.validate`.
 * @property noOpenHours at least one open interval is required and the week has none.
 * @property exceptions exception problems by the index of the date in the list.
 * @property effectiveOrder the last effective day is before the first.
 */
@Immutable
public data class MonarchBusinessHoursErrors(
    public val nameRequired: Boolean = false,
    public val hours: Map<Int, MonarchWeeklyHoursError> = emptyMap(),
    public val noOpenHours: Boolean = false,
    public val exceptions: Map<Int, MonarchBusinessHoursExceptionError> = emptyMap(),
    public val effectiveOrder: Boolean = false,
) {
    /** Whether no problem was found. */
    public val valid: Boolean
        get() = !nameRequired && hours.isEmpty() && !noOpenHours && exceptions.isEmpty() && !effectiveOrder
}

package com.rookhq.monarch.components.datadisplay.periodselector

import androidx.compose.runtime.Immutable

/**
 * The days a period covers, both ends included, as the Material 3 date
 * pickers give them: UTC midnight in epoch milliseconds.
 *
 * @property startMillis the first day, UTC midnight.
 * @property endMillis the last day, UTC midnight; equal to [startMillis] for one day.
 */
@Immutable
public data class MonarchPeriodRange(
    public val startMillis: Long,
    public val endMillis: Long,
)

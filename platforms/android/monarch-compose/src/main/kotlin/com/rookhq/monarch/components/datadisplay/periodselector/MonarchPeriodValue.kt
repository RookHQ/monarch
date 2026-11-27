package com.rookhq.monarch.components.datadisplay.periodselector

import androidx.compose.runtime.Immutable

/**
 * The period a `MonarchPeriodSelector` shows: a preset, and for
 * [MonarchPeriodPreset.Custom] the days the person picked.
 *
 * @property preset the chosen preset.
 * @property range the picked days; used only when [preset] is [MonarchPeriodPreset.Custom], `null` until picked.
 */
@Immutable
public data class MonarchPeriodValue(
    public val preset: MonarchPeriodPreset,
    public val range: MonarchPeriodRange? = null,
)

package dev.monarchui.components.geo.regionselect

import androidx.compose.runtime.Immutable

/**
 * One first-level subdivision of a country for a `MonarchRegionSelect`: a
 * state, province, territory or nation.
 *
 * @property code the stable code to store. The built-in lists use ISO 3166-2 codes such as "US-CA".
 * @property name the name people read. Already localised.
 */
@Immutable
public class MonarchRegionSelectRegion(
    public val code: String,
    public val name: String,
)

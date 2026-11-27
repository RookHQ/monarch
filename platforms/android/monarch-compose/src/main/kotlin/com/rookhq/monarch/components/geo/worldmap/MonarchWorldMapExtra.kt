package com.rookhq.monarch.components.geo.worldmap

import androidx.compose.runtime.Immutable

/**
 * A value that belongs to no single country, such as "Global" or "EU", shown
 * as a tile under a `MonarchWorldMap` and as a row of its table.
 *
 * @property label the visible name. Already localised.
 * @property value the value, written with the map's number format.
 * @property key a stable id. Defaults to [label].
 */
@Immutable
public class MonarchWorldMapExtra(
    public val label: String,
    public val value: Double,
    public val key: String = label,
)

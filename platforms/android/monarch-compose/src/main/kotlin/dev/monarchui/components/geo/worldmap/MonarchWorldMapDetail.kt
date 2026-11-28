package dev.monarchui.components.geo.worldmap

import androidx.compose.runtime.Immutable

/**
 * An extra line about a country in a `MonarchWorldMap`, such as a second
 * metric. It is written under the map when the country is inspected, read by
 * TalkBack with the country and shown as a column of the data table.
 *
 * @property label the name of the line, such as "Users". Already localised.
 * @property value the value, already formatted, such as "340".
 */
@Immutable
public class MonarchWorldMapDetail(
    public val label: String,
    public val value: String,
)

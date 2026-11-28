package dev.monarchui.components.scheduling.shiftroster

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A shift that can be assigned in a `MonarchShiftRoster`, such as "Morning, 06:00 to 14:00".
 *
 * @property id a stable identifier, unique among the roster's shifts. Assignments refer to it.
 * @property name the shift's name, already localised: "Morning".
 * @property start when the shift starts, in minutes from midnight (0 to 1439).
 * @property end when it ends, in minutes from midnight. An end at or before [start] ends the next day: the shift
 * is overnight.
 * @property shortName a short name for the narrow cells of the grid, such as "M". `null` uses [name].
 * @property color the shift's colour, for the bar and tint of its cells only. Unspecified is the theme's accent.
 */
@Immutable
public class MonarchShiftRosterShift(
    public val id: String,
    public val name: String,
    public val start: Int,
    public val end: Int,
    public val shortName: String? = null,
    public val color: Color = Color.Unspecified,
)

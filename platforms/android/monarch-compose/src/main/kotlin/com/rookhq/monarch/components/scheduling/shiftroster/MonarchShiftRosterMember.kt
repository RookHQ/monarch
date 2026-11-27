package com.rookhq.monarch.components.scheduling.shiftroster

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One person in a `MonarchShiftRoster`.
 *
 * @property id a stable identifier, unique in the roster. Assignments are keyed by it.
 * @property name the person's name, already localised. Shown and used for the avatar's initials.
 * @property description a second, muted line: a team, a role or an employee code.
 * @property image the person's picture, loaded by the app. `null` shows the initials.
 */
@Immutable
public class MonarchShiftRosterMember(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val image: Painter? = null,
)

package com.rookhq.monarch.components.identity.assigneepicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus

/**
 * A person `MonarchAssigneePicker` can assign. The app maps its own user
 * records to this.
 *
 * @property id the stable id that is stored.
 * @property name the name people read.
 * @property email shown under the name and matched by the search.
 * @property image the avatar picture, loaded by the app. `null` shows the initials.
 * @property hint short text at the end of the row, such as "3 open" or "Away".
 * @property enabled whether the person can be picked. A person who cannot stays listed.
 * @property group the heading the person is listed under, such as "In this team". Neighbours with the same
 * group share one heading; the list keeps the app's order when any person has a group.
 * @property status a presence dot on the avatar, such as online or away. `null` shows none.
 */
@Immutable
public class MonarchAssigneePerson(
    public val id: String,
    public val name: String,
    public val email: String? = null,
    public val image: Painter? = null,
    public val hint: String? = null,
    public val enabled: Boolean = true,
    public val group: String? = null,
    public val status: MonarchPresenceStatus? = null,
)

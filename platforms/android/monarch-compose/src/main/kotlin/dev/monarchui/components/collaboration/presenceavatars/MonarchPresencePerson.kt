package dev.monarchui.components.collaboration.presenceavatars

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus

/**
 * Someone who is here now, for `MonarchPresenceAvatars`.
 *
 * @property id a stable id, passed back through `onPersonClick` and used as the colour seed by default.
 * @property name the display name.
 * @property image the avatar picture, loaded by the app. `null` shows initials.
 * @property seed picks the fallback colour; [id] by default, so a renamed person keeps their colour.
 * @property status the presence, shown as a dot and always spoken.
 * @property customStatus the person's own status message, "In a meeting", added to the spoken text.
 * @property activity what they are doing here, "Editing" or "Viewing slide 4". Blank is left out.
 */
@Immutable
public class MonarchPresencePerson(
    public val id: String,
    public val name: String,
    public val image: Painter? = null,
    public val seed: String? = null,
    public val status: MonarchPresenceStatus = MonarchPresenceStatus.Online,
    public val customStatus: String? = null,
    public val activity: String? = null,
)

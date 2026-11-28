package com.rookhq.monarch.components.datadisplay.leaderboard

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarShape
import com.rookhq.monarch.foundation.MonarchTone

/**
 * One ranked row of a `MonarchLeaderboard`.
 *
 * @property name who or what is ranked: a person, team, campaign or product.
 * @property value the figure the board ranks by.
 * @property id a stable key; the name by default.
 * @property display the value as shown, "4.2 h" or "38%". `null` writes [value] with the board's formatter.
 * @property secondary a second line, such as a team or "Step 2 of Spring launch".
 * @property image the avatar picture, loaded by the app. `null` shows initials.
 * @property seed picks the avatar's fallback colour, such as the person's id.
 * @property avatarShape a circle for people, a square for teams, products and organisations.
 * @property change the change against the previous period as a ratio, 0.12 for 12%, shown with
 * `MonarchDeltaIndicator`. `null` shows none.
 * @property badge a short tag after the name, "3 breached", drawn as a `MonarchBadge`.
 * @property badgeTone the tone of [badge].
 * @property current this entry is the person viewing: tinted, outlined and tagged "(you)".
 */
@Immutable
public class MonarchLeaderboardEntry(
    public val name: String,
    public val value: Double,
    public val id: String? = null,
    public val display: String? = null,
    public val secondary: String? = null,
    public val image: Painter? = null,
    public val seed: String? = null,
    public val avatarShape: MonarchAvatarShape = MonarchAvatarShape.Circle,
    public val change: Double? = null,
    public val badge: String? = null,
    public val badgeTone: MonarchTone = MonarchTone.Neutral,
    public val current: Boolean = false,
)

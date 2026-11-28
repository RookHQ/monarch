package dev.monarchui.components.collaboration.notificationcenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One notification in a `MonarchNotificationCenter`.
 *
 * @property id a stable id, passed to `onMarkRead` and used as the list key.
 * @property title what happened, already localized: "Ana mentioned you in #design".
 * @property message a one-line detail under the title.
 * @property time when it happened, in epoch milliseconds (UTC).
 * @property read whether the person has seen it.
 * @property actorName who caused it; shown as an avatar with initials. No avatar when `null`.
 * @property actorImage the actor's avatar picture, loaded by the app. `null` shows initials.
 * @property icon a small icon after the title, such as the source app. Decorative; describe it in [iconLabel].
 * @property iconLabel the spoken description of [icon].
 */
@Immutable
public class MonarchNotificationCenterItem(
    public val id: String,
    public val title: String,
    public val time: Long,
    public val message: String? = null,
    public val read: Boolean = false,
    public val actorName: String? = null,
    public val actorImage: Painter? = null,
    public val icon: (@Composable () -> Unit)? = null,
    public val iconLabel: String? = null,
)

package com.rookhq.monarch.components.collaboration.messagerow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * The author of a `MonarchMessageRow`.
 *
 * @property name the display name, shown above the first message of a group and read by TalkBack on every message.
 * @property image the avatar picture, loaded by the app. `null` shows initials.
 * @property seed a stable id for the avatar colour, so a renamed person keeps their colour. The name by default.
 */
@Immutable
public class MonarchMessageRowAuthor(
    public val name: String,
    public val image: Painter? = null,
    public val seed: String? = null,
)

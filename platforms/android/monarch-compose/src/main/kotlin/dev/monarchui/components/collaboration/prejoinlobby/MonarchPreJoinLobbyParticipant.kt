package dev.monarchui.components.collaboration.prejoinlobby

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * A person already in the call, shown in a `MonarchPreJoinLobby`.
 *
 * @property name the display name, read by TalkBack and used for the initials.
 * @property image the avatar picture, loaded by the app. `null` shows initials.
 */
@Immutable
public class MonarchPreJoinLobbyParticipant(
    public val name: String,
    public val image: Painter? = null,
)

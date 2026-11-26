package com.rookhq.monarch.components.collaboration.calltile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable

/**
 * Icons for a `MonarchCallTile`. Monarch bundles no icons: where an icon is
 * `null` the tile writes the word instead ("Muted", "Pin"), so nothing is
 * lost. Each icon takes the content colour of the place it is drawn in.
 *
 * @property micOff a crossed-out microphone, in the name bar while the microphone is off.
 * @property screen a screen, in the name bar while the participant shares their screen.
 * @property handRaised a hand, in the raised-hand badge before its text.
 * @property pin the Pin button while not pinned.
 * @property unpin the Pin button while pinned, usually a crossed-out pin.
 * @property fullScreen the full-screen button.
 */
@Immutable
public class MonarchCallTileIcons(
    public val micOff: (@Composable () -> Unit)? = null,
    public val screen: (@Composable () -> Unit)? = null,
    public val handRaised: (@Composable () -> Unit)? = null,
    public val pin: (@Composable () -> Unit)? = null,
    public val unpin: (@Composable () -> Unit)? = null,
    public val fullScreen: (@Composable () -> Unit)? = null,
)

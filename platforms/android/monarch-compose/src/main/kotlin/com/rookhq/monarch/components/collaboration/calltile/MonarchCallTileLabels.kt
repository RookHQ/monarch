package com.rookhq.monarch.components.collaboration.calltile

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCallTile`. Every `null`
 * keeps the built-in string `monarch_calltile_<key>`.
 *
 * @property you the name of your own tile, given the name: "Ana (you)".
 * @property muted the microphone is off, written when there is no icon and always read.
 * @property sharingScreen the participant shares their screen.
 * @property handRaised the text of the raised-hand badge, also read.
 * @property speaking read while the participant speaks.
 * @property pin what TalkBack reads for the Pin button, given the name: "Pin Ana".
 * @property unpin the same while pinned: "Unpin Ana".
 * @property fullScreen what TalkBack reads for the full-screen button, given the name.
 * @property pinButton the visible text of the Pin button when there is no icon.
 * @property unpinButton the same while pinned.
 * @property fullScreenButton the visible text of the full-screen button when there is no icon.
 * @property reacted announced for a new reaction, given the name and the emoji: "Ana reacted 👏".
 * @property quality the spoken connection quality: "Poor connection".
 */
@Immutable
public class MonarchCallTileLabels(
    public val you: ((String) -> String)? = null,
    public val muted: String? = null,
    public val sharingScreen: String? = null,
    public val handRaised: String? = null,
    public val speaking: String? = null,
    public val pin: ((String) -> String)? = null,
    public val unpin: ((String) -> String)? = null,
    public val fullScreen: ((String) -> String)? = null,
    public val pinButton: String? = null,
    public val unpinButton: String? = null,
    public val fullScreenButton: String? = null,
    public val reacted: ((name: String, emoji: String) -> String)? = null,
    public val quality: ((MonarchCallQuality) -> String)? = null,
)

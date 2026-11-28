package dev.monarchui.components.collaboration.callstage

import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.calltile.MonarchCallTileLabels

/**
 * Replacements for the built-in strings of a `MonarchCallStage`. Every `null` keeps the built-in string
 * `monarch_callstage_<key>`.
 *
 * @property tile the strings of every `MonarchCallTile`.
 * @property region the name of the stage without a title: "Call".
 * @property duration the spoken name of the running timer: "Call duration".
 * @property waiting shown instead of the head count while you are alone: "Waiting for others…".
 * @property inCall the head count, given the number of people: "3 in call".
 * @property lowData the badge while audio-only mode is on: "Low data".
 * @property reconnecting the warning while the connection recovers: "You're offline. Trying to reconnect…".
 * @property mutedHint the hint when you speak while muted: "You're muted. Turn on your microphone to talk.".
 * @property pictureInPicture the picture in picture button: "Picture in picture".
 * @property shrink the button that docks the stage: "Shrink call".
 * @property expand the button that brings the docked stage back: "Expand call".
 * @property minimize the button that minimizes the docked stage: "Minimize call".
 */
@Immutable
public class MonarchCallStageLabels(
    public val tile: MonarchCallTileLabels = MonarchCallTileLabels(),
    public val region: String? = null,
    public val duration: String? = null,
    public val waiting: String? = null,
    public val inCall: ((count: Int) -> String)? = null,
    public val lowData: String? = null,
    public val reconnecting: String? = null,
    public val mutedHint: String? = null,
    public val pictureInPicture: String? = null,
    public val shrink: String? = null,
    public val expand: String? = null,
    public val minimize: String? = null,
)

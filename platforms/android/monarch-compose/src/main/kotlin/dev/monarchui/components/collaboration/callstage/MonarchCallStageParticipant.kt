package dev.monarchui.components.collaboration.callstage

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import dev.monarchui.components.collaboration.calltile.MonarchCallQuality
import dev.monarchui.components.collaboration.calltile.MonarchCallReaction
import dev.monarchui.foundation.MonarchTone

/**
 * One person on a call, as a `MonarchCallStage` shows them in a `MonarchCallTile`.
 *
 * @property id the stable id, used for the layout, pinning and the video slot.
 * @property name the display name. Names the tile and its buttons.
 * @property image the participant's picture for the avatar, loaded by the app. `null` shows initials.
 * @property showVideo shows the video slot. Pass `false` while connecting or when the camera is off.
 * @property self this is the person using the app: "(you)" is added and they never take the spotlight as the
 * speaker or with their own shared screen.
 * @property micOff the microphone is off.
 * @property speaking the participant is speaking.
 * @property handRaised the participant raised a hand.
 * @property screen the video is a shared screen. Someone else's shared screen takes the spotlight.
 * @property status a short status in the name bar, such as "Reconnecting…".
 * @property statusTone the colour of [status].
 * @property quality the connection quality; `null` when unknown.
 * @property reaction a reaction that rises over the tile for three seconds.
 */
@Immutable
public class MonarchCallStageParticipant(
    public val id: String,
    public val name: String,
    public val image: Painter? = null,
    public val showVideo: Boolean = true,
    public val self: Boolean = false,
    public val micOff: Boolean = false,
    public val speaking: Boolean = false,
    public val handRaised: Boolean = false,
    public val screen: Boolean = false,
    public val status: String? = null,
    public val statusTone: MonarchTone = MonarchTone.Neutral,
    public val quality: MonarchCallQuality? = null,
    public val reaction: MonarchCallReaction? = null,
)

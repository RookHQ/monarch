package dev.monarchui.components.collaboration.prejoinlobby

import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.callcontrols.MonarchCallControlsLabels

/**
 * Replacements for the built-in strings of a `MonarchPreJoinLobby`. Every
 * `null` keeps the built-in string `monarch_prejoinlobby_<key>`.
 *
 * @property title the heading: "Join the call".
 * @property preview the TalkBack name of the preview area: "Your camera preview".
 * @property cameraOff shown in the preview while the camera is off.
 * @property startingCamera shown in the preview while the camera is on but the app has no preview yet.
 * @property noPreview shown in the preview while there is an error.
 * @property micLevel the TalkBack name of the microphone meter.
 * @property muted the value of the microphone meter while the microphone is off.
 * @property participants the TalkBack name of the avatars of the people in the call.
 * @property alreadyIn the line next to those avatars, given their number: "3 already in the call".
 * @property cancel the cancel button.
 * @property join the join button: "Join now".
 * @property joining what TalkBack reads for the join button while joining.
 * @property callControls the strings of the microphone and camera toggles.
 */
@Immutable
public class MonarchPreJoinLobbyLabels(
    public val title: String? = null,
    public val preview: String? = null,
    public val cameraOff: String? = null,
    public val startingCamera: String? = null,
    public val noPreview: String? = null,
    public val micLevel: String? = null,
    public val muted: String? = null,
    public val participants: String? = null,
    public val alreadyIn: ((count: Int) -> String)? = null,
    public val cancel: String? = null,
    public val join: String? = null,
    public val joining: String? = null,
    public val callControls: MonarchCallControlsLabels = MonarchCallControlsLabels(),
)

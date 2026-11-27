package com.rookhq.monarch.components.collaboration.callcontrols

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of `MonarchCallControls`. Every
 * `null` keeps the built-in string `monarch_callcontrols_<key>`.
 *
 * @property toolbar the name of the group of controls.
 * @property microphone the name of the microphone toggle.
 * @property camera the name of the camera toggle.
 * @property shareScreen the name of the screen sharing toggle.
 * @property raiseHand the name of the raised hand toggle.
 * @property on the state of a toggle that is on.
 * @property off the state of a toggle that is off.
 * @property react the name of the reactions menu button.
 * @property reactWith the name of a reaction in the menu, given the emoji: "React 👍".
 * @property more the name of the More menu button.
 * @property layout the heading of the layout choices in More.
 * @property layoutName the name of a layout choice.
 * @property lowData the low data item in More.
 * @property devices the devices item in More.
 * @property stats the connection item in More.
 * @property pictureInPicture the picture in picture item in More.
 * @property leave the Leave button.
 */
@Immutable
public class MonarchCallControlsLabels(
    public val toolbar: String? = null,
    public val microphone: String? = null,
    public val camera: String? = null,
    public val shareScreen: String? = null,
    public val raiseHand: String? = null,
    public val on: String? = null,
    public val off: String? = null,
    public val react: String? = null,
    public val reactWith: ((emoji: String) -> String)? = null,
    public val more: String? = null,
    public val layout: String? = null,
    public val layoutName: ((layout: MonarchCallControlsLayout) -> String)? = null,
    public val lowData: String? = null,
    public val devices: String? = null,
    public val stats: String? = null,
    public val pictureInPicture: String? = null,
    public val leave: String? = null,
)

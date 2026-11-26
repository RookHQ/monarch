package com.rookhq.monarch.components.collaboration.devicepicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDevicePicker`. Every `null` keeps the built-in string
 * `monarch_devicepicker_<key>`.
 *
 * @property microphone the label of the microphone field: "Microphone".
 * @property camera the label of the camera field: "Camera".
 * @property speaker the label of the audio output field: "Speaker".
 * @property systemDefault the first option of every field, the device Android picks: "System default".
 * @property unnamedMicrophone names a microphone without a name from its position: "Microphone 2".
 * @property unnamedCamera names a camera without a name from its position: "Camera 2".
 * @property unnamedSpeaker names an output without a name from its position: "Speaker 2".
 * @property notDetermined the notice before access is asked.
 * @property allowAccess the button of that notice: "Allow access".
 * @property denied the notice when access is refused.
 * @property openSettings the button of that notice: "Open Settings".
 * @property unavailable the notice when there is no camera or microphone.
 */
@Immutable
public class MonarchDevicePickerLabels(
    public val microphone: String? = null,
    public val camera: String? = null,
    public val speaker: String? = null,
    public val systemDefault: String? = null,
    public val unnamedMicrophone: ((position: Int) -> String)? = null,
    public val unnamedCamera: ((position: Int) -> String)? = null,
    public val unnamedSpeaker: ((position: Int) -> String)? = null,
    public val notDetermined: String? = null,
    public val allowAccess: String? = null,
    public val denied: String? = null,
    public val openSettings: String? = null,
    public val unavailable: String? = null,
)

package com.rookhq.monarch.components.collaboration.devicepicker

import androidx.compose.runtime.Immutable

/**
 * A microphone, camera or audio output offered by a `MonarchDevicePicker`.
 *
 * The app lists the devices: microphones and speakers from `AudioManager.getDevices` or, from API 31,
 * `AudioManager.availableCommunicationDevices` (the id is `AudioDeviceInfo.id` as text), and cameras from
 * `CameraManager.cameraIdList` (the id is the camera id). It names them, for example from
 * `AudioDeviceInfo.productName` or the lens facing.
 *
 * @property id the stable id the app uses to apply the choice. A device with an empty id is left out.
 * @property name the name shown in the menu, already localised. A blank name is numbered by position:
 * "Camera 2".
 */
@Immutable
public class MonarchDevicePickerDevice(
    public val id: String,
    public val name: String,
)

package com.rookhq.monarch.components.collaboration.devicepicker

/** The camera and microphone permission state a `MonarchDevicePicker` explains, as the app knows it. */
public enum class MonarchDevicePickerAccess {
    /** Permission is granted: the fields only. The default. */
    Granted,

    /** Not asked yet: an info notice with Allow access. */
    NotDetermined,

    /** Refused, and Android no longer shows the request: a warning notice with Open Settings. */
    Denied,

    /** The device has no camera or microphone: a neutral notice. */
    Unavailable,
}

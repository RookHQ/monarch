package com.rookhq.monarch.components.layout.deviceframe

/** The device a `MonarchDeviceFrame` draws. The same names exist on every platform. */
public enum class MonarchDeviceFrameDevice {
    /** A phone: rounded corners, a narrow bezel and a camera island. 390 by 844dp by default. The default. */
    Phone,

    /** A tablet: a wider bezel, smaller corners and no island. 820 by 1180dp by default. */
    Tablet,
}

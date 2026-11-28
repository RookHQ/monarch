package dev.monarchui.components.identity.devicelist

/** The loading state of a `MonarchDeviceList`. */
public enum class MonarchDeviceListState {
    /** The devices are loaded. An empty list shows the empty state. */
    Ready,

    /** The devices are loading. */
    Loading,

    /** Loading failed. */
    Failed,
}

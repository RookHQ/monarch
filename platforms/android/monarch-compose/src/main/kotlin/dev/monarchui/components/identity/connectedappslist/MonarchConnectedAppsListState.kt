package dev.monarchui.components.identity.connectedappslist

/** The loading state of a `MonarchConnectedAppsList`. */
public enum class MonarchConnectedAppsListState {
    /** The apps are loaded. An empty list shows the empty state. */
    Ready,

    /** The apps are loading. */
    Loading,

    /** Loading failed. */
    Error,
}

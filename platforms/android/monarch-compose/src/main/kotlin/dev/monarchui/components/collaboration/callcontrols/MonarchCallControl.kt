package dev.monarchui.components.collaboration.callcontrols

/** One control of `MonarchCallControls`, passed to its icon slot so the app can draw the right glyph. */
public enum class MonarchCallControl {
    /** The microphone toggle. Its icon slot gets `on = false` when muted. */
    Microphone,

    /** The camera toggle. */
    Camera,

    /** The screen sharing toggle. */
    ShareScreen,

    /** The raised hand toggle. */
    RaiseHand,

    /** The reactions menu button. */
    React,

    /** The More menu button. */
    More,

    /** The Leave button. */
    Leave,
}

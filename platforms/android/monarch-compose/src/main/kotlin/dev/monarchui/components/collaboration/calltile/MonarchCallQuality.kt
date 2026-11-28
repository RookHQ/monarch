package dev.monarchui.components.collaboration.calltile

/** The connection quality of a participant, from the app's call statistics. `null` means unknown. */
public enum class MonarchCallQuality {
    /** Three lit bars, "Good connection". */
    Good,

    /** Two lit bars, "Unstable connection". */
    Fair,

    /** One lit bar, "Poor connection". */
    Poor,
}

package dev.monarchui.components.scheduling.timegrid

import androidx.compose.ui.layout.LayoutCoordinates

/** What the gestures of the columns read and write outside composition. */
internal class TimeGridPointer {
    /** The columns row, to read pointer positions in. */
    var area: LayoutCoordinates? = null

    /** Where the current touch went down in a column, for the slot it lands on; `null` for TalkBack and keys. */
    var tapY: Float? = null

    /** A long press picked an event up, so the release does not also open it. */
    var held: Boolean = false
}

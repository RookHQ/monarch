package dev.monarchui.components.workflow.runningtimer

/** The form of a `MonarchRunningTimer`. */
public enum class MonarchRunningTimerVariant {
    /** A large clock with Start, Stop, "I stopped earlier", Discard and room for the entry's fields. */
    Panel,

    /** A compact chip for a top bar: the clock, a stop button and a menu. Hidden while stopped unless asked. */
    Indicator,
}

package dev.monarchui.components.workflow.prioritypicker

/**
 * How urgent an issue is, from most to least urgent, with [None] for not
 * set. The order of the constants is the order of a `MonarchPriorityPicker`
 * menu.
 *
 * @property number the number many trackers store: 0 none, 1 urgent, 2 high, 3 medium, 4 low.
 */
@Suppress("MagicNumber") // The numbers trackers store, documented on [number].
public enum class MonarchPriorityLevel(public val number: Int) {
    /** Needs attention now. Drawn as a filled tile with an exclamation mark. */
    Urgent(1),

    /** Drawn as three filled bars. */
    High(2),

    /** Drawn as two filled bars. */
    Medium(3),

    /** Drawn as one filled bar. */
    Low(4),

    /** No priority set. Drawn as three dashes. */
    None(0),
    ;

    /** Converts a stored number to a level. */
    public companion object {
        /** The level of [number] (0 none, 1 urgent, 2 high, 3 medium, 4 low); any other value is [None]. */
        public fun fromNumber(number: Int?): MonarchPriorityLevel = entries.firstOrNull { it.number == number } ?: None
    }
}

package dev.monarchui.components.forms.ratingpicker

import androidx.compose.runtime.Immutable

/**
 * One value on the scale of a `MonarchRatingPicker`.
 *
 * @property value the number stored and passed to `onValueChange`.
 * @property label a short meaning shown with the number, already localised, for example "Meets expectations".
 */
@Immutable
public class MonarchRatingOption(public val value: Int, public val label: String? = null) {
    /** Builds numeric scales. */
    public companion object {
        /** The first value of the default scale. */
        public const val DEFAULT_FIRST: Int = 1

        /** The last value of the default scale. */
        public const val DEFAULT_LAST: Int = 5

        /**
         * The whole numbers from [first] to [last], without labels.
         *
         * @param first the first value.
         * @param last the last value.
         */
        public fun range(first: Int = DEFAULT_FIRST, last: Int = DEFAULT_LAST): List<MonarchRatingOption> =
            (first..last).map { MonarchRatingOption(it) }
    }
}

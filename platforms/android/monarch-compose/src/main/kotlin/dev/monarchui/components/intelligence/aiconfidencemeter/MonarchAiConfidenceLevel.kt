package dev.monarchui.components.intelligence.aiconfidencemeter

/** A confidence band. Each band has its own words, so it never depends on colour. */
public enum class MonarchAiConfidenceLevel {
    /** Below the low threshold. Read "Needs check" by default, so people know to verify it. */
    Low,

    /** Between the thresholds. */
    Medium,

    /** At or above the high threshold. */
    High,
    ;

    public companion object {
        /** The default edge under which confidence is low, as on the web. */
        public const val DEFAULT_LOW_THRESHOLD: Float = 0.6f

        /** The default edge from which confidence is high, as on the web. */
        public const val DEFAULT_HIGH_THRESHOLD: Float = 0.85f

        /**
         * The band of a confidence from 0 to 1: below [lowThreshold] is low, at
         * or above [highThreshold] is high, anything between is medium. The
         * value is clamped to 0 to 1 and not-a-number counts as 0.
         */
        public fun of(
            value: Float,
            lowThreshold: Float = DEFAULT_LOW_THRESHOLD,
            highThreshold: Float = DEFAULT_HIGH_THRESHOLD,
        ): MonarchAiConfidenceLevel {
            val clamped = clamp(value)
            return when {
                clamped < lowThreshold -> Low
                clamped >= highThreshold -> High
                else -> Medium
            }
        }

        /** The value clamped to 0 to 1, with not-a-number as 0. */
        internal fun clamp(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)
    }
}

package dev.monarchui.components.intelligence.aicontextwindowmeter

/** How full a context window is. The same names exist on every platform. */
public enum class MonarchAiContextWindowState {
    /** Below the warning share. */
    Ok,

    /** At or above the warning share, below the limit. */
    Warning,

    /** At or over the limit: earlier messages are being left out. */
    Full,
    ;

    /** Decides the state of a context window. */
    public companion object {
        /**
         * The state of a window holding [used] tokens of [limit], warning at the share [warningAt]. A limit of
         * zero or less is always [Ok]; a negative count counts as zero.
         */
        public fun of(used: Long, limit: Long, warningAt: Double = DefaultWarningAt): MonarchAiContextWindowState {
            if (limit <= 0L) return Ok
            val share = used.coerceAtLeast(0L).toDouble() / limit.toDouble()
            return when {
                share >= 1.0 -> Full
                share >= warningAt -> Warning
                else -> Ok
            }
        }

        private const val DefaultWarningAt = 0.8
    }
}

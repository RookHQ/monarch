package com.rookhq.monarch.components.intelligence.aicontextchips

/** How much of the context one item takes, from its share of the total or of a budget. */
public enum class MonarchAiContextWeight {
    /** Less than a fifth. One bar of the mark is filled. */
    Light,

    /** From a fifth to a half. Two bars are filled. */
    Medium,

    /** Half or more. Three bars are filled and the count is drawn in the default text colour. */
    Heavy,
    ;

    public companion object {
        private const val HeavyShare = 0.5
        private const val MediumShare = 0.2

        /**
         * The weight of [tokens] against [reference]: heavy at half the
         * reference or more, medium from a fifth, otherwise light. A count or
         * reference of zero or less is light.
         *
         * @param tokens the tokens of the item.
         * @param reference the total of all items, or the budget.
         */
        public fun of(tokens: Int, reference: Int): MonarchAiContextWeight {
            if (reference <= 0 || tokens <= 0) return Light
            val share = tokens.toDouble() / reference
            return when {
                share >= HeavyShare -> Heavy
                share >= MediumShare -> Medium
                else -> Light
            }
        }
    }
}

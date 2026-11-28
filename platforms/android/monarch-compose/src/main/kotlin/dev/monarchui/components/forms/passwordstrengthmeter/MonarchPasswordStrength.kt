package dev.monarchui.components.forms.passwordstrengthmeter

/** How strong a password is, from very weak to strong. The order is the order of strength. */
public enum class MonarchPasswordStrength {
    /** Fewer than half of the rules are met. */
    VeryWeak,

    /** Half of the rules are met. */
    Weak,

    /** Three quarters of the rules are met. */
    Fair,

    /** Every rule is met. */
    Good,

    /** Every rule is met and the password has at least 14 characters. */
    Strong,
    ;

    /** The default estimator. */
    public companion object {
        /** The length from which a password that meets every rule is strong. */
        public const val STRONG_LENGTH: Int = 14

        private const val FAIR_SHARE = 0.75
        private const val WEAK_SHARE = 0.5

        /**
         * The default score: the share of [rules] met, plus one level for
         * [STRONG_LENGTH] or more characters once every rule is met. An empty
         * password is [VeryWeak]. Replace it with a stronger estimator through
         * the `score` parameter of the meter.
         *
         * @param password the password to rate.
         * @param rules the rules to count.
         */
        public fun score(password: String, rules: List<MonarchPasswordRule>): MonarchPasswordStrength {
            if (password.isEmpty()) return VeryWeak
            val met = rules.count { it.test(password) }
            val share = if (rules.isEmpty()) 1.0 else met.toDouble() / rules.size
            return when {
                share >= 1.0 -> if (password.codePointCount(0, password.length) >= STRONG_LENGTH) Strong else Good
                share >= FAIR_SHARE -> Fair
                share >= WEAK_SHARE -> Weak
                else -> VeryWeak
            }
        }
    }
}

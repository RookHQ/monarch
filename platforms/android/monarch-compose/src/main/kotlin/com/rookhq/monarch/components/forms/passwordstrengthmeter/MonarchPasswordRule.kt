package com.rookhq.monarch.components.forms.passwordstrengthmeter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R

/**
 * A requirement listed by `MonarchPasswordStrengthMeter`, with its own test.
 *
 * @property id a stable key, unique in its list.
 * @property label what the password needs, already localised, for example "At least 8 characters".
 * @property test returns `true` when the password meets the rule.
 */
@Immutable
public class MonarchPasswordRule(
    public val id: String,
    public val label: String,
    public val test: (String) -> Boolean,
) {
    /** The default rules. */
    public companion object {
        /** The fewest characters the default length rule accepts. */
        public const val DEFAULT_MIN_LENGTH: Int = 8

        private val upperPattern = Regex("""\p{Lu}""")
        private val lowerPattern = Regex("""\p{Ll}""")
        private val digitPattern = Regex("""\p{Nd}""")
        private val symbolPattern = Regex("""[^\p{L}\p{Nd}]""")

        /**
         * The default requirements, with these labels: at least 8 characters,
         * upper and lower case letters, a number, and a symbol or space.
         * Lengths count Unicode code points, so an emoji is one character.
         *
         * @param length the label of the length rule.
         * @param case the label of the mixed case rule.
         * @param number the label of the number rule.
         * @param symbol the label of the symbol rule.
         */
        public fun defaults(length: String, case: String, number: String, symbol: String): List<MonarchPasswordRule> =
            listOf(
                MonarchPasswordRule("length", length) { it.codePointCount(0, it.length) >= DEFAULT_MIN_LENGTH },
                MonarchPasswordRule("case", case) {
                    upperPattern.containsMatchIn(it) && lowerPattern.containsMatchIn(it)
                },
                MonarchPasswordRule("number", number) { digitPattern.containsMatchIn(it) },
                MonarchPasswordRule("symbol", symbol) { symbolPattern.containsMatchIn(it) },
            )

        /** The default requirements with the built-in labels `monarch_passwordstrengthmeter_rule_<id>`. */
        @Composable
        public fun defaults(): List<MonarchPasswordRule> = defaults(
            length = stringResource(R.string.monarch_passwordstrengthmeter_rule_length),
            case = stringResource(R.string.monarch_passwordstrengthmeter_rule_case),
            number = stringResource(R.string.monarch_passwordstrengthmeter_rule_number),
            symbol = stringResource(R.string.monarch_passwordstrengthmeter_rule_symbol),
        )
    }
}

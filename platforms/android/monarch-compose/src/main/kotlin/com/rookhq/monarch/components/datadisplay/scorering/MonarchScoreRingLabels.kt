package com.rookhq.monarch.components.datadisplay.scorering

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchScoreRing`. Every `null`
 * uses the built-in string named after it in `strings_scorering.xml`.
 *
 * @property name the name TalkBack reads when there is no label: "Score".
 * @property valueText writes the spoken value from the formatted score and maximum: "78 out of 100".
 * @property ofMax writes the small text under the number from the formatted maximum: "/ 100".
 * @property noValue the spoken state when there is no score: "No score yet".
 */
@Immutable
public class MonarchScoreRingLabels(
    public val name: String? = null,
    public val valueText: ((value: String, max: String) -> String)? = null,
    public val ofMax: ((max: String) -> String)? = null,
    public val noValue: String? = null,
)

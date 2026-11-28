package dev.monarchui.components.intelligence.aiscore

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiScore`. Every `null`
 * uses the built-in string in `strings_aiscore.xml`.
 *
 * @property label what is scored when the component has no label: "Score".
 * @property aiGenerated the AI mark next to the label: "AI score".
 * @property valueText writes the spoken value from the formatted score and maximum: "72 out of 100".
 * @property unavailable shown and read when there is no score: "No score yet".
 * @property factors the heading of the factors: "What affects this score".
 * @property impact writes the spoken impact of a factor from the formatted amount and whether it raised the
 * score: "Raised the score by 18", "Lowered the score by 9".
 * @property noImpact the spoken impact of a factor with no effect: "No effect on the score".
 * @property bands the header of the band legend: "How to read this score".
 * @property currentBand marks the band the score is in, in the legend: "this score".
 * @property factorColumn the heading of the factor column: "Factor".
 * @property impactColumn the heading of the impact column: "Impact".
 */
@Immutable
public class MonarchAiScoreLabels(
    public val label: String? = null,
    public val aiGenerated: String? = null,
    public val valueText: ((value: String, max: String) -> String)? = null,
    public val unavailable: String? = null,
    public val factors: String? = null,
    public val impact: ((amount: String, raised: Boolean) -> String)? = null,
    public val noImpact: String? = null,
    public val bands: String? = null,
    public val currentBand: String? = null,
    public val factorColumn: String? = null,
    public val impactColumn: String? = null,
)

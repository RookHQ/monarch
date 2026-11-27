package com.rookhq.monarch.components.intelligence.aiconfidencemeter

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiConfidenceMeter`.
 * Every `null` keeps the built-in string `monarch_aiconfidencemeter_<key>`.
 *
 * @property low the label of the low band, "Needs check".
 * @property medium the label of the medium band, "Medium confidence".
 * @property high the label of the high band, "High confidence".
 * @property meter the name TalkBack reads for the bar, "Confidence".
 * @property valueText the text TalkBack reads for the value, from the band label and the formatted
 * percentage, by default "Needs check, 42% confidence".
 */
@Immutable
public class MonarchAiConfidenceMeterLabels(
    public val low: String? = null,
    public val medium: String? = null,
    public val high: String? = null,
    public val meter: String? = null,
    public val valueText: ((label: String, percent: String) -> String)? = null,
)

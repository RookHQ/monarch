package com.rookhq.monarch.components.intelligence.aisummarycard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchAiSummaryCard` supplies. Each `null`
 * keeps the built-in English string.
 *
 * @property aiSummary the provenance label above the title. Default "AI summary".
 * @property refresh the Refresh button. Default "Refresh".
 * @property generated the age line from the relative time, such as "3 hours ago". Default "Generated {time}".
 * @property generating shown and announced while a summary is written. Default "Generating summary…".
 * @property updated announced when a summary finishes. Default "Summary updated.".
 * @property stale the warning when the data changed. Default "The data changed since this summary was written.
 * Refresh to update it.".
 * @property error the error title. Default "The summary couldn’t be generated.".
 * @property empty shown when there is no summary and nothing is generating. Default "No summary yet.".
 * @property sources the heading of the source list. Default "Sources".
 */
@Immutable
public class MonarchAiSummaryCardLabels(
    public val aiSummary: String? = null,
    public val refresh: String? = null,
    public val generated: ((relativeTime: String) -> String)? = null,
    public val generating: String? = null,
    public val updated: String? = null,
    public val stale: String? = null,
    public val error: String? = null,
    public val empty: String? = null,
    public val sources: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchAiSummaryCardLabels && other.aiSummary == aiSummary && other.refresh == refresh &&
            other.generated == generated && other.generating == generating && other.updated == updated &&
            other.stale == stale && other.error == error && other.empty == empty && other.sources == sources

    override fun hashCode(): Int =
        listOf(aiSummary, refresh, generated, generating, updated, stale, error, empty, sources).hashCode()
}

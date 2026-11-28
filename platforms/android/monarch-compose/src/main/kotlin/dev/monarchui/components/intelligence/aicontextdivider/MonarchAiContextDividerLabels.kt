package dev.monarchui.components.intelligence.aicontextdivider

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiContextDivider`.
 * Every `null` keeps the built-in string `monarch_aicontextdivider_<key>`.
 *
 * @property label replaces the label of the kind: the day, "Context reset" or "Earlier messages were
 * summarized".
 * @property detail replaces the description of a context reset, "The assistant does not see the messages
 * above this line."
 * @property today the label of a day divider for today, "Today".
 * @property yesterday the label of a day divider for yesterday, "Yesterday".
 * @property showSummary the title of the disclosure that shows the summary, "Show summary".
 * @property summaryPrefix read by TalkBack before the summary and not shown, "AI-generated summary:".
 * @property expand the button that shows the original messages, "Show earlier messages".
 */
@Immutable
public class MonarchAiContextDividerLabels(
    public val label: String? = null,
    public val detail: String? = null,
    public val today: String? = null,
    public val yesterday: String? = null,
    public val showSummary: String? = null,
    public val summaryPrefix: String? = null,
    public val expand: String? = null,
)

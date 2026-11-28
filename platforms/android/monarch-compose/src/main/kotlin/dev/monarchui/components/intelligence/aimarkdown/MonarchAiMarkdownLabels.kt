package dev.monarchui.components.intelligence.aimarkdown

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings MonarchAiMarkdown adds around the content.
 * Each `null` keeps the built-in English string.
 *
 * @property table the name TalkBack reads for a table's scroll area. Default "Table".
 * @property footnotes the heading of the footnotes at the end. Default "Footnotes".
 * @property task the name of a task-list check mark; its state is read after it. Default "Task".
 * @property linkHost the host shown after a link whose text does not name it. Default "(example.com)".
 */
@Immutable
public class MonarchAiMarkdownLabels(
    public val table: String? = null,
    public val footnotes: String? = null,
    public val task: String? = null,
    public val linkHost: ((host: String) -> String)? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchAiMarkdownLabels && other.table == table && other.footnotes == footnotes &&
            other.task == task && other.linkHost == linkHost

    override fun hashCode(): Int {
        var result = table.hashCode()
        result = 31 * result + footnotes.hashCode()
        result = 31 * result + task.hashCode()
        result = 31 * result + linkHost.hashCode()
        return result
    }
}

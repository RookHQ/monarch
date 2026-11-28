package dev.monarchui.components.datadisplay.statcard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchStatCard` supplies. Each `null` keeps
 * the built-in English string.
 *
 * @property empty what TalkBack reads for a missing value, shown as a dash. Default "No data".
 * @property loading the state TalkBack reads while the value loads. Default "Loading".
 * @property help the name of the help button, from the label. Default "About {label}".
 */
@Immutable
public class MonarchStatCardLabels(
    public val empty: String? = null,
    public val loading: String? = null,
    public val help: ((label: String) -> String)? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchStatCardLabels && other.empty == empty && other.loading == loading && other.help == help

    override fun hashCode(): Int {
        var result = empty.hashCode()
        result = 31 * result + loading.hashCode()
        result = 31 * result + help.hashCode()
        return result
    }
}

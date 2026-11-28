package dev.monarchui.components.search.searchresults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextRange

/**
 * One result in a `MonarchSearchResults` group, drawn as a
 * `MonarchSearchResultItem`. Every string is plain text; the matches are
 * highlighted by the component.
 *
 * Two results are equal when their [id]s are equal.
 *
 * @property title the result's title.
 * @property id a stable id, unique across all groups.
 * @property path breadcrumb segments, such as `listOf("Handbook", "Travel")`.
 * @property snippet an excerpt of the matching content.
 * @property titleRanges exact matches in the title from the search service, as UTF-16 offsets with an exclusive
 * end. Replace the query's words for the title.
 * @property snippetRanges exact matches in the snippet from the search service.
 * @property snippetMarkTags the snippet carries `<mark>` and `</mark>` markers from the search service.
 * @property fileName a file name; its file type icon fills the leading slot.
 * @property meta trailing details such as a type or a date, in the caption type.
 * @property icon a leading icon. Decorative.
 * @property destructive draws the title in the danger colour, for a result that removes or archives something.
 */
@Immutable
public class MonarchSearchResult(
    public val title: String,
    public val id: String,
    public val path: List<String> = emptyList(),
    public val snippet: String? = null,
    public val titleRanges: List<TextRange>? = null,
    public val snippetRanges: List<TextRange>? = null,
    public val snippetMarkTags: Boolean = false,
    public val fileName: String? = null,
    public val meta: String? = null,
    public val icon: (@Composable () -> Unit)? = null,
    public val destructive: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = other is MonarchSearchResult && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "MonarchSearchResult(id=$id, title=$title)"
}

package dev.monarchui.components.intelligence.aitableanswer

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiTableAnswer`. Every
 * `null` keeps the built-in string `monarch_aitableanswer_<key>`.
 *
 * @property aiTable the provenance label above the title, "AI table".
 * @property rowCount the row count under the title: "1 row", "12 rows".
 * @property copyCsv the copy button, "Copy as CSV".
 * @property copied the copy button after a copy, where the app shows it, "Copied".
 * @property openAsArtifact the button that opens the table as an artifact, "Open as artifact".
 * @property sorted announced after a sort, from the column and the direction: "Sorted by Revenue, ascending.".
 * @property sortState the state of the sorted header, from the direction: "Sorted ascending".
 * @property empty read by TalkBack for an empty cell, "Empty".
 * @property noRows shown when there are no rows and none are coming, "No rows.".
 * @property loadingRows read for the placeholder row while rows stream in, "More rows are being generated".
 * @property complete announced when streaming ends, "Table complete.".
 * @property showAllRows the button that shows every row, from the number of rows: "Show all 250 rows".
 */
@Immutable
public class MonarchAiTableAnswerLabels(
    public val aiTable: String? = null,
    public val rowCount: ((count: Int) -> String)? = null,
    public val copyCsv: String? = null,
    public val copied: String? = null,
    public val openAsArtifact: String? = null,
    public val sorted: ((column: String, ascending: Boolean) -> String)? = null,
    public val sortState: ((ascending: Boolean) -> String)? = null,
    public val empty: String? = null,
    public val noRows: String? = null,
    public val loadingRows: String? = null,
    public val complete: String? = null,
    public val showAllRows: ((count: Int) -> String)? = null,
)

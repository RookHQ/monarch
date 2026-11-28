package dev.monarchui.components.datagrid.mergedialog

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchMergeDialog`. Every
 * `null` uses the built-in string named after it in `strings_mergedialog.xml`.
 *
 * @property title the heading: "Merge records".
 * @property description the line under the heading, which explains the choices.
 * @property keep the heading of the record choice: "Record to keep".
 * @property kept the badge on the kept record: "Kept".
 * @property values the heading of the field choices: "Values to keep".
 * @property same the heading of the fields that match: "Same in every record".
 * @property empty shown for an empty value: "Empty".
 * @property consequences the heading of the consequences: "What happens".
 * @property cancel the label of the cancel button: "Cancel".
 * @property error the message shown after a failed merge: "The records could not be merged. Nothing was changed."
 * @property from writes the source of a value from the record's title: "from Acme Ltd".
 * @property confirm writes the label of the merge button from the number of records: "Merge 2 records".
 */
@Immutable
public class MonarchMergeDialogLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val keep: String? = null,
    public val kept: String? = null,
    public val values: String? = null,
    public val same: String? = null,
    public val empty: String? = null,
    public val consequences: String? = null,
    public val cancel: String? = null,
    public val error: String? = null,
    public val from: ((record: String) -> String)? = null,
    public val confirm: ((count: Int) -> String)? = null,
)

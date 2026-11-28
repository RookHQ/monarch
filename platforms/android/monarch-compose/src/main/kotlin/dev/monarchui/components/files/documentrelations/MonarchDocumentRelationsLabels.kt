package dev.monarchui.components.files.documentrelations

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDocumentRelations`.
 * Every `null` uses the built-in string named after it in
 * `strings_documentrelations.xml`. Type labels are replaced by passing `types`.
 *
 * @property label the spoken name of the list: "Relations".
 * @property add the button that opens the add sheet: "Add relation".
 * @property sheetTitle the title of the add sheet: "Add a relation".
 * @property sheetDescription the text under the sheet's title.
 * @property type the label of the type picker: "Relation".
 * @property search the placeholder of the search field: "Search documents…".
 * @property results the heading of the results: "Matching documents".
 * @property noResults shown when no document matches: "No matching documents.".
 * @property cancel the button that closes the sheet: "Cancel".
 * @property removeText the visible text of a remove button without an icon: "Remove".
 * @property remove writes the spoken name of a remove button from the target's title.
 * @property added writes the announcement after a relation is added, from the target's title.
 * @property removed writes the announcement after a relation is removed, from the target's title.
 * @property failed shown when adding or removing failed without a message: "That didn't work. Try again.".
 * @property none shown with `showEmpty` when there are no relations: "No relations yet.".
 * @property working the spoken name of the progress while a relation is added or removed: "Working".
 */
@Immutable
public class MonarchDocumentRelationsLabels(
    public val label: String? = null,
    public val add: String? = null,
    public val sheetTitle: String? = null,
    public val sheetDescription: String? = null,
    public val type: String? = null,
    public val search: String? = null,
    public val results: String? = null,
    public val noResults: String? = null,
    public val cancel: String? = null,
    public val removeText: String? = null,
    public val remove: ((title: String) -> String)? = null,
    public val added: ((title: String) -> String)? = null,
    public val removed: ((title: String) -> String)? = null,
    public val failed: String? = null,
    public val none: String? = null,
    public val working: String? = null,
)

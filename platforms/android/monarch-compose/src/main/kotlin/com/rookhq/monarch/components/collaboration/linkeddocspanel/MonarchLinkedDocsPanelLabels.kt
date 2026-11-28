package com.rookhq.monarch.components.collaboration.linkeddocspanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchLinkedDocsPanel`. Every
 * `null` uses the built-in string named after it in `strings_linkeddocspanel.xml`.
 *
 * @property title the panel's heading: "Docs".
 * @property linkDoc the button that opens the doc menu: "Link doc".
 * @property addLink the button that opens the add-link form: "Add link".
 * @property newDoc the last item of the doc menu: "New doc".
 * @property searchDocs the placeholder of the doc menu's search field: "Search docs".
 * @property noDocs shown in the doc menu when nothing matches: "No docs match.".
 * @property empty shown without rows: "Nothing linked yet. Link a spec, runbook or notes, or add a link to any
 * page.".
 * @property emptyReadOnly shown without rows in a read-only panel: "Nothing linked yet.".
 * @property loading the name of the loading state: "Loading linked docs".
 * @property filterBySpace the name of the Space filter chips: "Filter by Space".
 * @property all the chip that shows every row: "All".
 * @property noSpace the chip of rows without a Space: "No Space".
 * @property chip a chip's text from the Space's name and its count of rows: "Engineering (2)".
 * @property hiddenByFilter the note under the rows from the count the filter hides: "2 more in other Spaces.".
 * @property showAll the button that clears the filter: "Show all".
 * @property unlink the spoken name of a row's unlink button from its title: "Unlink Release runbook".
 * @property unlinkText the visible text of the unlink button when no icon is given: "Unlink".
 * @property open the name of a row's tap action: "Open".
 * @property urlLabel the label of the address field: "Link".
 * @property urlInvalid the error under the address field: "Enter a full web address that starts with https://".
 * @property titleLabel the label of the title field: "Title (optional)".
 * @property add the form's submit button: "Add".
 * @property cancel the form's cancel button: "Cancel".
 */
@Immutable
public class MonarchLinkedDocsPanelLabels(
    public val title: String? = null,
    public val linkDoc: String? = null,
    public val addLink: String? = null,
    public val newDoc: String? = null,
    public val searchDocs: String? = null,
    public val noDocs: String? = null,
    public val empty: String? = null,
    public val emptyReadOnly: String? = null,
    public val loading: String? = null,
    public val filterBySpace: String? = null,
    public val all: String? = null,
    public val noSpace: String? = null,
    public val chip: ((space: String, count: Int) -> String)? = null,
    public val hiddenByFilter: ((count: Int) -> String)? = null,
    public val showAll: String? = null,
    public val unlink: ((title: String) -> String)? = null,
    public val unlinkText: String? = null,
    public val open: String? = null,
    public val urlLabel: String? = null,
    public val urlInvalid: String? = null,
    public val titleLabel: String? = null,
    public val add: String? = null,
    public val cancel: String? = null,
)

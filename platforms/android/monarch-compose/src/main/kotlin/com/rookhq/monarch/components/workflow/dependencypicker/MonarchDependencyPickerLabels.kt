package com.rookhq.monarch.components.workflow.dependencypicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDependencyPicker`. Every `null` keeps the built-in string
 * `monarch_dependencypicker_<key>`; [relations] replaces only the relations it names.
 *
 * @property relations the group headings and menu names of the relations.
 * @property relationLabel the label of the relation menu, "Relation".
 * @property searchLabel the label of the search field and the title of its sheet.
 * @property searchPlaceholder the placeholder of the search field.
 * @property noResults shown when the search finds nothing.
 * @property empty shown when there are no links.
 * @property remove the visible text of a remove button without an icon, "Remove".
 * @property removeItem the TalkBack name of a remove button, from the relation and the item.
 * @property added announced after adding, from the relation and the item.
 * @property removed announced after removing, from the relation and the item.
 * @property status the spoken status of a linked item, from the status's name.
 */
@Immutable
public class MonarchDependencyPickerLabels(
    public val relations: Map<MonarchDependencyRelation, String> = emptyMap(),
    public val relationLabel: String? = null,
    public val searchLabel: String? = null,
    public val searchPlaceholder: String? = null,
    public val noResults: String? = null,
    public val empty: String? = null,
    public val remove: String? = null,
    public val removeItem: ((relation: String, item: String) -> String)? = null,
    public val added: ((relation: String, item: String) -> String)? = null,
    public val removed: ((relation: String, item: String) -> String)? = null,
    public val status: ((name: String) -> String)? = null,
)

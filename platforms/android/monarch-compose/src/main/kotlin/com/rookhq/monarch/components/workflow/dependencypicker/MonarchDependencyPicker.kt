package com.rookhq.monarch.components.workflow.dependencypicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.forms.combobox.MonarchCombobox
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxLabels
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusGlyph
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchRemoveButton
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

private typealias Tokens = MonarchDependencyPickerTokens

/**
 * Lists an item's links (blocked by, blocks, related to, duplicates, duplicated by) and adds new ones: choose
 * the relation, then search for the item.
 *
 * Built by Monarch as a `Column`: one heading per relation that has links, then a row per link with the
 * item's `MonarchStatusGlyph`, key and title, and a remove button. Under the list, a `MonarchSelect` picks the
 * relation and a `MonarchCombobox` in its sheet presentation (a Material 3 `ModalBottomSheet` with a search
 * field) finds the item. Every relation is named in text, and the status is read by name, so nothing depends on
 * the glyph alone.
 *
 * The links are controlled: [onAdd] and [onRemove] ask the app to save the change, and the app updates [value].
 * Additions and removals are announced through a polite live region. Already linked items and [excludeIds]
 * (such as the item itself) are left out of the search. For a server search, pass [query] and [onQueryChange],
 * update [items] and [loading] from them; every item given is then offered.
 *
 * @param value the current links, grouped by relation in the list.
 * @param modifier the modifier of the picker.
 * @param onAdd adds a link. Without it, or with [readOnly], there are no add controls.
 * @param onRemove removes a link. Without it, or with [readOnly], rows have no remove button.
 * @param onOpen opens a linked item. Makes each row a button.
 * @param items the items to search.
 * @param excludeIds ids that cannot be linked, such as the item itself.
 * @param relations the relations offered, in order. All five by default.
 * @param defaultRelation the relation chosen at first. The first of [relations] by default.
 * @param readOnly hides the add controls and the remove buttons.
 * @param enabled whether links can be added and removed.
 * @param loading items are being fetched for the search.
 * @param query the search text, for a server search, with [onQueryChange].
 * @param onQueryChange called with the search text on every keystroke.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the buttons read
 * "Remove".
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchDependencyPicker(
    value: List<MonarchDependencyLink>,
    modifier: Modifier = Modifier,
    onAdd: ((MonarchDependencyRelation, MonarchDependencyItem) -> Unit)? = null,
    onRemove: ((MonarchDependencyLink) -> Unit)? = null,
    onOpen: ((MonarchDependencyItem) -> Unit)? = null,
    items: List<MonarchDependencyItem> = emptyList(),
    excludeIds: Set<String> = emptySet(),
    relations: List<MonarchDependencyRelation> = MonarchDependencyRelation.entries,
    defaultRelation: MonarchDependencyRelation? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
    query: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchDependencyPickerLabels = MonarchDependencyPickerLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchDependencyPickerText(labels, resources) }
    val colors = MonarchTheme.colors
    var relation by rememberSaveable { mutableStateOf(defaultRelation ?: relations.firstOrNull()) }
    var announcement by remember { mutableStateOf("") }
    val groups = Tokens.groups(value, relations)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap)) {
        if (groups.isEmpty()) {
            MonarchText(text = text.empty, color = Tokens.noteColor(colors))
        }
        groups.forEach { (r, links) ->
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.GroupGap)) {
                MonarchText(text = text.relation(r), style = MonarchTextStyle.Subheading)
                links.forEach { link ->
                    DependencyRow(
                        link = link,
                        text = text,
                        onOpen = onOpen,
                        onRemove = if (onRemove != null && !readOnly) {
                            {
                                onRemove(link)
                                announcement = text.removed(text.relation(link.relation), Tokens.itemLabel(link.item))
                            }
                        } else {
                            null
                        },
                        enabled = enabled,
                        removeIcon = removeIcon,
                    )
                }
            }
        }
        val chosen = relation
        if (!readOnly && onAdd != null && chosen != null) {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.GroupGap)) {
                MonarchSelect(
                    options = relations.map { MonarchSelectOption(it, text.relation(it)) },
                    selected = chosen,
                    onSelect = { relation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = text.relationLabel,
                    enabled = enabled,
                )
                val candidates = Tokens.candidates(items, value, excludeIds)
                MonarchCombobox(
                    label = text.searchLabel,
                    options = candidates.map(Tokens::option),
                    selected = null,
                    onSelectedChange = { id ->
                        candidates.firstOrNull { it.id == id }?.let { item ->
                            onAdd(chosen, item)
                            announcement = text.added(text.relation(chosen), Tokens.itemLabel(item))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    presentation = MonarchComboboxPresentation.Sheet,
                    enabled = enabled,
                    loading = loading,
                    placeholder = text.searchPlaceholder,
                    query = query,
                    onQueryChange = onQueryChange,
                    filter = if (onQueryChange != null) { _, _ -> true } else null,
                    labels = MonarchComboboxLabels(empty = text.noResults),
                )
            }
        }
        // Always present, so each change is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** One link: the glyph, key and title read as one element (a button with [onOpen]), and a remove button. */
@Composable
private fun DependencyRow(
    link: MonarchDependencyLink,
    text: MonarchDependencyPickerText,
    onOpen: ((MonarchDependencyItem) -> Unit)?,
    onRemove: (() -> Unit)?,
    enabled: Boolean,
    removeIcon: (@Composable () -> Unit)?,
) {
    val item = link.item
    val colors = MonarchTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.RowGap)) {
        val open = if (onOpen != null) {
            Modifier
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .clickable(role = Role.Button) { onOpen(item) }
        } else {
            Modifier
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MonarchTouchTarget.Minimum)
                .then(open)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.RowGap),
        ) {
            item.status?.let { status ->
                Box(Modifier.semantics { contentDescription = text.status(status.name) }) {
                    MonarchStatusGlyph(status.category)
                }
            }
            item.key?.let { MonarchText(text = it, style = MonarchTextStyle.Code, color = Tokens.keyColor(colors)) }
            MonarchText(text = item.title, modifier = Modifier.weight(1f))
        }
        if (onRemove != null) {
            MonarchRemoveButton(
                name = text.removeItem(text.relation(link.relation), Tokens.itemLabel(item)),
                text = text.remove,
                onClick = onRemove,
                enabled = enabled,
                icon = removeIcon,
            )
        }
    }
}

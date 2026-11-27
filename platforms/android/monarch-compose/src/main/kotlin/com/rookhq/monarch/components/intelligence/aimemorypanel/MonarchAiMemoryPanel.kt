package com.rookhq.monarch.components.intelligence.aimemorypanel

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkAction
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkActionBar
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart
import com.rookhq.monarch.components.intelligence.aimemorychip.MonarchAiMemoryChip
import com.rookhq.monarch.components.intelligence.aimemorychip.MonarchAiMemoryChipVariant
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.theme.MonarchTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Everything the assistant remembers, grouped by the person, organization or
 * project it is about, for a settings or privacy screen. People can search,
 * edit a memory, forget one, or select several and forget them together
 * after a confirmation.
 *
 * A product pattern composed from Monarch parts: a `LazyColumn` with
 * `MonarchSearchBar`, a select-all row and one section per entity whose
 * heading has a three-state `MonarchCheckbox`; each memory is a
 * `MonarchAiMemoryChip` row (tap for Edit and Forget) with its own checkbox
 * and its last-updated date. While memories are selected, a
 * `MonarchBulkActionBar` under the list offers Forget, which asks in an
 * `MonarchDialog` first. Searches, edits and forgets are announced politely.
 *
 * The list is lazy and scrolls; give the panel a bounded height (a screen).
 * Selection and the search are kept across configuration changes; the
 * memories are the app's state: remove or change them in the callbacks.
 *
 * @param memories everything remembered.
 * @param onEdit saves an edited memory, with its id and the trimmed new text.
 * @param onForget forgets one memory, straight away: the row's Forget does not confirm.
 * @param onForgetMany forgets the selected memories, after the person confirms.
 * @param modifier the modifier of the panel.
 * @param locale the locale for search, sorting and dates. `null` uses the first locale of the configuration.
 * @param timeZone the time zone of the dates. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device does
 * not know, is the device's zone.
 * @param state the scroll state of the list.
 * @param contentPadding the padding around the list content.
 * @param searchIcon the search glyph at the start of the field. Monarch bundles no icons.
 * @param clearIcon the glyph of the clear button of the field; without it there is no clear button.
 * @param labels replacements for the built-in strings, the row strings and the bulk bar strings.
 * @param initiallySelected ids selected when the panel first appears, so the bar with Forget selected shows.
 * @param editIcon an icon for the edit button at the end of each memory row; with it or [forgetIcon] the rows show
 * buttons instead of an options menu.
 * @param forgetIcon an icon for the forget button of each memory row.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiMemoryPanel(
    memories: List<MonarchAiMemoryItem>,
    onEdit: (id: String, text: String) -> Unit,
    onForget: (id: String) -> Unit,
    onForgetMany: (ids: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    timeZone: String? = null,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    searchIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Search,
    clearIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchAiMemoryPanelLabels = MonarchAiMemoryPanelLabels(),
    initiallySelected: List<String> = emptyList(),
    editIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Pencil,
    forgetIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Trash,
) {
    val tokens = MonarchAiMemoryPanelTokens
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val text = PanelText(LocalResources.current, labels)
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(initiallySelected) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var announcement by remember { mutableStateOf("") }
    val groups = remember(memories, query, resolvedLocale) { tokens.groups(memories, query, resolvedLocale) }
    val visibleIds = groups.flatMap { it.ids }
    val existing = memories.map { it.id }.toSet()
    val selectedIds = selected.filter { it in existing }
    val zone = monarchTimeZone(timeZone)
    val dates = remember(resolvedLocale, zone) {
        val format = DateFormat.getDateInstance(DateFormat.MEDIUM, resolvedLocale)
        format.timeZone = zone
        format
    }
    val panel = PanelState(
        selected = selectedIds,
        onSelect = { selected = it },
        text = text,
        format = { millis -> dates.format(Date(millis)) },
        onEdit = { id, new ->
            onEdit(id, new)
            announcement = text.edited
        },
        onForget = { id ->
            selected = selected - id
            onForget(id)
            announcement = text.forgotten(1)
        },
        editIcon = editIcon,
        forgetIcon = forgetIcon,
    )
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(tokens.RowGap),
        ) {
            if (memories.isNotEmpty()) {
                item(key = "search") {
                    MonarchSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { q ->
                            val count = memories.count { tokens.matches(it, q, resolvedLocale) }
                            announcement = if (q.isBlank()) "" else text.matches(count)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        debounceMillis = tokens.SEARCH_DEBOUNCE_MILLIS,
                        placeholder = text.search,
                        leadingIcon = searchIcon,
                        clearIcon = clearIcon,
                    )
                }
                item(key = "all") { SelectAllRow(visibleIds, panel) }
            }
            when {
                memories.isEmpty() -> item(key = "empty") { Message(text.empty) }
                groups.isEmpty() -> item(key = "none") { Message(text.noMatches(query.trim())) }
                else -> groups.forEach { group -> groupItems(group, panel) }
            }
        }
        MonarchBulkActionBar(
            count = selectedIds.size,
            onClear = { selected = emptyList() },
            totalCount = memories.size,
            actions = listOf(
                MonarchBulkAction(id = "forget", title = text.forget, destructive = true) { confirming = true },
            ),
            labels = labels.bulkBar,
        )
        // Always present, so TalkBack announces search results, edits and forgets.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    if (confirming && selectedIds.isNotEmpty()) {
        ConfirmForget(
            count = selectedIds.size,
            text = text,
            onConfirm = {
                onForgetMany(selectedIds)
                selected = selected - selectedIds.toSet()
                announcement = text.forgotten(selectedIds.size)
                confirming = false
            },
            onDismiss = { confirming = false },
        )
    }
}

/** The strings of the panel, resolved from the labels or the resources. */
private class PanelText(private val resources: Resources, private val labels: MonarchAiMemoryPanelLabels) {
    val search: String = labels.search ?: resources.getString(R.string.monarch_aimemorypanel_search)
    val empty: String = labels.empty ?: resources.getString(R.string.monarch_aimemorypanel_empty)
    val forget: String = labels.forget ?: resources.getString(R.string.monarch_aimemorypanel_forget)
    val cancel: String = labels.cancel ?: resources.getString(R.string.monarch_aimemorypanel_cancel)
    val edited: String = labels.edited ?: resources.getString(R.string.monarch_aimemorypanel_edited)
    val confirmMessage: String =
        labels.confirmMessage ?: resources.getString(R.string.monarch_aimemorypanel_confirm_message)
    val chip = labels.chip

    fun selectAll(shown: Int): String =
        labels.selectAll?.invoke(shown) ?: resources.getString(R.string.monarch_aimemorypanel_select_all, shown)

    fun selectGroup(name: String): String =
        labels.selectGroup?.invoke(name) ?: resources.getString(R.string.monarch_aimemorypanel_select_group, name)

    fun select(memory: String): String {
        val short = MonarchAiMemoryPanelTokens.shortText(memory)
        return labels.select?.invoke(short) ?: resources.getString(R.string.monarch_aimemorypanel_select, short)
    }

    fun kind(kind: MonarchAiMemoryEntityKind): String = labels.kind?.invoke(kind) ?: resources.getString(
        when (kind) {
            MonarchAiMemoryEntityKind.Person -> R.string.monarch_aimemorypanel_kind_person
            MonarchAiMemoryEntityKind.Organization -> R.string.monarch_aimemorypanel_kind_organization
            MonarchAiMemoryEntityKind.Project -> R.string.monarch_aimemorypanel_kind_project
            MonarchAiMemoryEntityKind.Other -> R.string.monarch_aimemorypanel_kind_other
        },
    )

    fun count(n: Int): String =
        labels.count?.invoke(n) ?: resources.getQuantityString(R.plurals.monarch_aimemorypanel_count, n, n)

    fun updated(date: String): String =
        labels.updated?.invoke(date) ?: resources.getString(R.string.monarch_aimemorypanel_updated, date)

    fun noMatches(query: String): String =
        labels.noMatches?.invoke(query) ?: resources.getString(R.string.monarch_aimemorypanel_no_matches, query)

    fun matches(n: Int): String =
        labels.matches?.invoke(n) ?: resources.getQuantityString(R.plurals.monarch_aimemorypanel_matches, n, n)

    fun confirmTitle(n: Int): String =
        labels.confirmTitle?.invoke(n)
            ?: resources.getQuantityString(R.plurals.monarch_aimemorypanel_confirm_title, n, n)

    fun forgotten(n: Int): String =
        labels.forgotten?.invoke(n)
            ?: resources.getQuantityString(R.plurals.monarch_aimemorypanel_forgotten, n, n)
}

/** What the rows need from the panel. */
private class PanelState(
    val selected: List<String>,
    val onSelect: (List<String>) -> Unit,
    val text: PanelText,
    val format: (Long) -> String,
    val onEdit: (String, String) -> Unit,
    val onForget: (String) -> Unit,
    val editIcon: (@Composable () -> Unit)?,
    val forgetIcon: (@Composable () -> Unit)?,
)

private fun LazyListScope.groupItems(group: MonarchAiMemoryPanelGroup, panel: PanelState) {
    item(key = "group:${group.entity.kind}:${group.entity.name}") { GroupHeader(group, panel) }
    items(group.items, key = { "memory:${it.id}" }) { memory -> MemoryRow(memory, panel) }
}

@Composable
private fun SelectAllRow(visibleIds: List<String>, panel: PanelState) {
    val state = MonarchAiMemoryPanelTokens.checkState(panel.selected, visibleIds)
    val enabled = visibleIds.isNotEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiMemoryPanelTokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MonarchTouchTarget.Minimum)
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .triStateToggleable(
                    state = state,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onClick = { panel.onSelect(MonarchAiMemoryPanelTokens.toggled(panel.selected, visibleIds, state)) },
                ),
            horizontalArrangement = Arrangement.spacedBy(MonarchAiMemoryPanelTokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchCheckbox(state = state, onClick = null, enabled = enabled)
            Text(
                text = panel.text.selectAll(visibleIds.size),
                style = MonarchTheme.typography.body,
                color = MonarchAiMemoryPanelTokens.colors(MonarchTheme.colors).heading,
            )
        }
        // As the web: the action sits here, off, until a memory is selected (then the bar under the list takes over).
        if (panel.selected.isEmpty()) {
            MonarchButton(
                text = panel.text.forget,
                onClick = {},
                variant = MonarchButtonVariant.Danger,
                size = MonarchButtonSize.Sm,
                enabled = false,
            )
        }
    }
}

@Composable
private fun GroupHeader(group: MonarchAiMemoryPanelGroup, panel: PanelState) {
    val tokens = MonarchAiMemoryPanelTokens
    val colors = tokens.colors(MonarchTheme.colors)
    val state = tokens.checkState(panel.selected, group.ids)
    val groupLabel = panel.text.selectGroup(group.entity.name)
    Row(modifier = Modifier.padding(top = tokens.Gap), verticalAlignment = Alignment.CenterVertically) {
        MonarchCheckbox(
            state = state,
            onClick = { panel.onSelect(tokens.toggled(panel.selected, group.ids, state)) },
            modifier = Modifier.semantics { contentDescription = groupLabel },
        )
        Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            Text(
                text = group.entity.name,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.bodyStrong,
                color = colors.heading,
            )
            Text(
                text = "${panel.text.kind(group.entity.kind)} · ${panel.text.count(group.items.size)}",
                style = MonarchTheme.typography.caption,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun MemoryRow(memory: MonarchAiMemoryItem, panel: PanelState) {
    val tokens = MonarchAiMemoryPanelTokens
    val checked = memory.id in panel.selected
    val selectLabel = panel.text.select(memory.text)
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonarchCheckbox(
            checked = checked,
            onCheckedChange = { on ->
                panel.onSelect(if (on) panel.selected + memory.id else panel.selected - memory.id)
            },
            modifier = Modifier.semantics { contentDescription = selectLabel },
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            MonarchAiMemoryChip(
                memory = MonarchAiMemoryPart(memory.id, MonarchAiMemoryAction.Write, memory.text),
                variant = MonarchAiMemoryChipVariant.Row,
                labels = panel.text.chip,
                onEdit = panel.onEdit,
                onForget = panel.onForget,
                editIcon = panel.editIcon,
                forgetIcon = panel.forgetIcon,
            )
            Text(
                text = panel.text.updated(panel.format(memory.updatedAt)),
                style = MonarchTheme.typography.caption,
                color = tokens.colors(MonarchTheme.colors).muted,
            )
        }
    }
}

@Composable
private fun Message(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(vertical = MonarchAiMemoryPanelTokens.EmptyPadding),
        style = MonarchTheme.typography.body,
        color = MonarchAiMemoryPanelTokens.colors(MonarchTheme.colors).muted,
    )
}

@Composable
private fun ConfirmForget(count: Int, text: PanelText, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = text.confirmTitle(count),
        description = text.confirmMessage,
        actions = {
            MonarchButton(text = text.cancel, onClick = onDismiss, variant = MonarchButtonVariant.Ghost)
            MonarchButton(text = text.forget, onClick = onConfirm, variant = MonarchButtonVariant.Danger)
        },
    )
}

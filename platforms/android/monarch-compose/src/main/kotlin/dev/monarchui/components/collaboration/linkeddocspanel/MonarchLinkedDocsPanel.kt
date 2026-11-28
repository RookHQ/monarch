package dev.monarchui.components.collaboration.linkeddocspanel

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.datadisplay.tag.MonarchTagSize
import dev.monarchui.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIconSize
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.layout.sectioncard.MonarchSectionCard
import dev.monarchui.components.layout.sectioncard.MonarchSectionCardLabels
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The panel of docs and links attached to a record (a ticket, a task, a
 * campaign): rows that open, Space filter chips, unlink, link a doc from a
 * searchable menu, and add an external link.
 *
 * Built on `MonarchSectionCard`, whose loading and error states show while
 * [items] is `null`. Each row is one TalkBack element, a button read as the
 * title, the secondary line and the status, with a `MonarchFileTypeIcon`
 * that is decorative because the text says what the row is; the unlink
 * button is a separate element named "Unlink" and the title. The Link doc
 * button opens a `MonarchMenu` with a `MonarchSearchBar`, the docs not yet
 * linked and New doc. Add link opens an inline form that accepts only full
 * http or https addresses. When rows span more than one Space, `MonarchTag`
 * chips filter them. The panel never fetches or navigates: the app supplies
 * the rows and does the opening, linking and unlinking.
 *
 * The rows are not lazy: a record has a handful of links, and the panel sits
 * inside a scrolling detail screen.
 *
 * @param items what is linked to the record, or `null` while loading.
 * @param onOpen called when a row is tapped. Open the doc or the address.
 * @param modifier the modifier of the panel.
 * @param readOnly hides linking, adding and unlinking.
 * @param errorMessage something went wrong. Without rows it replaces them; with rows it shows under them.
 * @param onRetry shows a retry button with [errorMessage].
 * @param notice replaces the rows and the actions, for example "You don't have access to docs in this
 * workspace.".
 * @param docOptions the docs the Link doc menu offers. Docs already linked (by `docId`) are left out.
 * @param onLinkDoc links the chosen doc. Shows Link doc with [docOptions] or [onDocQueryChange].
 * @param onDocQueryChange called with the menu's search text, to search docs on the server and update
 * [docOptions]. The menu also filters [docOptions] itself.
 * @param onCreateDoc adds New doc to the menu.
 * @param onAddLink adds an external link with its address and title (the host name when left empty).
 * Shows Add link.
 * @param onUnlink unlinks a row. Shows an unlink button on rows that are `unlinkable`.
 * @param unlinkingId the id of the row being unlinked: its button shows progress and the others are disabled.
 * @param spaceFilter shows Space filter chips when the rows span more than one Space.
 * @param unlinkIcon the unlink button's icon, for example a close mark. Without it the button shows "Unlink".
 * Monarch bundles no icons.
 * @param linkDocIcon the icon before "Link doc", a document by default.
 * @param addLinkIcon the icon before "Add link", a link by default.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchLinkedDocsPanel(
    items: List<MonarchLinkedDocsPanelItem>?,
    onOpen: (MonarchLinkedDocsPanelItem) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    notice: String? = null,
    docOptions: List<MonarchLinkedDocsPanelOption> = emptyList(),
    onLinkDoc: ((docId: String) -> Unit)? = null,
    onDocQueryChange: ((query: String) -> Unit)? = null,
    onCreateDoc: (() -> Unit)? = null,
    onAddLink: ((url: String, title: String) -> Unit)? = null,
    onUnlink: ((MonarchLinkedDocsPanelItem) -> Unit)? = null,
    unlinkingId: String? = null,
    spaceFilter: Boolean = true,
    unlinkIcon: (@Composable () -> Unit)? = null,
    labels: MonarchLinkedDocsPanelLabels = MonarchLinkedDocsPanelLabels(),
    linkDocIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Doc,
    addLinkIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Link,
) {
    val tokens = MonarchLinkedDocsPanelTokens
    var adding by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(tokens.ALL) }
    val all = items.orEmpty()
    val editable = !readOnly && notice == null
    val linked = all.filter { it.kind == MonarchLinkedDocsPanelKind.Doc }.mapNotNull { it.docId }.toSet()
    val hasDocs = docOptions.isNotEmpty() || onDocQueryChange != null
    val canLinkDoc = editable && hasDocs && onLinkDoc != null
    val canAdd = editable && onAddLink != null
    MonarchSectionCard(
        title = monarchString(labels.title, R.string.monarch_linkeddocspanel_title),
        modifier = modifier,
        meta = all.size.takeIf { it > 0 }?.toString(),
        state = tokens.cardState(loaded = items != null, failed = errorMessage != null),
        errorMessage = errorMessage,
        onRetry = onRetry,
        actions = if (canLinkDoc || canAdd) {
            {
                HeaderActions(
                    onLinkDoc = onLinkDoc.takeIf { canLinkDoc },
                    docMenu = { link ->
                        MonarchLinkedDocsPanelDocMenu(
                            docOptions, linked, labels, link, onDocQueryChange, onCreateDoc, linkDocIcon,
                        )
                    },
                    onToggleAdd = { adding = !adding }.takeIf { canAdd },
                    labels = labels,
                    addLinkIcon = addLinkIcon,
                )
            }
        } else {
            null
        },
        labels = MonarchSectionCardLabels(
            loading = monarchString(labels.loading, R.string.monarch_linkeddocspanel_loading),
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            if (adding && canAdd) {
                MonarchLinkedDocsPanelAddLinkForm(labels, onAddLink) { adding = false }
            }
            if (notice != null) {
                MonarchText(text = notice, color = tokens.mutedText(MonarchTheme.colors))
            } else {
                PanelRows(
                    all = all,
                    filter = filter,
                    onFilterChange = { filter = it },
                    spaceFilter = spaceFilter,
                    readOnly = readOnly,
                    onOpen = onOpen,
                    onUnlink = onUnlink?.takeIf { !readOnly },
                    unlinkingId = unlinkingId,
                    unlinkIcon = unlinkIcon,
                    labels = labels,
                )
            }
            if (items != null && errorMessage != null) InlineError(errorMessage, onRetry)
        }
    }
}

/** The Space chips, the rows or the empty text, and the note about rows the filter hides. */
@Composable
private fun PanelRows(
    all: List<MonarchLinkedDocsPanelItem>,
    filter: String,
    onFilterChange: (String) -> Unit,
    spaceFilter: Boolean,
    readOnly: Boolean,
    onOpen: (MonarchLinkedDocsPanelItem) -> Unit,
    onUnlink: ((MonarchLinkedDocsPanelItem) -> Unit)?,
    unlinkingId: String?,
    unlinkIcon: (@Composable () -> Unit)?,
    labels: MonarchLinkedDocsPanelLabels,
) {
    val tokens = MonarchLinkedDocsPanelTokens
    val chips = tokens.chips(all)
    val showChips = spaceFilter && chips.size > 1
    val active = tokens.activeKey(filter, chips, showChips)
    val shown = tokens.shown(all, active)
    if (showChips) SpaceChips(chips, active, all.size, labels, onFilterChange)
    if (all.isEmpty()) {
        MonarchText(
            text = if (readOnly) {
                monarchString(labels.emptyReadOnly, R.string.monarch_linkeddocspanel_empty_read_only)
            } else {
                monarchString(labels.empty, R.string.monarch_linkeddocspanel_empty)
            },
            color = tokens.mutedText(MonarchTheme.colors),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(MonarchShapeDefaults.BorderWidth, MonarchTheme.colors.borderDefault, MonarchTheme.shapes.md)
            .padding(horizontal = MonarchTheme.spacing.space3),
    ) {
        shown.forEachIndexed { index, item ->
            if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
            LinkedDocRow(
                item = item,
                onOpen = { onOpen(item) },
                onUnlink = onUnlink?.takeIf { item.unlinkable }?.let { { it(item) } },
                unlinkingId = unlinkingId,
                unlinkIcon = unlinkIcon,
                labels = labels,
            )
        }
    }
    val hidden = all.size - shown.size
    if (hidden > 0) HiddenNote(hidden, labels) { onFilterChange(tokens.ALL) }
}

/** Link doc and Add link, each when the panel offers it. */
@Composable
private fun HeaderActions(
    onLinkDoc: ((docId: String) -> Unit)?,
    docMenu: @Composable (link: (docId: String) -> Unit) -> Unit,
    onToggleAdd: (() -> Unit)?,
    labels: MonarchLinkedDocsPanelLabels,
    addLinkIcon: (@Composable () -> Unit)?,
) {
    if (onLinkDoc != null) docMenu(onLinkDoc)
    if (onToggleAdd != null) {
        MonarchButton(
            text = monarchString(labels.addLink, R.string.monarch_linkeddocspanel_add_link),
            onClick = onToggleAdd,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            leadingIcon = addLinkIcon,
        )
    }
}

/** An error while rows are shown: with a retry button when the app can retry. */
@Composable
private fun InlineError(message: String, onRetry: (() -> Unit)?) {
    if (onRetry != null) {
        MonarchInlineErrorRetry(message = message, onRetry = onRetry)
    } else {
        MonarchInlineNotice(message = message, tone = MonarchTone.Danger)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpaceChips(
    chips: List<MonarchLinkedDocsPanelTokens.Chip>,
    active: String,
    total: Int,
    labels: MonarchLinkedDocsPanelLabels,
    onSelect: (String) -> Unit,
) {
    val tokens = MonarchLinkedDocsPanelTokens
    val groupName = monarchString(labels.filterBySpace, R.string.monarch_linkeddocspanel_filter_by_space)
    val noSpace = monarchString(labels.noSpace, R.string.monarch_linkeddocspanel_no_space)
    val format = stringResource(R.string.monarch_linkeddocspanel_chip)
    val chipText: (String, Int) -> String = labels.chip ?: { name, count -> format.format(name, count) }
    FlowRow(
        modifier = Modifier.semantics {
            isTraversalGroup = true
            contentDescription = groupName
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.PartGap),
    ) {
        MonarchTag(
            text = chipText(monarchString(labels.all, R.string.monarch_linkeddocspanel_all), total),
            selected = active == tokens.ALL,
            onSelectedChange = { onSelect(tokens.ALL) },
            size = MonarchTagSize.Sm,
        )
        chips.forEach { chip ->
            MonarchTag(
                text = chipText(chip.name ?: noSpace, chip.count),
                selected = active == chip.key,
                onSelectedChange = { onSelect(chip.key) },
                size = MonarchTagSize.Sm,
            )
        }
    }
}

@Composable
private fun LinkedDocRow(
    item: MonarchLinkedDocsPanelItem,
    onOpen: () -> Unit,
    onUnlink: (() -> Unit)?,
    unlinkingId: String?,
    unlinkIcon: (@Composable () -> Unit)?,
    labels: MonarchLinkedDocsPanelLabels,
) {
    val tokens = MonarchLinkedDocsPanelTokens
    val shape = MonarchShapeDefaults.Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .monarchMinimumTouchTarget()
                .monarchFocusRing(shape)
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    onClickLabel = monarchString(labels.open, R.string.monarch_linkeddocspanel_open),
                    role = Role.Button,
                    onClick = onOpen,
                )
                .semantics(mergeDescendants = true) { }
                .padding(vertical = tokens.RowPaddingVertical),
            horizontalArrangement = Arrangement.spacedBy(tokens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.fileName != null) {
                MonarchFileTypeIcon(
                    family = tokens.familyOf(item),
                    size = MonarchFileTypeIconSize.Sm,
                    decorative = true,
                )
            } else {
                val glyph = if (item.kind == MonarchLinkedDocsPanelKind.Doc) {
                    MonarchDefaultIcons.Doc
                } else {
                    MonarchDefaultIcons.Link
                }
                CompositionLocalProvider(LocalContentColor provides tokens.mutedText(MonarchTheme.colors)) { glyph() }
            }
            Column(modifier = Modifier.weight(1f)) {
                MonarchText(text = item.title, style = MonarchTextStyle.BodyStrong)
                tokens.metaOf(item)?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = tokens.mutedText(MonarchTheme.colors),
                    )
                }
            }
            item.status?.let { MonarchBadge(text = it) }
        }
        if (onUnlink != null) {
            UnlinkButton(item, onUnlink, unlinkingId, unlinkIcon, labels)
        }
    }
}

@Composable
private fun UnlinkButton(
    item: MonarchLinkedDocsPanelItem,
    onUnlink: () -> Unit,
    unlinkingId: String?,
    unlinkIcon: (@Composable () -> Unit)?,
    labels: MonarchLinkedDocsPanelLabels,
) {
    val name = labels.unlink?.invoke(item.title)
        ?: stringResource(R.string.monarch_linkeddocspanel_unlink, item.title)
    val busy = unlinkingId == item.id
    val enabled = unlinkingId == null
    if (unlinkIcon != null && !busy) {
        MonarchIconButton(label = name, onClick = onUnlink, enabled = enabled, icon = unlinkIcon)
    } else {
        MonarchButton(
            text = monarchString(labels.unlinkText, R.string.monarch_linkeddocspanel_unlink_text),
            onClick = onUnlink,
            modifier = Modifier.semantics { contentDescription = name },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            loading = busy,
        )
    }
}

@Composable
private fun HiddenNote(hidden: Int, labels: MonarchLinkedDocsPanelLabels, onShowAll: () -> Unit) {
    val text = labels.hiddenByFilter?.invoke(hidden)
        ?: pluralStringResource(R.plurals.monarch_linkeddocspanel_hidden_by_filter, hidden, hidden)
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchLinkedDocsPanelTokens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            style = MonarchTextStyle.Caption,
            color = MonarchLinkedDocsPanelTokens.mutedText(MonarchTheme.colors),
        )
        MonarchButton(
            text = monarchString(labels.showAll, R.string.monarch_linkeddocspanel_show_all),
            onClick = onShowAll,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}

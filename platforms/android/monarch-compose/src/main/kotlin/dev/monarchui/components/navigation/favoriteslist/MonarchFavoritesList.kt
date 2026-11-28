package dev.monarchui.components.navigation.favoriteslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchRemovalFocus
import dev.monarchui.internal.monarchRemovalFallback
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRemovalFocus
import dev.monarchui.theme.MonarchTheme

/**
 * A navigation section of the person's pinned destinations (projects, views,
 * documents) for a navigation drawer or a rail, with an Edit button that turns
 * on reorder and remove.
 *
 * Rows are Material 3 `NavigationDrawerItem`s, so the section sits in a
 * `ModalDrawerSheet` or `PermanentDrawerSheet` next to the app's other
 * destinations. The current destination is selected, on the subtle accent
 * fill and in semibold. With [onRemove] or [onReorder] the heading has an Edit
 * button. In edit mode, with [onReorder], the rows become a
 * `MonarchReorderableList` (long press and drag, or the handle's Move up and
 * Move down actions); with [onRemove] each row has a remove button. Outside
 * edit mode [onRemove] is also offered to TalkBack as a custom action on each
 * row. The list holds no data: the app stores the favourites and updates
 * [items] from the callbacks.
 *
 * Monarch bundles no icons, so the star before the heading and the remove
 * buttons' icon come from [headingIcon] and [removeIcon]; without
 * [removeIcon] the remove button shows "Remove".
 *
 * @param items the pinned destinations, in order.
 * @param onSelect opens a destination when its row is tapped outside edit mode.
 * @param modifier the modifier of the section.
 * @param editing whether edit mode is on. `null` lets the section keep this itself, starting off.
 * @param onEditingChange called with the new state when the Edit or Done button is tapped, and with `false`
 * when the last item is removed.
 * @param emptyMessage shown under the heading when there are no items. Without it the section draws nothing
 * when empty.
 * @param onRemove removes an item. With it rows can be removed in edit mode and through TalkBack's actions.
 * @param onReorder receives the new order after a move. With it edit mode lets people drag or move rows.
 * @param headingIcon an icon before the heading, such as a star. Decorative.
 * @param removeIcon makes the remove buttons icon buttons with this icon. They keep their spoken name.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchFavoritesList(
    items: List<MonarchFavoriteItem>,
    onSelect: (MonarchFavoriteItem) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean? = null,
    onEditingChange: ((Boolean) -> Unit)? = null,
    emptyMessage: String? = null,
    onRemove: ((MonarchFavoriteItem) -> Unit)? = null,
    onReorder: ((List<MonarchFavoriteItem>) -> Unit)? = null,
    headingIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Star,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchFavoritesListLabels = MonarchFavoritesListLabels(),
) {
    val tokens = MonarchFavoritesListTokens
    var ownEditing by rememberSaveable { mutableStateOf(false) }
    val editable = tokens.editable(onRemove != null, onReorder != null)
    val requested = editing ?: ownEditing
    val inEdit = tokens.inEdit(editable, requested, items.size)
    val currentOnEditingChange by rememberUpdatedState(onEditingChange)
    val setEditing: (Boolean) -> Unit = { next ->
        ownEditing = next
        currentOnEditingChange?.invoke(next)
    }
    var announcement by remember { mutableStateOf("") }
    // The same text twice in a row is still announced.
    val announce: (String) -> Unit = { text -> announcement = if (text == announcement) "$text\u00A0" else text }
    // As on the web: after a removal focus goes to the next row's remove button, else the previous one's, else
    // the Edit/Done button.
    val removalFocus = rememberMonarchRemovalFocus(items.map { it.id })
    val removal = favoritesRemoval(onRemove, labels, announce) { removalFocus.removing(items.map { it.id }, it.id) }
    // Removing the last item ends edit mode.
    val endEditing = items.isEmpty() && requested
    LaunchedEffect(endEditing) { if (endEditing) setEditing(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (items.isNotEmpty() || emptyMessage != null) {
            FavoritesHeading(
                title = monarchString(labels.title, R.string.monarch_favoriteslist_title),
                icon = headingIcon,
            ) {
                if (editable && items.isNotEmpty()) {
                    EditButton(inEdit, labels, Modifier.monarchRemovalFallback(removalFocus)) { setEditing(!inEdit) }
                }
            }
        }
        FavoritesRows(items, onSelect, removal.remove, removeIcon, labels, removal.name, removalFocus)
            .Body(inEdit = inEdit, emptyMessage = emptyMessage, onReorder = onReorder)
        Announcement(announcement)
    }
}

/** A polite live region that stays in the tree, so each new [text] is announced. */
@Composable
private fun Announcement(text: String) {
    MonarchLiveStatus(text = text.takeIf { it.isNotEmpty() }, visible = false)
}

/** The spoken name of a row's removal and the removal itself, which announces what was removed. */
private class FavoritesRemoval(val name: (String) -> String, val remove: ((MonarchFavoriteItem) -> Unit)?)

@Composable
private fun favoritesRemoval(
    onRemove: ((MonarchFavoriteItem) -> Unit)?,
    labels: MonarchFavoritesListLabels,
    announce: (String) -> Unit,
    willRemove: (MonarchFavoriteItem) -> Unit,
): FavoritesRemoval {
    val removedFormat = stringResource(R.string.monarch_favoriteslist_removed)
    val nameFormat = stringResource(R.string.monarch_favoriteslist_remove_label)
    val name: (String) -> String = { title -> labels.removeLabel?.invoke(title) ?: nameFormat.format(title) }
    val remove: ((MonarchFavoriteItem) -> Unit)? = onRemove?.let { callback ->
        { item ->
            announce(labels.removed?.invoke(item.title) ?: removedFormat.format(item.title))
            willRemove(item)
            callback(item)
        }
    }
    return FavoritesRemoval(name, remove)
}

@Composable
private fun EditButton(inEdit: Boolean, labels: MonarchFavoritesListLabels, modifier: Modifier, onClick: () -> Unit) {
    val done = monarchString(labels.done, R.string.monarch_favoriteslist_done)
    val edit = monarchString(labels.edit, R.string.monarch_favoriteslist_edit)
    val editName = monarchString(labels.editLabel, R.string.monarch_favoriteslist_edit_label)
    MonarchButton(
        text = if (inEdit) done else edit,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = if (inEdit) done else editName },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
    )
}

@Composable
private fun EmptyMessage(text: String) {
    MonarchText(
        text = text,
        modifier = Modifier.padding(horizontal = MonarchFavoritesListTokens.HeadingPadding),
        style = MonarchTextStyle.Caption,
        color = MonarchFavoritesListTokens.mutedColor(MonarchTheme.colors),
    )
}

/** The rows of the list in its three forms, sharing what they need. */
private class FavoritesRows(
    val items: List<MonarchFavoriteItem>,
    val onSelect: (MonarchFavoriteItem) -> Unit,
    val onRemove: ((MonarchFavoriteItem) -> Unit)?,
    val removeIcon: (@Composable () -> Unit)?,
    val labels: MonarchFavoritesListLabels,
    val removeName: (String) -> String,
    val removalFocus: MonarchRemovalFocus<String>,
) {
    @Composable
    fun Body(inEdit: Boolean, emptyMessage: String?, onReorder: ((List<MonarchFavoriteItem>) -> Unit)?) {
        when {
            items.isEmpty() -> emptyMessage?.let { EmptyMessage(it) }
            inEdit && onReorder != null -> Reorderable { from, to ->
                onReorder(MonarchFavoritesListTokens.moved(items, from, to))
            }
            inEdit -> Editing()
            else -> Browsing()
        }
    }

    @Composable
    fun Browsing() {
        items.forEach { item ->
            FavoriteRow(item = item, onSelect = onSelect, removeName = removeName, onRemove = onRemove)
        }
    }

    @Composable
    fun Editing() {
        Column {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MonarchTouchTarget.Minimum)
                        .padding(start = MonarchFavoritesListTokens.HeadingPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) { EditRowContent(item, onRemove, removeIcon, labels, removeName, removalFocus.requester(item.id)) }
            }
        }
    }

    @Composable
    fun Reorderable(onMove: (from: Int, to: Int) -> Unit) {
        MonarchReorderableList(
            items = items,
            key = { it.id },
            itemLabel = { it.title },
            onReorder = onMove,
            modifier = Modifier.heightIn(max = MonarchFavoritesListTokens.EditMaxHeight),
            labels = labels.reorder,
        ) { item, _ ->
            EditRowContent(item, onRemove, removeIcon, labels, removeName, removalFocus.requester(item.id))
        }
    }
}

@Composable
private fun FavoritesHeading(title: String, icon: (@Composable () -> Unit)?, action: @Composable () -> Unit) {
    val tokens = MonarchFavoritesListTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .padding(start = tokens.HeadingPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(Modifier.size(tokens.HeadingIconSize).clearAndSetSemantics { }) { icon() }
            }
            // Shown in capitals as on the web; the spoken and searchable text stays the title as given.
            MonarchText(
                text = title.uppercase(),
                modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(title) },
                style = MonarchTextStyle.Label,
                color = tokens.mutedColor(MonarchTheme.colors),
            )
        }
        action()
    }
}

@Composable
private fun FavoriteRow(
    item: MonarchFavoriteItem,
    onSelect: (MonarchFavoriteItem) -> Unit,
    removeName: (String) -> String,
    onRemove: ((MonarchFavoriteItem) -> Unit)?,
) {
    val tokens = MonarchFavoritesListTokens
    val selected = tokens.rowColors(current = true, colors = MonarchTheme.colors)
    val resting = tokens.rowColors(current = false, colors = MonarchTheme.colors)
    val actionName = removeName(item.title)
    NavigationDrawerItem(
        label = { Text(text = item.title, fontWeight = tokens.titleWeight(item.current)) },
        selected = item.current,
        onClick = { onSelect(item) },
        modifier = Modifier.semantics {
            if (onRemove != null) {
                customActions = listOf(
                    CustomAccessibilityAction(actionName) {
                        onRemove(item)
                        true
                    },
                )
            }
        },
        icon = item.icon?.let { icon -> { Box(Modifier.clearAndSetSemantics { }) { icon() } } },
        shape = MonarchTheme.shapes.md,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = selected.container,
            unselectedContainerColor = resting.container,
            selectedIconColor = selected.content,
            unselectedIconColor = resting.content,
            selectedTextColor = selected.content,
            unselectedTextColor = resting.content,
        ),
    )
}

@Composable
private fun RowScope.EditRowContent(
    item: MonarchFavoriteItem,
    onRemove: ((MonarchFavoriteItem) -> Unit)?,
    removeIcon: (@Composable () -> Unit)?,
    labels: MonarchFavoritesListLabels,
    removeName: (String) -> String,
    focus: FocusRequester,
) {
    MonarchText(text = item.title, modifier = Modifier.weight(1f))
    if (onRemove != null) {
        val name = removeName(item.title)
        if (removeIcon != null) {
            MonarchIconButton(
                label = name,
                onClick = { onRemove(item) },
                modifier = Modifier.focusRequester(focus),
            ) {
                Box(Modifier.clearAndSetSemantics { }) { removeIcon() }
            }
        } else {
            MonarchButton(
                text = monarchString(labels.remove, R.string.monarch_favoriteslist_remove),
                onClick = { onRemove(item) },
                modifier = Modifier.focusRequester(focus).semantics { contentDescription = name },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

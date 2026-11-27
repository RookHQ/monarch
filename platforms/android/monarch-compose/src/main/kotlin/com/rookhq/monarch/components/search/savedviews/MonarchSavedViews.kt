package com.rookhq.monarch.components.search.savedviews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.overlays.menu.MonarchMenuLabel
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A menu button that switches between named views of a list (its filters,
 * sort and columns), with an Edited marker when the list differs from the
 * selected view and actions to save, save as new, discard, rename, set the
 * default and delete.
 *
 * Built by Monarch from `MonarchButton`, a `MonarchMenu` and
 * `MonarchDialog`s with a `MonarchTextField`. The app stores the
 * views; the component holds no data. Only actions with a callback are
 * offered. [onSaveNew], [onRename] and [onDelete] suspend: their dialog
 * stays open with a busy button until they return, and shows a failure when
 * they throw. The other actions return at once; the app reports their
 * failures.
 *
 * @param views the views in menu order.
 * @param selectedId the id of the selected view, or `null` for none: the button then reads "All items".
 * @param onSelect called with the id of the view the person chooses. Apply its filters, sort and columns.
 * @param modifier the modifier of the button.
 * @param size the size of the button. The touch target is 48dp at every size.
 * @param modified the list's filters differ from the selected view: shows an Edited badge and offers to save or
 * discard.
 * @param enabled whether the menu can be opened.
 * @param maxNameLength the longest name the naming field accepts.
 * @param trailingIcon a decorative icon at the end of the button, such as a down chevron.
 * @param labels replacements for the built-in strings.
 * @param onSaveNew stores the current state under the trimmed name and selects it. Offers "Save as new view…".
 * @param onSaveChanges saves the current state into the view with the id. Offered when modified and not read-only.
 * @param onDiscardChanges reapplies the selected view. Offered when modified.
 * @param onRename renames the view with the id to the trimmed name. Offered for a selected view that is not
 * read-only.
 * @param onDelete deletes the view with the id after the person confirms. Offered for a selected view that is not
 * read-only.
 * @param onSetDefault makes the view with the id open by default. Offered for a selected view that is not already
 * the default.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchSavedViews(
    views: List<MonarchSavedView>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    modified: Boolean = false,
    enabled: Boolean = true,
    maxNameLength: Int = 80,
    trailingIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchSavedViewsLabels = MonarchSavedViewsLabels(),
    onSaveNew: (suspend (String) -> Unit)? = null,
    onSaveChanges: ((String) -> Unit)? = null,
    onDiscardChanges: (() -> Unit)? = null,
    onRename: (suspend (String, String) -> Unit)? = null,
    onDelete: (suspend (String) -> Unit)? = null,
    onSetDefault: ((String) -> Unit)? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // "" names a new view; an id renames that view.
    var naming by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val current = views.firstOrNull { it.id == selectedId }
    val name = current?.name ?: monarchString(labels.noView, R.string.monarch_savedviews_no_view)
    val edited = monarchString(labels.modified, R.string.monarch_savedviews_modified)
    val viewName = monarchString(labels.view, R.string.monarch_savedviews_view)
    val actions = MonarchSavedViewsTokens.actions(
        current, modified, onSaveChanges != null, onSaveNew != null, onDiscardChanges != null,
        onRename != null, onSetDefault != null, onDelete != null,
    )
    Box(modifier = modifier) {
        MonarchButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = viewName
                stateDescription = MonarchSavedViewsTokens.state(name, edited.takeIf { modified })
            },
            size = size,
            enabled = enabled,
        ) {
            Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            current?.count?.let {
                Spacer(Modifier.width(MonarchSavedViewsTokens.Gap))
                Text(text = it.toString(), color = MonarchSavedViewsTokens.secondaryText(MonarchTheme.colors))
            }
            if (modified) {
                Spacer(Modifier.width(MonarchSavedViewsTokens.Gap))
                MonarchBadge(text = edited, tone = MonarchTone.Warning)
            }
            trailingIcon?.let {
                Spacer(Modifier.width(MonarchSavedViewsTokens.Gap))
                it()
            }
        }
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            ViewItems(views, selectedId, labels) { id ->
                expanded = false
                if (id != selectedId) onSelect(id)
            }
            if (actions.any) {
                MonarchMenuDivider()
                ActionItems(
                    current = current,
                    actions = actions,
                    labels = labels,
                    close = { expanded = false },
                    onSaveChanges = onSaveChanges,
                    onDiscardChanges = onDiscardChanges,
                    onSetDefault = onSetDefault,
                    onNaming = { naming = it },
                    onDeleting = { deletingId = it },
                )
            }
        }
    }
    naming?.let { mode ->
        val renaming = views.firstOrNull { it.id == mode }
        SavedViewsNameDialog(
            initial = renaming?.name.orEmpty(),
            views = views,
            renamingId = renaming?.id,
            maxLength = maxNameLength,
            text = nameText(labels, renaming != null),
            onDismiss = { naming = null },
            onSubmit = { typed ->
                if (renaming != null) onRename?.invoke(renaming.id, typed) else onSaveNew?.invoke(typed)
            },
        )
    }
    val deleting = views.firstOrNull { it.id == deletingId }
    if (deleting != null && onDelete != null) {
        SavedViewsDeleteDialog(
            text = SavedViewsDeleteText(
                title = labels.deleteTitle?.invoke(deleting.name)
                    ?: stringResource(R.string.monarch_savedviews_delete_title, deleting.name),
                message = monarchString(labels.deleteMessage, R.string.monarch_savedviews_delete_message),
                confirm = monarchString(labels.deleteConfirm, R.string.monarch_savedviews_delete_confirm),
                cancel = monarchString(labels.cancel, R.string.monarch_savedviews_cancel),
                failed = monarchString(labels.deleteFailed, R.string.monarch_savedviews_delete_failed),
            ),
            onDismiss = { deletingId = null },
            onConfirm = { onDelete(deleting.id) },
        )
    }
}

@Composable
private fun nameText(labels: MonarchSavedViewsLabels, renaming: Boolean): SavedViewsNameText = SavedViewsNameText(
    title = if (renaming) {
        monarchString(labels.renameTitle, R.string.monarch_savedviews_rename_title)
    } else {
        monarchString(labels.saveTitle, R.string.monarch_savedviews_save_title)
    },
    name = monarchString(labels.name, R.string.monarch_savedviews_name),
    save = monarchString(labels.save, R.string.monarch_savedviews_save),
    cancel = monarchString(labels.cancel, R.string.monarch_savedviews_cancel),
    required = monarchString(labels.nameRequired, R.string.monarch_savedviews_name_required),
    taken = monarchString(labels.nameTaken, R.string.monarch_savedviews_name_taken),
    failed = monarchString(labels.saveFailed, R.string.monarch_savedviews_save_failed),
)

/** The views, grouped under their headings, each with a check that marks the selected one. */
@Composable
private fun ViewItems(
    views: List<MonarchSavedView>,
    selectedId: String?,
    labels: MonarchSavedViewsLabels,
    onChoose: (String) -> Unit,
) {
    val tokens = MonarchSavedViewsTokens
    val shared = monarchString(labels.shared, R.string.monarch_savedviews_shared)
    val defaultView = monarchString(labels.defaultView, R.string.monarch_savedviews_default_view)
    val ungrouped = monarchString(labels.views, R.string.monarch_savedviews_views)
    tokens.groups(views).forEachIndexed { index, group ->
        if (index > 0) MonarchMenuDivider()
        MonarchMenuLabel(text = group.name ?: ungrouped)
        group.views.forEach { view ->
            val chosen = view.id == selectedId
            MonarchMenuItem(onClick = { onChoose(view.id) }, selected = chosen) {
                Text(view.name)
                tokens.details(view, shared, defaultView)?.let {
                    Text(
                        text = it,
                        style = MonarchTheme.typography.caption,
                        color = tokens.secondaryText(MonarchTheme.colors),
                    )
                }
            }
        }
    }
}

/** The actions for the current state, in the web's order; each flag already includes its callback. */
@Suppress("LongParameterList")
@Composable
private fun ActionItems(
    current: MonarchSavedView?,
    actions: SavedViewsActions,
    labels: MonarchSavedViewsLabels,
    close: () -> Unit,
    onSaveChanges: ((String) -> Unit)?,
    onDiscardChanges: (() -> Unit)?,
    onSetDefault: ((String) -> Unit)?,
    onNaming: (String) -> Unit,
    onDeleting: (String) -> Unit,
) {
    val id = current?.id.orEmpty()
    val saveChanges = labels.saveChanges?.invoke(current?.name.orEmpty())
        ?: stringResource(R.string.monarch_savedviews_save_changes, current?.name.orEmpty())
    val items = listOfNotNull(
        SavedViewsMenuAction(saveChanges) { onSaveChanges?.invoke(id) }.takeIf { actions.saveChanges },
        SavedViewsMenuAction(monarchString(labels.saveAsNew, R.string.monarch_savedviews_save_as_new)) {
            onNaming("")
        }.takeIf { actions.saveAsNew },
        SavedViewsMenuAction(monarchString(labels.discardChanges, R.string.monarch_savedviews_discard_changes)) {
            onDiscardChanges?.invoke()
        }.takeIf { actions.discard },
        SavedViewsMenuAction(monarchString(labels.rename, R.string.monarch_savedviews_rename)) {
            onNaming(id)
        }.takeIf { actions.rename },
        SavedViewsMenuAction(monarchString(labels.setDefault, R.string.monarch_savedviews_set_default)) {
            onSetDefault?.invoke(id)
        }.takeIf { actions.setDefault },
        SavedViewsMenuAction(monarchString(labels.delete, R.string.monarch_savedviews_delete), danger = true) {
            onDeleting(id)
        }.takeIf { actions.delete },
    )
    items.forEach { item ->
        MonarchMenuItem(
            text = item.text,
            onClick = {
                close()
                item.action()
            },
            destructive = item.danger,
        )
    }
}

/** One action of the menu. */
private class SavedViewsMenuAction(val text: String, val danger: Boolean = false, val action: () -> Unit)

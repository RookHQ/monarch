package dev.monarchui.components.identity.workspaceswitcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchFieldGlyph
import dev.monarchui.components.forms.textfield.MonarchFieldGlyphIcon
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.internal.MonarchChoiceSheetAction
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchChoiceSheetEntry
import dev.monarchui.internal.MonarchChoiceSheetTexts
import dev.monarchui.internal.MonarchSingleChoiceSheet
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Switches between the workspaces a person belongs to, from a button with
 * the workspace's logo, name and handle. Up to five loaded workspaces open a
 * menu, so people with few workspaces switch in one tap; more, or while
 * loading, open a searchable sheet. Both offer Create workspace and All
 * workspaces.
 *
 * A ghost `MonarchButton` with a `MonarchAvatar` logo and the dropdown arrow
 * opens a `MonarchMenu` of `MonarchMenuItem`s, or a sheet with a
 * `MonarchSearchBar` and a `LazyColumn` of single-choice rows (the sheet
 * `MonarchSpaceSwitcher` uses). The current workspace has the accent check in
 * the menu and a radio mark in the sheet. Search ignores case and accents and matches the
 * name, the handle and the keywords. The app does the switch.
 *
 * @param workspaces the workspaces this person belongs to, in display order.
 * @param selection the current workspace id, `null` when none is chosen.
 * @param onSelectionChange called with the chosen workspace id. Choosing the current one again does not call it.
 * @param modifier the modifier of the button.
 * @param size the height and padding of the button.
 * @param enabled whether the menu or sheet can be opened.
 * @param loading the workspaces are still loading: the button opens the sheet, which says so.
 * @param iconOnly shows only the logo and the arrow, for a narrow bar. TalkBack still reads the name.
 * @param onCreate adds Create workspace, which closes the menu or sheet and runs this.
 * @param onManage adds All workspaces, which closes the menu or sheet and runs this, for example to open Rook ID.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the list when the switcher first appears, as the web example does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
public fun MonarchWorkspaceSwitcher(
    workspaces: List<MonarchWorkspaceOption>,
    selection: String?,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    iconOnly: Boolean = false,
    onCreate: (() -> Unit)? = null,
    onManage: (() -> Unit)? = null,
    labels: MonarchWorkspaceSwitcherLabels = MonarchWorkspaceSwitcherLabels(),
    initiallyOpen: Boolean = false,
) {
    val rules = MonarchWorkspaceSwitcherRules
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    var query by rememberSaveable { mutableStateOf("") }
    val label = monarchString(labels.label, R.string.monarch_workspaceswitcher_label)
    val placeholder = monarchString(labels.placeholder, R.string.monarch_workspaceswitcher_placeholder)
    val current = rules.current(workspaces, selection)
    val close = {
        open = false
        query = ""
    }
    val choose: (String?) -> Unit = { next ->
        close()
        if (next != null && next != current?.id) onSelectionChange(next)
    }
    val actions = listOfNotNull(
        onCreate?.let { create ->
            MonarchChoiceSheetAction(
                monarchString(labels.create, R.string.monarch_workspaceswitcher_create),
                MonarchDefaultIcons.Plus,
            ) {
                close()
                create()
            }
        },
        onManage?.let { manage ->
            MonarchChoiceSheetAction(
                monarchString(labels.manage, R.string.monarch_workspaceswitcher_manage),
                MonarchDefaultIcons.Manage,
            ) {
                close()
                manage()
            }
        },
    )
    val menu = rules.usesMenu(workspaces.size, loading)
    Box(modifier = modifier) {
        WorkspaceSwitcherButton(current, label, placeholder, size, enabled, iconOnly, open) { open = true }
        if (menu) {
            MonarchMenu(expanded = open && enabled, onDismissRequest = close) {
                MonarchSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = monarchString(labels.search, R.string.monarch_workspaceswitcher_search),
                    leadingIcon = MonarchDefaultIcons.Search,
                )
                workspaces.filter { rules.matches(it, query) }.forEach { workspace ->
                    WorkspaceSwitcherMenuItem(workspace, chosen = workspace.id == current?.id) { choose(workspace.id) }
                }
                if (actions.isNotEmpty()) MonarchMenuDivider()
                actions.forEach { action ->
                    MonarchMenuItem(text = action.label, onClick = action.onClick, leadingIcon = action.icon)
                }
            }
        }
    }
    if (!menu && open && enabled) {
        MonarchSingleChoiceSheet(
            texts = MonarchChoiceSheetTexts(
                title = label,
                search = monarchString(labels.search, R.string.monarch_workspaceswitcher_search),
                loading = monarchString(labels.loading, R.string.monarch_workspaceswitcher_loading),
                empty = monarchString(labels.empty, R.string.monarch_workspaceswitcher_empty),
                cancel = monarchString(labels.cancel, R.string.monarch_workspaceswitcher_cancel),
            ),
            query = query,
            onQueryChange = { query = it },
            entries = workspaces.filter { rules.matches(it, query) }.map { workspace ->
                MonarchChoiceSheetEntry(
                    id = workspace.id,
                    label = workspace.name,
                    detail = rules.handle(workspace),
                    spoken = rules.spoken(workspace),
                ) {
                    WorkspaceSwitcherLogo(workspace.id, workspace.name, workspace)
                }
            },
            selected = current?.id,
            loading = loading,
            actions = actions,
            onChoose = choose,
            onDismiss = close,
        )
    }
}

/** The button: the logo, the name and handle (unless icon only) and the arrow, read as one element. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun WorkspaceSwitcherButton(
    current: MonarchWorkspaceOption?,
    label: String,
    placeholder: String,
    size: MonarchButtonSize,
    enabled: Boolean,
    iconOnly: Boolean,
    open: Boolean,
    onClick: () -> Unit,
) {
    val value = current?.let { MonarchWorkspaceSwitcherRules.spoken(it) } ?: placeholder
    MonarchButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = label
            stateDescription = value
        },
        variant = MonarchButtonVariant.Ghost,
        size = size,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(MonarchWorkspaceSwitcherTokens.ButtonGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkspaceSwitcherLogo(current?.id ?: "", current?.name ?: label, current)
            if (!iconOnly) {
                Column {
                    Text(text = current?.name ?: placeholder, style = MonarchTheme.typography.label)
                    current?.let { MonarchWorkspaceSwitcherRules.handle(it) }?.let {
                        Text(
                            text = it,
                            style = MonarchTheme.typography.caption,
                            color = MonarchWorkspaceSwitcherTokens.handleColor(MonarchTheme.colors),
                        )
                    }
                }
            }
            // The Monarch chevron in the muted colour, turned up while the menu is open.
            MonarchFieldGlyphIcon(
                glyph = MonarchFieldGlyph.ChevronDown,
                modifier = Modifier.rotate(if (open) HalfTurn else 0f),
                tint = MonarchTheme.colors.textMuted,
            )
        }
    }
}

/** A workspace in the menu: logo, name and handle, and the accent check on the current one. */
@Composable
private fun WorkspaceSwitcherMenuItem(workspace: MonarchWorkspaceOption, chosen: Boolean, onClick: () -> Unit) {
    val spoken = MonarchWorkspaceSwitcherRules.spoken(workspace)
    MonarchMenuItem(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = spoken },
        selected = chosen,
        leadingIcon = { WorkspaceSwitcherLogo(workspace.id, workspace.name, workspace) },
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(MonarchWorkspaceSwitcherTokens.ButtonGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = workspace.name, modifier = Modifier.weight(1f, fill = false))
            MonarchWorkspaceSwitcherRules.handle(workspace)?.let {
                Text(
                    text = it,
                    style = MonarchTheme.typography.caption,
                    color = MonarchWorkspaceSwitcherTokens.handleColor(MonarchTheme.colors),
                )
            }
        }
    }
}

/** The decorative logo: the workspace's picture, or its initial on a tinted square tile. */
@Composable
private fun WorkspaceSwitcherLogo(id: String, name: String, workspace: MonarchWorkspaceOption?) {
    MonarchAvatar(
        image = workspace?.logo,
        name = name,
        shape = MonarchAvatarShape.Square,
        size = MonarchAvatarSize.Sm,
        seed = id.ifEmpty { name },
        decorative = true,
    )
}

private const val HalfTurn = 180f

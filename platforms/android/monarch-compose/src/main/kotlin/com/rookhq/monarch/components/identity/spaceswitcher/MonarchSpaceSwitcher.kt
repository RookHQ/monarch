package com.rookhq.monarch.components.identity.spaceswitcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyph
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyphIcon
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarShape
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.internal.MonarchChoiceSheetAction
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchChoiceSheetEntry
import com.rookhq.monarch.internal.MonarchChoiceSheetTexts
import com.rookhq.monarch.internal.MonarchSingleChoiceSheet
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Chooses which Space of the workspace the app shows, "All spaces" or one
 * Space, from a button that opens a sheet with a searchable list, New Space
 * and Manage Spaces.
 *
 * Built on Material 3: a ghost `MonarchButton` with the Space's icon, name
 * and the exposed-dropdown arrow opens a `ModalBottomSheet` with a
 * `MonarchSearchBar` and a `LazyColumn` of single-choice rows, the chosen one
 * marked with a radio mark. Search ignores case and accents and matches the
 * name and the description. A Space id that is no longer in [spaces] reads as
 * All spaces. Monarch never fetches: the app supplies the Spaces and stores
 * the choice.
 *
 * @param spaces the Spaces this person can open, in display order.
 * @param selection the chosen Space id, `null` for All spaces.
 * @param onSelectionChange called with the chosen Space id, or `null` for All spaces.
 * @param modifier the modifier of the button.
 * @param size the height and padding of the button.
 * @param enabled whether the sheet can be opened.
 * @param allowsAll offers All spaces first. Without it a Space must stay chosen and the button reads
 * "Choose a Space" until one is.
 * @param loading the Spaces are still loading: the sheet says so.
 * @param iconOnly shows only the icon and the arrow, for a narrow bar. TalkBack still reads the name.
 * @param hidesWhenEmpty draws nothing when there are no Spaces and nothing is loading.
 * @param allIcon the icon of All spaces, decorative. `null` shows a tinted tile.
 * @param onCreate adds New Space, which closes the sheet and runs this.
 * @param onManage adds Manage Spaces, which closes the sheet and runs this, for example to open Rook ID.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the list when the switcher first appears, as the web example does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
public fun MonarchSpaceSwitcher(
    spaces: List<MonarchSpaceOption>,
    selection: String?,
    onSelectionChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    allowsAll: Boolean = true,
    loading: Boolean = false,
    iconOnly: Boolean = false,
    hidesWhenEmpty: Boolean = true,
    allIcon: Painter? = null,
    onCreate: (() -> Unit)? = null,
    onManage: (() -> Unit)? = null,
    labels: MonarchSpaceSwitcherLabels = MonarchSpaceSwitcherLabels(),
    initiallyOpen: Boolean = false,
) {
    val rules = MonarchSpaceSwitcherRules
    if (rules.hidden(spaces, loading, hidesWhenEmpty)) return
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    var query by rememberSaveable { mutableStateOf("") }
    val label = monarchString(labels.label, R.string.monarch_spaceswitcher_label)
    val all = monarchString(labels.all, R.string.monarch_spaceswitcher_all)
    val current = rules.current(spaces, selection)
    val placeholder = monarchString(labels.placeholder, R.string.monarch_spaceswitcher_placeholder)
    val name = current?.name ?: if (allowsAll) all else placeholder
    SpaceSwitcherButton(
        modifier = modifier,
        texts = Triple(label, name, all),
        icon = current?.icon ?: allIcon,
        currentId = current?.id,
        size = size,
        enabled = enabled,
        iconOnly = iconOnly,
        open = open,
    ) { open = true }
    if (open && enabled) {
        val close = {
            open = false
            query = ""
        }
        val entries = spaceSwitcherEntries(spaces, query, allowsAll, all, allIcon)
        MonarchSingleChoiceSheet(
            texts = MonarchChoiceSheetTexts(
                title = label,
                search = monarchString(labels.search, R.string.monarch_spaceswitcher_search),
                loading = monarchString(labels.loading, R.string.monarch_spaceswitcher_loading),
                empty = monarchString(labels.empty, R.string.monarch_spaceswitcher_empty),
                cancel = monarchString(labels.cancel, R.string.monarch_spaceswitcher_cancel),
            ),
            query = query,
            onQueryChange = { query = it },
            entries = entries,
            selected = current?.id,
            loading = loading,
            actions = spaceSwitcherActions(labels, onCreate, onManage, close),
            onChoose = { next ->
                if (rules.accepts(next, allowsAll)) {
                    close()
                    if (next != current?.id) onSelectionChange(next)
                }
            },
            onDismiss = close,
        )
    }
}

/** The button: the icon, the caption and name (unless icon only) and the arrow, read as one element. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun SpaceSwitcherButton(
    modifier: Modifier,
    texts: Triple<String, String, String>,
    icon: Painter?,
    currentId: String?,
    size: MonarchButtonSize,
    enabled: Boolean,
    iconOnly: Boolean,
    open: Boolean,
    onClick: () -> Unit,
) {
    val (label, name, all) = texts
    MonarchButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = label
            stateDescription = name
        },
        variant = MonarchButtonVariant.Ghost,
        size = size,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(MonarchSpaceSwitcherTokens.ButtonGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpaceSwitcherIcon(id = currentId ?: "", name = if (currentId == null) all else name, icon = icon)
            if (!iconOnly) {
                Column {
                    Text(
                        text = label,
                        style = MonarchTheme.typography.caption,
                        color = MonarchSpaceSwitcherTokens.detailColor(MonarchTheme.colors),
                    )
                    Text(text = name, style = MonarchTheme.typography.label)
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

/** New Space and Manage Spaces, each closing the sheet first. */
@Composable
private fun spaceSwitcherActions(
    labels: MonarchSpaceSwitcherLabels,
    onCreate: (() -> Unit)?,
    onManage: (() -> Unit)?,
    close: () -> Unit,
): List<MonarchChoiceSheetAction> {
    val create = monarchString(labels.create, R.string.monarch_spaceswitcher_create)
    val manage = monarchString(labels.manage, R.string.monarch_spaceswitcher_manage)
    return listOfNotNull(
        onCreate?.let { action ->
            MonarchChoiceSheetAction(create, MonarchDefaultIcons.Plus) {
                close()
                action()
            }
        },
        onManage?.let { action ->
            MonarchChoiceSheetAction(manage, MonarchDefaultIcons.Manage) {
                close()
                action()
            }
        },
    )
}

/** The rows of the sheet: All spaces first (unless searching or not allowed), then the matching Spaces. */
private fun spaceSwitcherEntries(
    spaces: List<MonarchSpaceOption>,
    query: String,
    allowsAll: Boolean,
    all: String,
    allIcon: Painter?,
): List<MonarchChoiceSheetEntry> = buildList {
    val rules = MonarchSpaceSwitcherRules
    if (allowsAll && query.isBlank()) {
        add(MonarchChoiceSheetEntry(null, all, null, all) { SpaceSwitcherIcon("", all, allIcon) })
    }
    spaces.filter { rules.matches(query, it.name, it.description) }.forEach { space ->
        val spoken = rules.spoken(space.name, space.description)
        add(
            MonarchChoiceSheetEntry(space.id, space.name, space.description, spoken) {
                SpaceSwitcherIcon(space.id, space.name, space.icon)
            },
        )
    }
}

/** The decorative icon of a Space: its picture, or its first letter on a tinted square tile. */
@Composable
internal fun SpaceSwitcherIcon(id: String, name: String, icon: Painter?) {
    MonarchAvatar(
        image = icon,
        name = name,
        shape = MonarchAvatarShape.Square,
        size = MonarchAvatarSize.Sm,
        seed = id.ifEmpty { name },
        decorative = true,
    )
}

private const val HalfTurn = 180f

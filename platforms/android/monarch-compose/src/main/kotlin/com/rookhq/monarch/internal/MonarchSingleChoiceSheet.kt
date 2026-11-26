package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * One row of a [MonarchSingleChoiceSheet].
 *
 * @property id the id passed back when chosen; `null` for a row such as "All spaces".
 * @property label the visible name.
 * @property detail secondary text under the name.
 * @property spoken what TalkBack reads for the row.
 * @property leading the decorative icon, avatar or logo.
 */
internal class MonarchChoiceSheetEntry(
    val id: String?,
    val label: String,
    val detail: String?,
    val spoken: String,
    val leading: @Composable () -> Unit,
)

/** An action under the list of a [MonarchSingleChoiceSheet], such as "New space". */
internal class MonarchChoiceSheetAction(
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
)

/** The texts of a [MonarchSingleChoiceSheet]. */
internal class MonarchChoiceSheetTexts(
    val title: String,
    val search: String,
    val loading: String,
    val empty: String,
    val cancel: String,
)

/** The sizes of the rows of [MonarchSingleChoiceSheet]. */
internal object MonarchSingleChoiceSheetDefaults {
    /** The least height of a row: the touch target. */
    val RowMinHeight: Dp = MonarchTokens.Control.xl.dp

    /** The vertical padding of a row. */
    val RowPadding: Dp = MonarchTokens.Space.space1.dp

    /** The space between the picture, the text and the check. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The check of the chosen row. */
    val CheckSize: Dp = MonarchTokens.Space.space4.dp
}

/**
 * The searchable single-choice sheet of the switchers (space, workspace): a
 * [MonarchSearchListSheet] on the raised surface whose rows are radio rows
 * (the chosen one marked with the accent check), with a polite message while loading or when
 * nothing matches, the [actions] and Cancel. The caller filters [entries] for
 * [query].
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchSingleChoiceSheet(
    texts: MonarchChoiceSheetTexts,
    query: String,
    onQueryChange: (String) -> Unit,
    entries: List<MonarchChoiceSheetEntry>,
    selected: String?,
    loading: Boolean,
    actions: List<MonarchChoiceSheetAction>,
    onChoose: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    MonarchSearchListSheet(
        title = texts.title,
        query = query,
        onQueryChange = onQueryChange,
        onDismiss = onDismiss,
        searchPlaceholder = texts.search,
        searching = loading,
        containerColor = MonarchTheme.colors.bgRaised,
        buttons = {
            actions.forEach { action ->
                MonarchButton(
                    text = action.label,
                    onClick = action.onClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Ghost,
                    leadingIcon = action.icon,
                )
            }
            MonarchButton(
                text = texts.cancel,
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = MonarchSearchListSheetDefaults.Padding),
                variant = MonarchButtonVariant.Ghost,
            )
        },
    ) {
        items(entries, key = { it.id ?: "" }) { entry ->
            ChoiceSheetRow(entry, chosen = entry.id == selected, onClick = { onChoose(entry.id) })
        }
        val message = when {
            loading -> texts.loading
            entries.isEmpty() -> texts.empty
            else -> null
        }
        if (message != null) item(key = "message") { ChoiceSheetMessage(message) }
    }
}

/** A single-choice row: the leading picture, the name and detail, and a radio mark for the chosen one. */
@Composable
private fun ChoiceSheetRow(entry: MonarchChoiceSheetEntry, chosen: Boolean, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchSingleChoiceSheetDefaults.RowMinHeight)
            .monarchFocusRing(MonarchTheme.shapes.sm)
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = entry.spoken }
            .padding(vertical = MonarchSingleChoiceSheetDefaults.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchSingleChoiceSheetDefaults.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entry.leading()
        Column(modifier = Modifier.weight(1f).clearAndSetSemantics { }) {
            Text(text = entry.label, style = MonarchTheme.typography.body, color = colors.textDefault)
            entry.detail?.let {
                Text(text = it, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
        // The chosen row is marked with the accent check of Monarch menus; the row itself is the radio button.
        Box(Modifier.size(MonarchSingleChoiceSheetDefaults.CheckSize)) {
            if (chosen) {
                MonarchStrokeGlyphMark(
                    MonarchStrokeGlyph.Check,
                    colors.accentStrong,
                    Modifier.size(MonarchSingleChoiceSheetDefaults.CheckSize),
                )
            }
        }
    }
}

/** Loading or no matches, announced politely. */
@Composable
private fun ChoiceSheetMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(vertical = MonarchSingleChoiceSheetDefaults.RowPadding)
            .semantics { liveRegion = LiveRegionMode.Polite },
        style = MonarchTheme.typography.body,
        color = MonarchTheme.colors.textMuted,
    )
}

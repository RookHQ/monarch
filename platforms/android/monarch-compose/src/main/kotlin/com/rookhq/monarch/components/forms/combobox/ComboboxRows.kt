package com.rookhq.monarch.components.forms.combobox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.select.MonarchMenuRow
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyph
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyphIcon
import com.rookhq.monarch.theme.MonarchTheme

/** Whether [entry] is the chosen row: the chosen option, or the clear row while nothing is chosen. */
private fun isChosen(entry: ComboboxEntry, selected: String?): Boolean = when (entry) {
    is ComboboxEntry.Option -> entry.option.id == selected
    is ComboboxEntry.Clear -> selected == null
    else -> false
}

/** One row of the menu presentation: a Material 3 menu item, a heading or a message. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComboboxMenuEntry(entry: ComboboxEntry, selected: String?, onChoose: (ComboboxEntry) -> Unit) {
    when (entry) {
        is ComboboxEntry.Header -> ComboboxHeader(entry.text)
        is ComboboxEntry.Message ->
            if (entry.failed) ComboboxFailureRow(entry, onChoose) else ComboboxMessage(entry.text)
        else -> {
            val chosen = isChosen(entry, selected)
            val enabled = (entry as? ComboboxEntry.Option)?.option?.enabled ?: true
            val leading = (entry as? ComboboxEntry.Option)?.option?.leading
            MonarchMenuRow(
                selected = chosen,
                enabled = enabled,
                onClick = { onChoose(entry) },
                mark = chosen && entry !is ComboboxEntry.Create,
                leading = leading?.let { slot -> { ComboboxLeading(slot, enabled) } },
                hint = (entry as? ComboboxEntry.Option)?.option?.hint?.let { text ->
                    {
                        Text(
                            text,
                            style = MonarchTheme.typography.caption,
                            color = MonarchComboboxTokens.secondaryColor(enabled, MonarchTheme.colors),
                        )
                    }
                },
            ) { ComboboxRowText(entry, enabled) }
        }
    }
}

/** One row of the sheet presentation: a selectable row with a radio mark, a heading or a message. */
@Composable
internal fun ComboboxSheetEntry(entry: ComboboxEntry, selected: String?, onChoose: (ComboboxEntry) -> Unit) {
    when (entry) {
        is ComboboxEntry.Header -> ComboboxHeader(entry.text)
        is ComboboxEntry.Message ->
            if (entry.failed) ComboboxFailureRow(entry, onChoose) else ComboboxMessage(entry.text)
        else -> {
            val chosen = isChosen(entry, selected)
            val enabled = (entry as? ComboboxEntry.Option)?.option?.enabled ?: true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MonarchComboboxTokens.RowMinHeight)
                    .selectable(
                        selected = chosen,
                        enabled = enabled,
                        role = if (entry is ComboboxEntry.Create) Role.Button else Role.RadioButton,
                        onClick = { onChoose(entry) },
                    )
                    .padding(vertical = MonarchComboboxTokens.RowVerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(MonarchComboboxTokens.RowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (entry as? ComboboxEntry.Option)?.option?.leading?.let { ComboboxLeading(it, enabled) }
                Column(modifier = Modifier.weight(1f)) { ComboboxRowText(entry, enabled) }
                if (entry !is ComboboxEntry.Create) ComboboxRowEnd(entry, chosen, enabled)
            }
        }
    }
}

/** The label and description of a row. */
@Composable
private fun ComboboxRowText(entry: ComboboxEntry, enabled: Boolean) {
    val colors = MonarchTheme.colors
    val (label, description) = when (entry) {
        is ComboboxEntry.Option -> MonarchComboboxTokens.label(entry.option) to entry.option.description
        is ComboboxEntry.Clear -> AnnotatedString(entry.label) to null
        is ComboboxEntry.Create -> AnnotatedString(entry.label) to null
        else -> AnnotatedString("") to null
    }
    Column(verticalArrangement = Arrangement.spacedBy(MonarchComboboxTokens.ItemTextGap)) {
        Text(label, style = MonarchTheme.typography.body, color = MonarchComboboxTokens.labelColor(enabled, colors))
        if (description != null) {
            Text(
                description,
                style = MonarchTheme.typography.caption,
                color = MonarchComboboxTokens.secondaryColor(enabled, colors),
            )
        }
    }
}

/** The hint and the accent check at the end of a row; the row carries the selected state for TalkBack. */
@Composable
private fun ComboboxRowEnd(entry: ComboboxEntry, chosen: Boolean, enabled: Boolean) {
    val colors = MonarchTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchComboboxTokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (entry as? ComboboxEntry.Option)?.option?.hint?.let {
            Text(
                it,
                style = MonarchTheme.typography.caption,
                color = MonarchComboboxTokens.secondaryColor(enabled, colors),
            )
        }
        if (chosen) {
            MonarchFieldGlyphIcon(
                MonarchFieldGlyph.Check,
                tint = if (enabled) colors.accentStrong else colors.textDisabled,
            )
        }
    }
}

/** The leading content of an option, decorative, in the secondary colour. */
@Composable
private fun ComboboxLeading(content: @Composable () -> Unit, enabled: Boolean) {
    val color = MonarchComboboxTokens.secondaryColor(enabled, MonarchTheme.colors)
    CompositionLocalProvider(LocalContentColor provides color) {
        Box(modifier = Modifier.clearAndSetSemantics { }) { content() }
    }
}

/** The load error, announced politely, and the retry button when the app can load again. */
@Composable
private fun ComboboxFailureRow(entry: ComboboxEntry.Message, onChoose: (ComboboxEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MonarchComboboxTokens.HeaderHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchComboboxTokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.text,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = MonarchComboboxTokens.HeaderVerticalPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = MonarchComboboxTokens.errorColor(MonarchTheme.colors),
        )
        entry.retry?.let { retry ->
            MonarchButton(text = retry, onClick = { onChoose(entry) }, variant = MonarchButtonVariant.Ghost)
        }
    }
}

/** A group heading. */
@Composable
private fun ComboboxHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(
                horizontal = MonarchComboboxTokens.HeaderHorizontalPadding,
                vertical = MonarchComboboxTokens.HeaderVerticalPadding,
            )
            .semantics { heading() },
        style = MonarchTheme.typography.caption,
        color = MonarchComboboxTokens.secondaryColor(true, MonarchTheme.colors),
    )
}

/** A message in the list (loading, no matches, cut short), announced politely when it appears. */
@Composable
private fun ComboboxMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(
                horizontal = MonarchComboboxTokens.HeaderHorizontalPadding,
                vertical = MonarchComboboxTokens.HeaderVerticalPadding,
            )
            .semantics { liveRegion = LiveRegionMode.Polite },
        style = MonarchTheme.typography.caption,
        color = MonarchComboboxTokens.secondaryColor(true, MonarchTheme.colors),
    )
}

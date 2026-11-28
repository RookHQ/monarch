package com.rookhq.monarch.components.forms.combobox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.formfield.MonarchFieldFrame
import com.rookhq.monarch.components.forms.formfield.monarchFieldSemantics
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyph
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyphIcon
import com.rookhq.monarch.components.forms.textfield.MonarchFieldIconButton
import com.rookhq.monarch.components.forms.textfield.MonarchFieldInput
import com.rookhq.monarch.components.forms.textfield.MonarchFieldTrigger
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.internal.MonarchSearchListSheet
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The menu presentation: an editable field that filters an exposed dropdown
 * menu. While the menu is open the field holds the search text, with the
 * chosen label as its placeholder; when it closes the field shows the chosen
 * label again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComboboxMenuField(state: ComboboxState, actions: ComboboxActions, modifier: Modifier) {
    ExposedDropdownMenuBox(
        expanded = state.expanded,
        onExpandedChange = { if (it) actions.open() else actions.close() },
        modifier = modifier,
    ) {
        ComboboxFrame(state) {
            ComboboxInput(
                state = state,
                actions = actions,
                value = if (state.expanded) state.query else state.chosen?.label.orEmpty(),
                placeholder = if (state.expanded) {
                    state.chosen?.label ?: state.texts.search
                } else {
                    state.texts.placeholder
                },
            )
        }
        ExposedDropdownMenu(
            expanded = state.expanded,
            onDismissRequest = actions.close,
            shape = MonarchTheme.shapes.lg,
            containerColor = MonarchTheme.colors.bgRaised,
            border = BorderStroke(MonarchControlMetrics.BorderWidth, MonarchTheme.colors.borderSubtle),
            modifier = Modifier.heightIn(max = MonarchComboboxTokens.MenuMaxHeight),
        ) {
            state.entries.forEach { entry -> ComboboxMenuEntry(entry, state.selected, actions.choose) }
        }
    }
}

/**
 * The sheet presentation: a read-only field that opens a modal bottom sheet
 * with the label as its title, a search field, the list and Cancel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComboboxSheetField(state: ComboboxState, actions: ComboboxActions, modifier: Modifier) {
    ExposedDropdownMenuBox(
        expanded = state.expanded,
        onExpandedChange = { if (it) actions.open() else actions.close() },
        modifier = modifier,
    ) {
        ComboboxFrame(state) { ComboboxTrigger(state, actions) }
    }
    if (state.expanded) {
        MonarchSearchListSheet(
            title = state.label,
            query = state.query,
            onQueryChange = actions.type,
            onDismiss = actions.close,
            searchPlaceholder = state.texts.search,
            searching = state.loading,
            containerColor = MonarchComboboxTokens.sheetColor(MonarchTheme.colors),
            gap = MonarchComboboxTokens.SheetGap,
            buttons = {
                MonarchButton(
                    text = state.texts.cancel,
                    onClick = actions.close,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = MonarchComboboxTokens.SheetPadding),
                    variant = MonarchButtonVariant.Ghost,
                )
            },
        ) {
            items(state.entries) { entry -> ComboboxSheetEntry(entry, state.selected, actions.choose) }
        }
    }
}

/** The label above and the helper or error below the box, which alone is the menu anchor. */
@Composable
private fun ComboboxFrame(state: ComboboxState, box: @Composable () -> Unit) {
    MonarchFieldFrame(
        label = state.label,
        labelVisible = state.labelVisible,
        required = state.required,
        enabled = state.enabled,
        helperText = state.texts.helperText,
        errorText = state.texts.errorText,
        content = box,
    )
}

/** The editable box of the menu presentation: it holds the search text while the menu is open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.ComboboxInput(
    state: ComboboxState,
    actions: ComboboxActions,
    value: String,
    placeholder: String?,
) {
    MonarchFieldInput(
        value = value,
        onValueChange = actions.type,
        modifier = Modifier
            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, state.enabled)
            .monarchFieldSemantics(state.label, state.texts.errorText),
        enabled = state.enabled,
        invalid = !state.texts.errorText.isNullOrBlank(),
        placeholder = placeholder,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ComboboxClear(state, actions)
                MonarchFieldGlyphIcon(MonarchFieldGlyph.ChevronDown)
            }
        },
    )
}

/** The read-only box of the sheet presentation, which opens the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.ComboboxTrigger(state: ComboboxState, actions: ComboboxActions) {
    val shown = state.chosen?.label ?: state.texts.placeholder.orEmpty()
    MonarchFieldTrigger(
        text = shown,
        isPlaceholder = state.chosen == null,
        modifier = Modifier
            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, state.enabled)
            .semantics(mergeDescendants = true) {
                stateDescription = shown
                // The anchor only drops its click when disabled; TalkBack must still hear "disabled".
                if (!state.enabled) disabled()
            }
            .monarchFieldSemantics(state.label, state.texts.errorText),
        expanded = state.expanded,
        enabled = state.enabled,
        invalid = !state.texts.errorText.isNullOrBlank(),
        extra = { ComboboxClear(state, actions) },
    )
}

/** The clear button before the arrow: a real icon button with a 48dp target, when the app gave an icon. */
@Composable
private fun ComboboxClear(state: ComboboxState, actions: ComboboxActions) {
    val clear = actions.clear
    val icon = actions.clearIcon
    if (clear == null || icon == null) return
    MonarchFieldIconButton(name = state.texts.clearName, onClick = clear, bleedEnd = false) {
        CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) { icon() }
    }
}

package com.rookhq.monarch.components.search.searchbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgressVariant
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyph
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyphIcon
import com.rookhq.monarch.components.forms.textfield.MonarchFieldIconButton
import com.rookhq.monarch.foundation.MonarchFieldSize
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.field.MonarchFieldBox
import com.rookhq.monarch.internal.field.MonarchFieldPlaceholder
import com.rookhq.monarch.internal.field.monarchFieldTextStyle
import com.rookhq.monarch.internal.field.rememberFieldInteractionSource
import com.rookhq.monarch.internal.field.rememberMonarchFieldState
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * A search box in Monarch colours, with an optional delay before the search
 * runs, a pending state and a clear button.
 *
 * The input is Monarch's field box (48dp, radius `md`, 1dp border, a search icon at the start) on the
 * platform's `BasicTextField` with the keyboard's search action. The results area that opens under the field
 * when [expanded] stays the Material 3 `DockedSearchBar`, with a transparent container while closed. Monarch
 * adds [onSearch] with a debounce, a progress indicator while a search runs, and a clear button named
 * "Clear search".
 *
 * [onSearch] is called with the query to run: [debounceMillis] after the
 * last keystroke (at once when it is 0), at once on the keyboard's search
 * action, and with an empty query when the field is cleared.
 *
 * @param query the text in the field.
 * @param onQueryChange called with the new text on every keystroke and when the field is cleared.
 * @param onSearch called with the query to run; see above. Use it for fetching.
 * @param modifier the modifier of the search bar.
 * @param expanded whether the results area under the field is open.
 * @param onExpandedChange called when the field asks to open or close the results area, for example when it
 * gains focus or the user goes back.
 * @param enabled whether the user can focus and edit the field.
 * @param pending a search is running: the leading icon becomes a progress indicator and TalkBack reads
 * "Searching" as the state of the field.
 * @param debounceMillis milliseconds without typing before [onSearch] runs. 0 runs it on every keystroke.
 * @param placeholder replaces the built-in placeholder, "Search" (`monarch_searchbar_placeholder`).
 * @param leadingIcon replaces Monarch's own magnifier at the start, for example a `MonarchIcon` with a `null`
 * content description.
 * @param clearIcon replaces Monarch's own close mark on the clear button. The clear button is shown while
 * there is text and the field is enabled.
 * @param onClear called after the field was cleared with the clear button.
 * @param clearLabel replaces the name of the clear button, "Clear search" (`monarch_searchbar_clear`).
 * @param pendingLabel replaces "Searching" (`monarch_searchbar_pending`).
 * @param interactionSource the source of the focus and press interactions of the field.
 * @param content the results or suggestions, shown under the field while [expanded].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    pending: Boolean = false,
    debounceMillis: Long = 0L,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    clearLabel: String? = null,
    pendingLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val roles = MonarchSearchBarTokens.roles(MonarchTheme.colors)
    val currentOnSearch by rememberUpdatedState(onSearch)
    var scheduled by remember { mutableStateOf<ScheduledSearch?>(null) }
    LaunchedEffect(scheduled) {
        val search = scheduled ?: return@LaunchedEffect
        delay(debounceMillis)
        scheduled = null
        currentOnSearch(search.query)
    }
    val change: (String) -> Unit = { text ->
        onQueryChange(text)
        if (debounceMillis <= 0L) {
            scheduled = null
            onSearch(text)
        } else {
            scheduled = ScheduledSearch(text)
        }
    }
    val submit: (String) -> Unit = { text ->
        scheduled = null
        onSearch(text)
    }
    val clear: () -> Unit = {
        scheduled = null
        onQueryChange("")
        onSearch("")
        onClear?.invoke()
    }
    val pendingText = monarchString(pendingLabel, R.string.monarch_searchbar_pending)
    val clearText = monarchString(clearLabel, R.string.monarch_searchbar_clear)
    val placeholderText = monarchString(placeholder, R.string.monarch_searchbar_placeholder)
    val leading: (@Composable () -> Unit)? = if (pending) {
        {
            MonarchProgress(
                modifier = Modifier.size(MonarchSearchBarTokens.PendingSize),
                variant = MonarchProgressVariant.Circular,
                label = pendingText,
            )
        }
    } else {
        leadingIcon
    }
    val trailing: (@Composable () -> Unit)? = if (query.isNotEmpty() && enabled) {
        {
            MonarchFieldIconButton(name = clearText, onClick = clear) {
                clearIcon?.invoke() ?: MonarchFieldGlyphIcon(MonarchFieldGlyph.Close)
            }
        }
    } else {
        null
    }
    val container = if (expanded) roles.container else Color.Transparent
    DockedSearchBar(
        inputField = {
            SearchInput(
                query = query,
                onQueryChange = change,
                onSearch = submit,
                onExpandedChange = onExpandedChange,
                enabled = enabled,
                placeholder = placeholderText,
                leading = leading,
                trailing = trailing,
                // The placeholder names the field for TalkBack also while it holds a query, as the Material input did.
                modifier = Modifier.semantics {
                    contentDescription = placeholderText
                    if (pending) stateDescription = pendingText
                },
                interactionSource = interactionSource ?: rememberFieldInteractionSource(),
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        // The bar is drawn the ring width wider on each side, so the field box inside (inset by the ring) lines
        // up with every other field at the container's edge and its focus ring is not clipped by the bar.
        modifier = modifier.fillMaxWidth().outsetBy(MonarchTheme.focusRingWidth),
        shape = MonarchShapeDefaults.Control,
        colors = SearchBarDefaults.colors(containerColor = container, dividerColor = roles.divider),
        tonalElevation = MonarchSearchBarTokens.Elevation,
        shadowElevation = MonarchSearchBarTokens.Elevation,
        content = content,
    )
}

/**
 * The input of the search bar: the platform's `BasicTextField` in Monarch's field box (48dp, as every field), with the
 * search keyboard action, a leading icon and the clear button at the end. It asks to expand the results
 * when it gains focus, as the Material input field does. The box is inset by the ring width so the focus
 * ring is not clipped by the surface of the search bar.
 */
@Composable
@Suppress("LongParameterList")
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean,
    placeholder: String,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val state = rememberMonarchFieldState(interactionSource, enabled = enabled)
    val colors = MonarchTheme.colors
    val style = monarchFieldTextStyle()
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(MonarchTheme.focusRingWidth)
            .onFocusChanged { if (it.isFocused) onExpandedChange(true) },
        enabled = enabled,
        textStyle = style.copy(color = if (enabled) colors.textDefault else colors.textDisabled),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colors.accentStrong),
        decorationBox = { inner ->
            MonarchFieldBox(
                size = MonarchFieldSize.Medium,
                state = state,
                leading = leading ?: { MonarchFieldGlyphIcon(MonarchFieldGlyph.Search) },
                trailing = trailing,
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) MonarchFieldPlaceholder(placeholder, enabled)
                    inner()
                }
            }
        },
    )
}

/** A search waiting for the debounce. A new object per keystroke, so the wait restarts every time. */
private class ScheduledSearch(val query: String)

/** Measures the content [by] wider and taller on every side and centres it, reporting the original size. */
private fun Modifier.outsetBy(by: Dp): Modifier = layout { measurable, constraints ->
    val extra = by.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = (constraints.minWidth + extra).coerceAtMost(Constraints.Infinity),
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    val width = (placeable.width - extra).coerceAtLeast(0)
    val height = (placeable.height - extra).coerceAtLeast(0)
    layout(width, height) { placeable.placeRelative(-extra / 2, -extra / 2) }
}

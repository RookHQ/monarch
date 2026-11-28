package com.rookhq.monarch.components.files.toggleblock

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.internal.MonarchDefaultIcons

/**
 * A collapsible section of a document: a summary line that shows or hides
 * the content under it.
 *
 * `MonarchCollapsible` as a document block: the summary is one button with
 * the state "Expanded" or "Collapsed" and the TalkBack expand and collapse
 * actions, and the content is indented under the summary. Hidden content
 * leaves the composition, so TalkBack does not reach it. The content appears
 * at once when the user has turned animations off.
 *
 * This overload keeps the state the app gives it; the other keeps its own.
 *
 * @param summary the line that stays visible. It is the name of the button.
 * @param expanded whether the content is shown.
 * @param onExpandedChange called with the new state when the user activates the summary.
 * @param modifier the modifier of the block.
 * @param enabled whether the user can activate the summary. The content keeps its current state.
 * @param trailingIcon a chevron that points down, for example a `MonarchIcon` with a `null` content
 * description. It turns half a turn while expanded. The web's chevron by default; `null` draws none.
 * @param expandedLabel the state TalkBack reads while expanded. `null` uses the built-in "Expanded".
 * @param collapsedLabel the state TalkBack reads while collapsed. `null` uses the built-in "Collapsed".
 * @param interactionSource the source of the press, focus and hover interactions of the summary.
 * @param content the content that is shown and hidden.
 */
@Composable
public fun MonarchToggleBlock(
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    expandedLabel: String? = null,
    collapsedLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MonarchCollapsible(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        // As the web: the chevron comes before the summary, points right and turns down while expanded.
        header = {
            if (trailingIcon != null) {
                val turn = if (trailingIcon === MonarchDefaultIcons.ChevronDown) {
                    if (expanded) MonarchToggleBlockTokens.EXPANDED_TURN else 0f
                } else {
                    0f
                }
                Box(modifier = Modifier.graphicsLayer { rotationZ = turn }) {
                    val chevron = trailingIcon === MonarchDefaultIcons.ChevronDown
                    if (chevron) MonarchDefaultIcons.ChevronRight() else trailingIcon()
                }
            }
            Text(text = summary, modifier = Modifier.weight(1f, fill = false))
        },
        modifier = modifier,
        enabled = enabled,
        expandedLabel = expandedLabel,
        collapsedLabel = collapsedLabel,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(
                start = MonarchToggleBlockTokens.ContentIndent,
                bottom = MonarchToggleBlockTokens.ContentBottomPadding,
            ),
            content = content,
        )
    }
}

/**
 * A collapsible section of a document that keeps its own state, which
 * survives configuration changes. See the other overload for the behavior.
 *
 * @param summary the line that stays visible. It is the name of the button.
 * @param modifier the modifier of the block.
 * @param initiallyExpanded whether the content shows at first.
 * @param enabled whether the user can activate the summary.
 * @param trailingIcon a chevron that points down; it turns half a turn while expanded.
 * @param expandedLabel the state TalkBack reads while expanded. `null` uses the built-in "Expanded".
 * @param collapsedLabel the state TalkBack reads while collapsed. `null` uses the built-in "Collapsed".
 * @param interactionSource the source of the press, focus and hover interactions of the summary.
 * @param content the content that is shown and hidden.
 */
@Composable
public fun MonarchToggleBlock(
    summary: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    expandedLabel: String? = null,
    collapsedLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    MonarchToggleBlock(
        summary = summary,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        enabled = enabled,
        trailingIcon = trailingIcon,
        expandedLabel = expandedLabel,
        collapsedLabel = collapsedLabel,
        interactionSource = interactionSource,
        content = content,
    )
}

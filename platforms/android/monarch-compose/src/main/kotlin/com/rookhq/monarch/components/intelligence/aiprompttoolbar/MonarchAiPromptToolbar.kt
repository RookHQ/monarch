package com.rookhq.monarch.components.intelligence.aiprompttoolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.monarchString

/**
 * The row of controls under a prompt: attach, web search, mode, model, tone.
 *
 * Built from a Compose `FlowRow`: controls that do not fit move to a new
 * line. Fill it with [MonarchAiPromptToolbarButton] and
 * [MonarchAiPromptToolbarToggle] for consistent sizing, and with pickers such
 * as `MonarchAiTonePicker`. The row is one TalkBack group named [label]; each
 * control inside stays its own element, read in order.
 *
 * @param modifier the modifier of the toolbar.
 * @param label the TalkBack name of the group. `null` uses the built-in string `monarch_aiprompttoolbar_label`,
 * "Prompt options".
 * @param content the controls, in order.
 */
@Composable
public fun MonarchAiPromptToolbar(
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable FlowRowScope.() -> Unit,
) {
    val name = monarchString(label, R.string.monarch_aiprompttoolbar_label)
    FlowRow(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = name
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchAiPromptToolbarTokens.Gap),
        verticalArrangement = Arrangement.spacedBy(MonarchAiPromptToolbarTokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

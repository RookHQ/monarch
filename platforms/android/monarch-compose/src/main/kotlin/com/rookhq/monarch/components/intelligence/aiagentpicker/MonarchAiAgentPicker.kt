package com.rookhq.monarch.components.intelligence.aiagentpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import com.rookhq.monarch.components.intelligence.aiagentcard.LocalAgentCardChosen
import com.rookhq.monarch.components.intelligence.aiagentcard.LocalAgentCardTrailingInset
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.MonarchAiAccent
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.tokens.MonarchTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCard
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCardVariant
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Chooses one AI agent from compact agent cards, each with its avatar,
 * name, role, description, capabilities and tool count, so people see what
 * an agent is for before they choose it.
 *
 * A product pattern composed from Monarch parts: each option is a compact
 * `MonarchAiAgentCard` in a `Modifier.selectable` row with a
 * `MonarchRadio` as its mark, inside a `Modifier.selectableGroup`. TalkBack
 * reads each option as one radio button with the agent's name, role and
 * description, its selected state and its position in the group; the group
 * is named by [MonarchAiAgentPickerLabels.label]. The chosen card has the AI
 * accent border and a filled radio mark, so the choice is not colour alone.
 * An agent that cannot be chosen stays visible and is read as disabled.
 *
 * @param agents the agents, in display order.
 * @param selected the chosen agent's [MonarchAiAgentPickerOption.key], or `null` for none.
 * @param onSelect called with the key of the agent the person chooses; not called for the chosen one.
 * @param modifier the modifier of the group.
 * @param layout one agent per row, or two columns on wide windows.
 * @param enabled whether any agent can be chosen.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiAgentPicker(
    agents: List<MonarchAiAgentPickerOption>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    layout: MonarchAiAgentPickerLayout = MonarchAiAgentPickerLayout.List,
    enabled: Boolean = true,
    labels: MonarchAiAgentPickerLabels = MonarchAiAgentPickerLabels(),
) {
    val tokens = MonarchAiAgentPickerTokens
    val name = monarchString(labels.label, R.string.monarch_aiagentpicker_label)
    if (agents.isEmpty()) {
        Text(
            text = monarchString(labels.empty, R.string.monarch_aiagentpicker_empty),
            modifier = modifier,
            style = MonarchTheme.typography.body,
            color = tokens.emptyColor(MonarchTheme.colors),
        )
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val count = tokens.columns(layout, maxWidth, LocalDensity.current.fontScale)
        Column(
            modifier = Modifier
                .selectableGroup()
                .semantics {
                    isTraversalGroup = true
                    contentDescription = name
                },
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            agents.chunked(count).forEach { row ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                ) {
                    row.forEach { option ->
                        AiAgentPickerOptionRow(
                            option = option,
                            selected = option.key == selected,
                            enabled = tokens.choosable(option, enabled),
                            onClick = { if (option.key != selected) onSelect(option.key) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    repeat(count - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** One agent: the compact card and the radio mark, one selectable element. */
@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun AiAgentPickerOptionRow(
    option: MonarchAiAgentPickerOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val tokens = MonarchAiAgentPickerTokens
    val shape = MonarchTheme.shapes.lg
    val colors = MonarchTheme.colors
    val markSize = MonarchTokens.Space.space5.dp
    val markInset = MonarchTokens.Space.space5.dp
    MonarchAiAccent {
        Box(
            modifier = modifier
                .then(if (enabled) Modifier else Modifier.alpha(tokens.DISABLED_ALPHA))
                .monarchFocusRing(shape)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        ) {
            CompositionLocalProvider(
                LocalAgentCardChosen provides selected,
                LocalAgentCardTrailingInset provides (markSize + MonarchTokens.Space.space2.dp),
            ) {
                MonarchAiAgentCard(
                    agent = option.agent,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    image = option.image,
                    description = option.description,
                    capabilities = option.capabilities,
                    tools = option.tools,
                    state = option.state,
                    variant = MonarchAiAgentCardVariant.Compact,
                )
            }
            // The whole card is the control; the mark at its top end (the web's check in a circle) shows the state.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = markInset, end = markInset)
                    .size(markSize)
                    .then(
                        if (selected) {
                            Modifier.background(colors.aiSolid, CircleShape)
                        } else {
                            Modifier.border(tokens.SelectedBorderWidth, colors.borderInput, CircleShape)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    MonarchIcon(
                        MonarchGlyphs.Check,
                        contentDescription = null,
                        size = MonarchIconSize.Sm,
                        tint = colors.aiOnSolid,
                    )
                }
            }
        }
    }
}

package dev.monarchui.components.intelligence.aifollowupsuggestions

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchControlState
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * Follow-up questions shown after an assistant reply, as chips that wrap
 * onto new rows. Choosing one hands its text to the app.
 *
 * Monarch chips with the button role in a `FlowRow`, each 32dp high in a
 * 48dp touch target. Suggestions are
 * trimmed, and blank or repeated ones are left out; with none left nothing
 * is shown. A chip shows at most two lines and TalkBack reads its full
 * text. The chips have the surface fill, the assistant border and the
 * strong assistant text; a pressed chip takes the subtle assistant fill,
 * animated only when animations are on.
 *
 * @param suggestions questions the person might ask next, as complete prompts, already localised.
 * @param onSelect called with the text of the chosen suggestion.
 * @param modifier the modifier of the group.
 * @param enabled whether the chips can be chosen, for example `false` while a reply is generated.
 * @param label the TalkBack name of the group. `null` uses `monarch_aifollowupsuggestions_label`,
 * "Suggested follow-ups".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiFollowUpSuggestions(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val shown = remember(suggestions) { MonarchAiFollowUpSuggestionsTokens.visible(suggestions) }
    if (shown.isEmpty()) return
    val name = monarchString(label, R.string.monarch_aifollowupsuggestions_label)
    FlowRow(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = name
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchAiFollowUpSuggestionsTokens.Gap),
    ) {
        shown.forEach { suggestion ->
            FollowUpChip(text = suggestion, enabled = enabled, onClick = { onSelect(suggestion) })
        }
    }
}

@Composable
private fun FollowUpChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val state = rememberMonarchControlState(interactionSource, enabled)
    val target = MonarchAiFollowUpSuggestionsTokens.colors(state, MonarchTheme.colors)
    val spec = MonarchTheme.motion.spec<Color>(MonarchTokens.Duration.fast)
    val container by animateColorAsState(target.container, spec, label = "follow-up fill")
    val shape = MonarchShapeDefaults.Mark
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .monarchMinimumTouchTarget()
            .widthIn(max = MonarchAiFollowUpSuggestionsTokens.MaxChipWidth)
            .heightIn(min = MonarchAiFollowUpSuggestionsTokens.ChipHeight)
            .monarchFocusRing(shape)
            .background(container, shape)
            .border(MonarchShapeDefaults.BorderWidth, target.border ?: Color.Transparent, shape)
            .padding(
                horizontal = MonarchAiFollowUpSuggestionsTokens.ChipPaddingHorizontal,
                vertical = MonarchAiFollowUpSuggestionsTokens.ChipPaddingVertical,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MonarchTheme.typography.label,
            color = target.content,
            maxLines = MonarchAiFollowUpSuggestionsTokens.MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

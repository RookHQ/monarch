package dev.monarchui.components.intelligence.aistarterprompts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchControlState
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * Suggested ways to start a conversation, as cards in a grid or rows in a
 * list. Choosing one hands the starter to the app.
 *
 * Built by Monarch from Material 3 `Surface`s with a click, laid out in rows
 * of equal-height cells. It is not a lazy grid, because four to six starters
 * sit inside a screen that already scrolls. Each starter is one button that
 * TalkBack reads as its title and its prompt; the icon is decorative. A
 * pressed starter takes the subtle assistant fill and border, animated only
 * when animations are on. With no starters nothing is shown.
 *
 * @param prompts the starters, most useful first. Four to six work best.
 * @param onSelect called with the starter the user chose.
 * @param modifier the modifier of the group.
 * @param layout cards in a grid or full-width rows.
 * @param enabled whether the starters can be chosen, for example `false` while a reply is generated.
 * @param hidePrompt hides the prompt under each title. TalkBack then reads the title only.
 * @param label the TalkBack name of the group. `null` uses `monarch_aistarterprompts_label`,
 * "Suggested prompts".
 */
@Composable
public fun MonarchAiStarterPrompts(
    prompts: List<MonarchAiStarterPrompt>,
    onSelect: (MonarchAiStarterPrompt) -> Unit,
    modifier: Modifier = Modifier,
    layout: MonarchAiStarterPromptsLayout = MonarchAiStarterPromptsLayout.Grid,
    enabled: Boolean = true,
    hidePrompt: Boolean = false,
    label: String? = null,
) {
    if (prompts.isEmpty()) return
    val name = monarchString(label, R.string.monarch_aistarterprompts_label)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = name
        },
    ) {
        val columns = MonarchAiStarterPromptsTokens.columns(layout, maxWidth, fontScale)
        val gap = if (layout == MonarchAiStarterPromptsLayout.List) {
            MonarchAiStarterPromptsTokens.ListGap
        } else {
            MonarchAiStarterPromptsTokens.GridGap
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            prompts.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    row.forEach { starter ->
                        Starter(
                            starter = starter,
                            layout = layout,
                            enabled = enabled,
                            hidePrompt = hidePrompt,
                            onClick = { onSelect(starter) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    // Empty cells keep the last row's cards the width of the others.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Starter(
    starter: MonarchAiStarterPrompt,
    layout: MonarchAiStarterPromptsLayout,
    enabled: Boolean,
    hidePrompt: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state = rememberMonarchControlState(interactionSource, enabled)
    val theme = MonarchTheme.colors
    val target = MonarchAiStarterPromptsTokens.colors(state, theme)
    val spec = MonarchTheme.motion.spec<Color>(MonarchTokens.Duration.fast)
    val container by animateColorAsState(target.container, spec, label = "starter fill")
    val border by animateColorAsState(target.border ?: target.container, spec, label = "starter border")
    val shape = MonarchTheme.shapes.lg
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = MonarchAiStarterPromptsTokens.MinHeight)
            .monarchFocusRing(shape)
            .semantics { role = Role.Button },
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = target.content,
        border = BorderStroke(MonarchShapeDefaults.BorderWidth, border),
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = MonarchAiStarterPromptsTokens.MinHeight)
                .padding(MonarchAiStarterPromptsTokens.contentPadding(layout)),
            // The text sits in the middle of a card that is taller than it, not at the top.
            verticalArrangement = Arrangement.spacedBy(
                MonarchAiStarterPromptsTokens.ContentGap,
                Alignment.CenterVertically,
            ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchAiStarterPromptsTokens.IconGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                starter.icon?.let { icon ->
                    CompositionLocalProvider(
                        LocalContentColor provides MonarchAiStarterPromptsTokens.iconColor(enabled, theme),
                    ) {
                        Box(Modifier.clearAndSetSemantics { }) { icon() }
                    }
                }
                Text(
                    text = starter.title,
                    style = MonarchTheme.typography.label,
                )
            }
            if (!hidePrompt) {
                Text(
                    text = starter.prompt,
                    style = MonarchTheme.typography.caption,
                    color = MonarchAiStarterPromptsTokens.promptColor(enabled, theme),
                    maxLines = MonarchAiStarterPromptsTokens.PROMPT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

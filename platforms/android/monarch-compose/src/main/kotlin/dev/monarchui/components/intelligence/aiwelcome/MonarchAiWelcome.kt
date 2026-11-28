package dev.monarchui.components.intelligence.aiwelcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * The empty state of a new conversation: who the assistant is, a greeting,
 * what it can help with and where to start.
 *
 * A Monarch pattern: a centred `Column` with `MonarchAiAgentAvatar` (or an AI
 * mark), the title as a heading, an optional description, a list of
 * capabilities, the starters (usually `MonarchAiStarterPrompts`) and a footer
 * (usually `MonarchAiDisclaimer`). TalkBack reads the agent as one element
 * ("AI assistant: Dossier assistant, Writing assistant"), the title as a
 * heading, and the capabilities as one group named "What I can help with",
 * one element per capability. Use it as the empty content of the
 * conversation's message list.
 *
 * @param title the greeting, such as "How can I help?". A heading.
 * @param modifier the modifier of the welcome.
 * @param agent the assistant answering in this conversation: its avatar, name and role are shown.
 * @param agentImage the agent's picture. Monarch loads no images; load [MonarchAiAgentRef.avatar] in the app.
 * @param description one or two sentences on what the assistant can see or do.
 * @param capabilities short capability statements, shown as a checklist.
 * @param icon an AI glyph shown in the mark when there is no [agent], tinted in the AI colour. Monarch bundles
 * no icons; without it the mark is an empty AI tile.
 * @param capabilityIcon a check mark before each capability, tinted in the muted colour. Without it a dot is
 * drawn. Decorative.
 * @param capabilitiesLabel replaces the spoken name of the capabilities, "What I can help with".
 * @param agentPrefix replaces the spoken prefix before the agent's name, "AI assistant:".
 * @param footer content under the starters, such as `MonarchAiDisclaimer`.
 * @param starters starting points, such as `MonarchAiStarterPrompts`.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchAiWelcome(
    title: String,
    modifier: Modifier = Modifier,
    agent: MonarchAiAgentRef? = null,
    agentImage: Painter? = null,
    description: String? = null,
    capabilities: List<String> = emptyList(),
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    capabilityIcon: (@Composable () -> Unit)? = null,
    capabilitiesLabel: String? = null,
    agentPrefix: String? = null,
    footer: (@Composable () -> Unit)? = null,
    starters: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchAiWelcomeTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding)
            .semantics { isTraversalGroup = true },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (agent != null && agentImage != null) {
            MonarchAiAgentAvatar(agent = agent, image = agentImage, size = MonarchAvatarSize.Lg, decorative = true)
        } else if (icon != null) {
            // As the web: without a picture the mark is the assistant glyph in its tile, not the agent's initials.
            Mark(icon)
        }
        if (agent != null) {
            AgentLine(agent, monarchString(agentPrefix, R.string.monarch_aiwelcome_agent_prefix))
        }
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.title,
            color = colors.textDefault,
            textAlign = TextAlign.Center,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MonarchTheme.typography.body,
                color = tokens.mutedColor(colors),
                textAlign = TextAlign.Center,
            )
        }
        if (capabilities.isNotEmpty()) {
            Capabilities(
                capabilities,
                monarchString(capabilitiesLabel, R.string.monarch_aiwelcome_capabilities),
                capabilityIcon,
            )
        }
        if (starters != null) Box(Modifier.fillMaxWidth()) { starters() }
        if (footer != null) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { footer() }
    }
}

/** The AI tile shown without an agent. Decorative. */
@Composable
private fun Mark(icon: (@Composable () -> Unit)?) {
    val markColors = MonarchAiWelcomeTokens.markColors(MonarchTheme.colors)
    Box(
        modifier = Modifier
            .size(MonarchAiWelcomeTokens.MarkSize)
            .monarchSurface(markColors, MonarchShapeDefaults.Container)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) CompositionLocalProvider(LocalContentColor provides markColors.content, content = icon)
    }
}

/** The agent's name and role, read with the hidden prefix as one element. */
@Composable
private fun AgentLine(agent: MonarchAiAgentRef, prefix: String) {
    val tokens = MonarchAiWelcomeTokens
    val colors = MonarchTheme.colors
    val spoken = listOfNotNull(agent.name, agent.role?.takeIf { it.isNotBlank() }).joinToString(", ")
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$prefix $spoken" },
        horizontalArrangement = Arrangement.spacedBy(tokens.CapabilityGap, Alignment.CenterHorizontally),
    ) {
        Text(text = agent.name, style = MonarchTheme.typography.label, color = tokens.agentNameColor(colors))
        if (!agent.role.isNullOrBlank()) {
            Text(text = "·", style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
            Text(text = agent.role, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
        }
    }
}

/** The capabilities as a named group, one element each, aligned to the start. */
@Composable
private fun Capabilities(capabilities: List<String>, label: String, capabilityIcon: (@Composable () -> Unit)?) {
    val tokens = MonarchAiWelcomeTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = tokens.Gap)
            .semantics {
                contentDescription = label
                isTraversalGroup = true
            },
        verticalArrangement = Arrangement.spacedBy(tokens.CapabilityGap),
    ) {
        capabilities.forEach { capability ->
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(tokens.CapabilityGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                    if (capabilityIcon != null) {
                        CompositionLocalProvider(LocalContentColor provides tokens.mutedColor(colors)) {
                            capabilityIcon()
                        }
                    } else {
                        Box(
                            Modifier
                                .size(tokens.DotSize)
                                .background(tokens.mutedColor(colors), MonarchShapeDefaults.Mark),
                        )
                    }
                }
                Text(
                    text = capability,
                    modifier = Modifier.weight(1f),
                    style = MonarchTheme.typography.body,
                    color = colors.textDefault,
                )
            }
        }
    }
}

package com.rookhq.monarch.components.intelligence.aiagentcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * What an AI agent is and what it may do: its name and role, a description,
 * what it can do, the tools it can call, the data it can see and who owns it,
 * with optional actions. The compact variant summarises it for lists and
 * pickers.
 *
 * A product pattern composed from Monarch parts: `MonarchCard`,
 * `MonarchAiAgentAvatar` (decorative, since the name is written beside it; a
 * state other than idle is still read) and `MonarchBadge` for the
 * capabilities. The role line always says "AI agent", so the card never reads
 * as a person's profile. The name and the role line are one TalkBack heading;
 * the section titles are headings; each tool is one element read as its name
 * and what it does; the owner is read as "Owner, <name>".
 *
 * @param agent the agent: its name, role, codename and id.
 * @param modifier the modifier of the card.
 * @param image the agent's picture, loaded by the app; `null` shows the initials.
 * @param description one or two sentences on what the agent is for.
 * @param capabilities short capability phrases, shown as badges, such as "Drafts replies".
 * @param tools the tools the agent can call. The compact card shows only their count.
 * @param dataAccess what data the agent can read, such as "Documents in this space". Full card only.
 * @param owner who owns or maintains the agent. Full card only.
 * @param state what the agent is doing now, shown on its avatar.
 * @param variant every section, or the compact summary.
 * @param labels replacements for the built-in strings.
 * @param actions buttons at the end of the full card, such as "Start a chat". Not shown on the compact card.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiAgentCard(
    agent: MonarchAiAgentRef,
    modifier: Modifier = Modifier,
    image: Painter? = null,
    description: String? = null,
    capabilities: List<String> = emptyList(),
    tools: List<MonarchAiAgentTool> = emptyList(),
    dataAccess: List<String> = emptyList(),
    owner: String? = null,
    state: MonarchAiAgentState = MonarchAiAgentState.Idle,
    variant: MonarchAiAgentCardVariant = MonarchAiAgentCardVariant.Full,
    labels: MonarchAiAgentCardLabels = MonarchAiAgentCardLabels(),
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val tokens = MonarchAiAgentCardTokens
    val compact = variant == MonarchAiAgentCardVariant.Compact
    val capabilitiesTitle = monarchString(labels.capabilities, R.string.monarch_aiagentcard_capabilities)
    MonarchCard(modifier = modifier, selected = LocalAgentCardChosen.current) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            Header(agent, image, state, variant, tools.size, labels)
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MonarchTheme.typography.body,
                    color = tokens.nameColor(MonarchTheme.colors),
                )
            }
            if (compact) {
                if (capabilities.isNotEmpty()) {
                    Capabilities(
                        capabilities,
                        Modifier.semantics {
                            isTraversalGroup = true
                            contentDescription = capabilitiesTitle
                        },
                    )
                }
            } else {
                FullSections(capabilitiesTitle, capabilities, tools, dataAccess, owner, labels)
                if (actions != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) { actions() }
                }
            }
        }
    }
}

/** The avatar, the name and the role line, and the tool count on the compact card. */
@Composable
private fun Header(
    agent: MonarchAiAgentRef,
    image: Painter?,
    state: MonarchAiAgentState,
    variant: MonarchAiAgentCardVariant,
    toolCount: Int,
    labels: MonarchAiAgentCardLabels,
) {
    val tokens = MonarchAiAgentCardTokens
    val colors = MonarchTheme.colors
    val subtitle = tokens.subtitle(agent.role, monarchString(labels.kind, R.string.monarch_aiagentcard_kind))
    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAiAgentAvatar(
            agent = agent,
            image = image,
            state = state,
            size = tokens.avatarSize(variant),
            decorative = true,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    heading()
                    contentDescription = "${agent.name}, $subtitle"
                },
        ) {
            Text(text = agent.name, style = MonarchTheme.typography.bodyStrong, color = tokens.nameColor(colors))
            Text(text = subtitle, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
        }
        if (variant == MonarchAiAgentCardVariant.Compact && toolCount > 0) {
            Text(
                text = labels.toolCount?.invoke(toolCount)
                    ?: pluralStringResource(R.plurals.monarch_aiagentcard_tool_count, toolCount, toolCount),
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(colors),
                modifier = Modifier.padding(end = LocalAgentCardTrailingInset.current),
            )
        }
    }
}

/** Set by the agent picker: the card is the chosen one (the subtle fill and edge of a selected card). */
internal val LocalAgentCardChosen = androidx.compose.runtime.compositionLocalOf { false }

/** Set by the agent picker: room at the end of the compact header for the choice mark drawn over the card. */
internal val LocalAgentCardTrailingInset = androidx.compose.runtime.compositionLocalOf { MonarchTokens.Space.space0.dp }

@Composable
private fun FullSections(
    capabilitiesTitle: String,
    capabilities: List<String>,
    tools: List<MonarchAiAgentTool>,
    dataAccess: List<String>,
    owner: String?,
    labels: MonarchAiAgentCardLabels,
) {
    val tokens = MonarchAiAgentCardTokens
    val colors = MonarchTheme.colors
    val toolBar = colors.aiBorder
    if (capabilities.isNotEmpty()) {
        Section(capabilitiesTitle) { Capabilities(capabilities, Modifier) }
    }
    if (tools.isNotEmpty()) {
        Section(monarchString(labels.tools, R.string.monarch_aiagentcard_tools)) {
            tools.forEach { tool ->
                Column(
                    modifier = Modifier
                        .clearAndSetSemantics { contentDescription = tokens.toolText(tool) }
                        .drawBehind {
                            drawRect(toolBar, size = Size(tokens.ToolBarWidth.toPx(), size.height))
                        }
                        .padding(start = tokens.ItemGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.ToolGap),
                ) {
                    Text(text = tool.name, style = MonarchTheme.typography.bodyStrong, color = tokens.nameColor(colors))
                    if (!tool.description.isNullOrBlank()) {
                        Text(
                            text = tool.description,
                            style = MonarchTheme.typography.caption,
                            color = tokens.mutedColor(colors),
                        )
                    }
                }
            }
        }
    }
    if (dataAccess.isNotEmpty()) {
        Section(monarchString(labels.dataAccess, R.string.monarch_aiagentcard_data_access)) {
            dataAccess.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(tokens.BulletSize).background(tokens.mutedColor(colors), CircleShape))
                    Text(text = item, style = MonarchTheme.typography.body, color = tokens.nameColor(colors))
                }
            }
        }
    }
    if (!owner.isNullOrBlank()) {
        val label = monarchString(labels.owner, R.string.monarch_aiagentcard_owner)
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = tokens.ownerText(label, owner) },
            verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label, style = MonarchTheme.typography.label, color = tokens.headingColor(colors))
                MonarchAvatar(image = null, name = owner, size = MonarchAvatarSize.Xs, decorative = true)
                Text(text = owner, style = MonarchTheme.typography.body, color = tokens.nameColor(colors))
            }
        }
    }
}

/** A section title, a heading, and its items. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val tokens = MonarchAiAgentCardTokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.ItemGap)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.label,
            color = tokens.headingColor(MonarchTheme.colors),
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Capabilities(capabilities: List<String>, modifier: Modifier) {
    val gap = MonarchAiAgentCardTokens.ItemGap
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        capabilities.forEach { MonarchBadge(text = it, tone = MonarchTone.Ai) }
    }
}

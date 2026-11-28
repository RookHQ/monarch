package dev.monarchui.components.intelligence.aisubagenttree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/** One agent: the open and close button (or its space), then the selectable agent. */
@Composable
internal fun MonarchAiSubagentTreeRow(
    line: MonarchAiSubagentTreeLine,
    selected: Boolean,
    expandIcon: @Composable () -> Unit,
    statusIcon: (@Composable (MonarchAiSubagentStatus) -> Unit)?,
    texts: MonarchAiSubagentTreeTexts,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
) {
    val tokens = MonarchAiSubagentTreeTokens
    val colors = MonarchTheme.colors
    val agent = line.agent
    val status = texts.status(agent.status)
    val activity = agent.activity?.takeIf { agent.status == MonarchAiSubagentStatus.Working }
    val summary = agent.summary?.takeIf { MonarchAiSubagentTreeRules.showsSummary(agent.status) }
    val facts = texts.facts(agent)
    val description = (listOfNotNull(agent.name, status, agent.role, activity) + facts + listOfNotNull(summary))
        .joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(tokens.rowColor(selected, colors), MonarchShapeDefaults.Row)
            .padding(start = tokens.Indent * line.depth),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .width(tokens.SelectedMarkWidth)
                .fillMaxHeight()
                .background(if (selected) tokens.selectedMarkColor(colors) else Color.Transparent),
        )
        if (line.hasChildren) {
            Toggle(agent.name, line.open, expandIcon, texts, onToggle)
        } else {
            Spacer(Modifier.width(MonarchTouchTarget.Minimum))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MonarchTouchTarget.Minimum)
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .selectable(
                    selected = selected,
                    interactionSource = null,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onSelect,
                )
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(top = tokens.RowPadding, bottom = tokens.RowPadding, end = tokens.EndPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = agent.name, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
                Box(Modifier.clearAndSetSemantics { }) {
                    MonarchStatusBadge(
                        definition = MonarchStatusDefinition(
                            label = status,
                            tone = tokens.tone(agent.status),
                            icon = statusIcon?.let { icon -> { icon(agent.status) } },
                        ),
                        size = MonarchStatusBadgeSize.Sm,
                    )
                }
            }
            agent.role?.let { Text(text = it, style = MonarchTheme.typography.body, color = colors.textDefault) }
            activity?.let { Text(text = it, style = MonarchTheme.typography.caption, color = colors.aiStrong) }
            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString(" · "),
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
            summary?.let { Text(text = it, style = MonarchTheme.typography.caption, color = colors.textDefault) }
        }
    }
}

/** The open and close button of an agent with children; the chevron turns a quarter when open. */
@Composable
private fun Toggle(
    name: String,
    open: Boolean,
    expandIcon: @Composable () -> Unit,
    texts: MonarchAiSubagentTreeTexts,
    onToggle: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val turn = MonarchAiSubagentTreeTokens.EXPANDED_ROTATION
    val rotation by animateFloatAsState(
        targetValue = if (open) (if (rtl) -turn else turn) else 0f,
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.fast),
        label = "subagent tree chevron",
    )
    val label = texts.toggle(name, open)
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onToggle)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
            Box(Modifier.graphicsLayer { rotationZ = rotation }.clearAndSetSemantics { }) { expandIcon() }
        }
    }
}

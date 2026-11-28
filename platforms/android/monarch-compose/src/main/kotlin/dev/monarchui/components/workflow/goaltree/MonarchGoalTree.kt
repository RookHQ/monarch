package dev.monarchui.components.workflow.goaltree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * Nested goals (objectives and key results) with progress, status, owner and
 * alignment. Goals with children open and close, open goals can offer an
 * "Add key result" button, and each goal can carry its own actions.
 *
 * A Monarch pattern drawn as rows in a `Column` (put it in a scrolling
 * parent), built from `MonarchProgress` (a Material 3
 * `LinearProgressIndicator`), `MonarchStatusBadge`, `MonarchAvatar`,
 * `MonarchButton` and `MonarchEmptyState`. Each goal is one TalkBack element
 * whose description is its title, status, owner, facts and alignment, with its progress as
 * a progress value; the open and close button is a separate element. Status
 * is always in words, so it never depends on colour.
 *
 * @param goals every goal, flat. Nesting comes from [MonarchGoal.parentId]; order is kept among siblings.
 * @param expandIcon a chevron that points to the end, for example a `MonarchIcon` of size `Sm`. It turns to point
 * down when the goal is open. Monarch bundles no icons.
 * @param modifier the modifier of the tree.
 * @param expanded the ids of the open goals. `null` keeps them inside the tree, with every goal that has children
 * open at first.
 * @param onExpandedChange called with the new set of open ids when a goal opens or closes.
 * @param quietChildren hides the badge of children that are on track or not started; their status is still read.
 * @param canAddChild which goals get the add button, from the goal and its depth (0 at the top). Top-level goals when
 * `null`.
 * @param onAddChild adds an "Add key result" button inside the open goals [canAddChild] allows. Receives the parent;
 * the app adds the goal.
 * @param addIcon a decorative icon before the add button's text, such as a plus sign.
 * @param labels replacements for the built-in strings.
 * @param actions controls at the end of each goal's row, such as an icon button that opens a `DropdownMenu`.
 */
@Suppress("LongParameterList", "ModifierParameter") // The icon keeps its place, so positional callers compile.
@ExperimentalMonarchApi
@Composable
public fun MonarchGoalTree(
    goals: List<MonarchGoal>,
    expandIcon: @Composable () -> Unit = MonarchDefaultIcons.ChevronRight,
    modifier: Modifier = Modifier,
    expanded: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    quietChildren: Boolean = true,
    canAddChild: ((goal: MonarchGoal, depth: Int) -> Boolean)? = null,
    onAddChild: ((MonarchGoal) -> Unit)? = null,
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Plus,
    labels: MonarchGoalTreeLabels = MonarchGoalTreeLabels(),
    actions: (@Composable (MonarchGoal) -> Unit)? = null,
) {
    if (goals.isEmpty()) {
        MonarchEmptyState(
            title = monarchString(labels.empty, R.string.monarch_goaltree_empty),
            modifier = modifier,
            size = MonarchEmptyStateSize.Sm,
        )
        return
    }
    var inside by rememberSaveable(saver = expandedSaver) {
        mutableStateOf(MonarchGoalTreeTokens.initiallyExpanded(goals))
    }
    val open = expanded ?: inside
    val canAdd: (MonarchGoal, Int) -> Boolean = { goal, depth ->
        onAddChild != null && (canAddChild?.invoke(goal, depth) ?: (depth == 0))
    }
    val lines = remember(goals, open, onAddChild, canAddChild) { MonarchGoalTreeTokens.lines(goals, open, canAdd) }
    val toggle: (String) -> Unit = { id ->
        val next = MonarchGoalTreeTokens.toggled(open, id)
        if (expanded == null) inside = next
        onExpandedChange?.invoke(next)
    }
    Column(modifier = modifier) {
        lines.forEach { line ->
            when (line) {
                is GoalTreeLine.Goal -> GoalRow(line, quietChildren, expandIcon, labels, toggle, actions)
                is GoalTreeLine.Add -> AddRow(line, addIcon, labels) { onAddChild?.invoke(line.parent) }
            }
        }
    }
}

private val expandedSaver = listSaver<MutableState<Set<String>>, String>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun GoalRow(
    line: GoalTreeLine.Goal,
    quietChildren: Boolean,
    expandIcon: @Composable () -> Unit,
    labels: MonarchGoalTreeLabels,
    onToggle: (String) -> Unit,
    actions: (@Composable (MonarchGoal) -> Unit)?,
) {
    val goal = line.goal
    val tokens = MonarchGoalTreeTokens
    val statusName = statusName(goal.status, labels)
    val locale = LocalConfiguration.current.locales[0]
    val percentText = remember(goal.progress, locale) {
        NumberFormat.getPercentInstance(locale).format(tokens.fraction(goal.progress).toDouble())
    }
    val countText = if (!line.open && line.childCount > 0) childCount(line.childCount, labels) else null
    val alignedText = goal.alignedTo?.let {
        labels.alignedTo?.invoke(it) ?: stringResource(R.string.monarch_goaltree_aligned_to, it)
    }
    val description = listOfNotNull(goal.title, statusName, goal.ownerName, goal.meta, alignedText, countText)
        .joinToString(", ")
    Row(
        modifier = Modifier.padding(
            start = tokens.Indent * line.depth,
            top = tokens.RowPadding,
            bottom = tokens.RowPadding,
        ),
        verticalAlignment = Alignment.Top,
    ) {
        if (line.canToggle) {
            ToggleButton(goal.title, line.open, expandIcon, labels) { onToggle(goal.id) }
        } else {
            Spacer(Modifier.width(MonarchTouchTarget.Minimum))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(tokens.fraction(goal.progress), 0f..1f)
                    stateDescription = percentText
                },
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                goal.ownerName?.let {
                    MonarchAvatar(image = goal.ownerImage, name = it, size = MonarchAvatarSize.Sm, decorative = true)
                }
                MonarchText(
                    text = goal.title,
                    style = if (line.depth == 0) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body,
                )
                if (tokens.showsBadge(goal.status, line.depth, quietChildren)) {
                    // The status is in the goal's description; the badge would otherwise be its own element.
                    Box(Modifier.clearAndSetSemantics { }) {
                        MonarchStatusBadge(
                            definition = MonarchStatusDefinition(label = statusName, tone = tokens.tone(goal.status)),
                            size = MonarchStatusBadgeSize.Sm,
                        )
                    }
                }
                goal.meta?.let { Caption(it) }
                countText?.let { Caption(it) }
            }
            alignedText?.let {
                Row(
                    modifier = Modifier.clearAndSetSemantics { },
                    horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
                        MonarchDefaultIcons.Link()
                    }
                    Caption(it)
                }
            }
            Row(
                modifier = Modifier.clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchProgress(
                    modifier = Modifier.weight(1f),
                    progress = tokens.fraction(goal.progress),
                    tone = tokens.barTone(goal.status),
                    size = MonarchProgressSize.Sm,
                )
                Caption(percentText)
            }
        }
        actions?.invoke(goal)
    }
}

@Composable
private fun Caption(text: String) {
    MonarchText(text = text, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
}

@Composable
private fun ToggleButton(
    title: String,
    open: Boolean,
    expandIcon: @Composable () -> Unit,
    labels: MonarchGoalTreeLabels,
    onToggle: () -> Unit,
) {
    val name = if (open) {
        labels.collapse?.invoke(title) ?: stringResource(R.string.monarch_goaltree_collapse, title)
    } else {
        labels.expand?.invoke(title) ?: stringResource(R.string.monarch_goaltree_expand, title)
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val turn = if (rtl) -MonarchGoalTreeTokens.EXPANDED_ROTATION else MonarchGoalTreeTokens.EXPANDED_ROTATION
    val rotation by animateFloatAsState(
        targetValue = if (open) turn else 0f,
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.fast),
        label = "goal tree expand icon",
    )
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onToggle)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
            Box(Modifier.graphicsLayer { rotationZ = rotation }.clearAndSetSemantics { }) { expandIcon() }
        }
    }
}

@Composable
private fun AddRow(
    line: GoalTreeLine.Add,
    addIcon: (@Composable () -> Unit)?,
    labels: MonarchGoalTreeLabels,
    onClick: () -> Unit,
) {
    val text = monarchString(labels.addChild, R.string.monarch_goaltree_add_child)
    val name = labels.addChildTo?.invoke(line.parent.title)
        ?: stringResource(R.string.monarch_goaltree_add_child_to, line.parent.title)
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = Modifier
            .padding(start = MonarchGoalTreeTokens.Indent * line.depth + MonarchTouchTarget.Minimum)
            .semantics { contentDescription = name },
        variant = MonarchButtonVariant.Link,
        size = MonarchButtonSize.Sm,
        leadingIcon = addIcon,
    )
}

@Composable
private fun statusName(status: MonarchGoalStatus, labels: MonarchGoalTreeLabels): String =
    labels.status?.invoke(status) ?: stringResource(
        when (status) {
            MonarchGoalStatus.NotStarted -> R.string.monarch_goaltree_status_not_started
            MonarchGoalStatus.OnTrack -> R.string.monarch_goaltree_status_on_track
            MonarchGoalStatus.AtRisk -> R.string.monarch_goaltree_status_at_risk
            MonarchGoalStatus.OffTrack -> R.string.monarch_goaltree_status_off_track
            MonarchGoalStatus.Done -> R.string.monarch_goaltree_status_done
        },
    )

@Composable
private fun childCount(count: Int, labels: MonarchGoalTreeLabels): String =
    labels.childCount?.invoke(count) ?: pluralStringResource(R.plurals.monarch_goaltree_child_count, count, count)

package dev.monarchui.components.intelligence.aiknowledgescope

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import dev.monarchui.tokens.MonarchTokens
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.MonarchAiAccent
import dev.monarchui.theme.MonarchTheme

/**
 * A tree of spaces, documents and apps with checkboxes, where a person picks
 * what grounds the assistant's answers.
 *
 * Built from three-state `MonarchCheckbox` rows in a column: checking a group
 * selects everything in it, and a group with only some of its contents
 * selected shows the mixed state. Each row is one TalkBack element with the
 * checkbox role, named "Handbook, Space" and read as checked, not checked or
 * partially checked. A group has a separate 48dp button before its row that
 * opens and closes it ("Show contents of Handbook"), and the row offers the
 * TalkBack expand and collapse actions. The tree is not lazy, so it can sit
 * in a scrolling screen; for hundreds of sources, use a search instead.
 *
 * @param sources the sources as a tree.
 * @param selection the ids of the selected sources. A group's id stands for everything in it.
 * @param onSelectionChange called with the new selection after a toggle: every fully selected source, groups
 * included, in tree order. Ids that are not in the tree are kept at the end.
 * @param expandIcon a chevron that points to the end, for example a `MonarchIcon` of size `Sm` with a `null`
 * content description and `mirrorInRtl = true`. It turns a quarter turn while a group is open. Drawn by default,
 * as on the web.
 * @param modifier the modifier of the tree.
 * @param enabled `false` makes the checkboxes impossible to change; groups can still be opened.
 * @param initiallyExpanded the ids of the groups that start open. Later opening and closing is kept across
 * configuration changes.
 * @param showSummary shows "3 sources selected" under the tree. A fully selected group counts as one.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("ModifierParameter") // The icon keeps its place before the modifier, so positional callers still compile.
public fun MonarchAiKnowledgeScope(
    sources: List<MonarchAiKnowledgeSource>,
    selection: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    expandIcon: @Composable () -> Unit = MonarchDefaultIcons.ChevronRight,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initiallyExpanded: Set<String> = emptySet(),
    showSummary: Boolean = true,
    labels: MonarchAiKnowledgeScopeLabels = MonarchAiKnowledgeScopeLabels(),
) {
    val logic = MonarchAiKnowledgeScopeLogic
    var expanded by rememberSaveable { mutableStateOf<List<String>>(ArrayList(initiallyExpanded)) }
    val states = logic.states(sources, selection)
    val treeLabel = monarchString(labels.label, R.string.monarch_aiknowledgescope_label)
    Column(
        modifier = modifier.semantics {
            contentDescription = treeLabel
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(MonarchAiKnowledgeScopeTokens.SummaryGap),
    ) {
        if (sources.isEmpty()) {
            MonarchEmptyState(
                title = monarchString(labels.empty, R.string.monarch_aiknowledgescope_empty),
                size = MonarchEmptyStateSize.Sm,
            )
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    MonarchShapeDefaults.BorderWidth,
                    MonarchTheme.colors.borderSubtle,
                    MonarchShapeDefaults.Container,
                )
                .padding(vertical = MonarchTokens.Space.space1.dp),
        ) {
            logic.rows(sources, expanded).forEach { row ->
                val id = row.source.id
                SourceRow(
                    row = row,
                    state = states[id] ?: ToggleableState.Off,
                    open = id in expanded,
                    enabled = enabled,
                    expandIcon = expandIcon,
                    labels = labels,
                    onToggle = { onSelectionChange(logic.toggle(sources, selection, id)) },
                    onOpen = { expanded = ArrayList(logic.toggleExpanded(expanded, id)) },
                )
            }
        }
        if (showSummary) {
            val count = logic.count(sources, states)
            Text(
                text = labels.summary?.invoke(count) ?: if (count == 0) {
                    stringResource(R.string.monarch_aiknowledgescope_summary_none)
                } else {
                    pluralStringResource(R.plurals.monarch_aiknowledgescope_summary, count, count)
                },
                style = MonarchTheme.typography.caption,
                color = MonarchAiKnowledgeScopeTokens.secondaryColor(enabled = true, MonarchTheme.colors),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun SourceRow(
    row: AiKnowledgeScopeRow,
    state: ToggleableState,
    open: Boolean,
    enabled: Boolean,
    expandIcon: @Composable () -> Unit,
    labels: MonarchAiKnowledgeScopeLabels,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val source = row.source
    val tokens = MonarchAiKnowledgeScopeTokens
    val colors = MonarchTheme.colors
    val kindName = labels.kind?.invoke(source.kind) ?: stringResource(tokens.kindRes(source.kind))
    val itemName = labels.itemLabel?.invoke(source.label, kindName)
        ?: stringResource(R.string.monarch_aiknowledgescope_item, source.label, kindName)
    val spoken = listOfNotNull(itemName, source.description).joinToString(", ")
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = tokens.Indent * (row.level - 1)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (source.isGroup) {
            DisclosureButton(source.label, open, expandIcon, labels, onOpen)
        } else {
            Spacer(Modifier.size(MonarchTouchTarget.Minimum))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MonarchTouchTarget.Minimum)
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .triStateToggleable(state = state, enabled = enabled, role = Role.Checkbox, onClick = onToggle)
                .semantics {
                    contentDescription = spoken
                    if (source.isGroup) {
                        if (open) {
                            collapse {
                                onOpen()
                                true
                            }
                        } else {
                            expand {
                                onOpen()
                                true
                            }
                        }
                    }
                }
                .padding(vertical = tokens.VerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchAiAccent { MonarchCheckbox(state = state, onClick = null, enabled = enabled) }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.TextGap),
            ) {
                Text(
                    text = source.label,
                    style = MonarchTheme.typography.body,
                    color = tokens.labelColor(enabled, colors),
                )
                source.description?.let {
                    Text(
                        text = it,
                        style = MonarchTheme.typography.caption,
                        color = tokens.secondaryColor(enabled, colors),
                    )
                }
            }
            // As on the web, the kind sits at the end of the row.
            Text(
                text = kindName,
                modifier = Modifier.padding(end = MonarchTokens.Space.space3.dp).clearAndSetSemantics { },
                style = MonarchTheme.typography.caption,
                color = tokens.secondaryColor(enabled, colors),
            )
        }
    }
}

@Composable
private fun DisclosureButton(
    label: String,
    open: Boolean,
    expandIcon: @Composable () -> Unit,
    labels: MonarchAiKnowledgeScopeLabels,
    onOpen: () -> Unit,
) {
    val name = if (open) {
        labels.collapse?.invoke(label) ?: stringResource(R.string.monarch_aiknowledgescope_collapse, label)
    } else {
        labels.expand?.invoke(label) ?: stringResource(R.string.monarch_aiknowledgescope_expand, label)
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val turn = MonarchAiKnowledgeScopeTokens.EXPANDED_ROTATION
    val rotation by animateFloatAsState(
        targetValue = if (open) (if (rtl) -turn else turn) else 0f,
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.fast),
        label = "knowledge scope chevron",
    )
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onOpen)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MonarchAiKnowledgeScopeTokens.iconColor(MonarchTheme.colors),
        ) {
            Box(Modifier.graphicsLayer { rotationZ = rotation }.clearAndSetSemantics { }) { expandIcon() }
        }
    }
}

package dev.monarchui.components.search.querybuilder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEditor
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEditorLabels
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionEditorSegments
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionField
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchRemoveButton
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

private typealias Tokens = MonarchQueryBuilderTokens

/**
 * Builds a filter query from AND / OR groups of field, operator and value rules, nested up to [maxDepth].
 *
 * Built by Monarch as nested `Column`s: each group is an outlined card with its name as a heading, a
 * "Join rules with [AND | OR]" Material 3 `SingleChoiceSegmentedButtonRow`, its rules as a
 * `MonarchConditionEditor` (so rules get the same typed operators and value inputs everywhere), its sub-groups,
 * and an Add group button. Rules stack vertically, one control per line, to fit a phone; each nested level is
 * indented by the group padding and alternates its fill. A new sub-group starts with the opposite combinator of
 * its parent.
 *
 * The query is controlled: every change calls [onValueChange] with the whole new top group. Validate with
 * [MonarchQueryGroup.problems] and preview with [MonarchQueryGroup.matches].
 *
 * TalkBack reads each group name ("Top group", "Group 1", "Group 1.2") and each rule ("Rule 1") as headings; the
 * AND / OR switch is named "Top group combinator"; remove buttons name what they remove ("Remove Group 1",
 * "Remove rule 1 of Top group").
 *
 * @param value the query: the top group.
 * @param onValueChange called with the whole new query after every change.
 * @param fields the fields a rule can test.
 * @param modifier the modifier of the builder.
 * @param maxDepth how many levels of groups are allowed, counting the top group. 1 turns off sub-groups.
 * @param maxRulesPerGroup the most rules in one group.
 * @param showErrors shows what is missing on each incomplete rule, for example after a submit attempt.
 * @param enabled whether the query can be changed.
 * @param createId makes the id of a new rule or group. By default the first free `q-<n>` in the query.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the buttons read
 * "Remove".
 * @param addIcon an icon before the text of the Add rule and Add group buttons.
 * @param labels replacements for the built-in strings.
 * @param ruleLabels replacements for the strings of the rule editors, such as operator names and problems. Rule
 * headings, remove names, Add rule and the empty note come from [labels].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchQueryBuilder(
    value: MonarchQueryGroup,
    onValueChange: (MonarchQueryGroup) -> Unit,
    fields: List<MonarchConditionField>,
    modifier: Modifier = Modifier,
    maxDepth: Int = Tokens.DEFAULT_MAX_DEPTH,
    maxRulesPerGroup: Int = Tokens.DEFAULT_MAX_RULES,
    showErrors: Boolean = false,
    enabled: Boolean = true,
    createId: (() -> String)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Plus,
    labels: MonarchQueryBuilderLabels = MonarchQueryBuilderLabels(),
    ruleLabels: MonarchConditionEditorLabels = MonarchConditionEditorLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchQueryBuilderText(labels, resources) }
    val context = QueryBuilderContext(
        fields = fields,
        maxDepth = maxDepth,
        maxRulesPerGroup = maxRulesPerGroup,
        showErrors = showErrors,
        enabled = enabled,
        nextId = { createId?.invoke() ?: Tokens.freshId(value) },
        removeIcon = removeIcon,
        addIcon = addIcon,
        text = text,
        ruleLabels = ruleLabels,
    )
    Column(modifier = modifier.semantics { contentDescription = text.label }) {
        QueryGroup(group = value, path = listOf(1), onChange = onValueChange, onRemove = null, context = context)
    }
}

/** What every group of one builder shares. */
@Suppress("LongParameterList")
private class QueryBuilderContext(
    val fields: List<MonarchConditionField>,
    val maxDepth: Int,
    val maxRulesPerGroup: Int,
    val showErrors: Boolean,
    val enabled: Boolean,
    val nextId: () -> String,
    val removeIcon: (@Composable () -> Unit)?,
    val addIcon: (@Composable () -> Unit)?,
    val text: MonarchQueryBuilderText,
    val ruleLabels: MonarchConditionEditorLabels,
)

/** One group: its heading and switch, its rules, its sub-groups and Add group. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMonarchApi::class)
@Composable
private fun QueryGroup(
    group: MonarchQueryGroup,
    path: List<Int>,
    onChange: (MonarchQueryGroup) -> Unit,
    onRemove: (() -> Unit)?,
    context: QueryBuilderContext,
) {
    val text = context.text
    val colors = MonarchTheme.colors
    val name = text.group(path)
    val depth = path.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(Tokens.groupColors(colors, depth), MonarchShapeDefaults.Container)
            .padding(Tokens.GroupPadding),
        verticalArrangement = Arrangement.spacedBy(Tokens.GroupGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonarchText(text = name, modifier = Modifier.weight(1f), style = MonarchTextStyle.Subheading)
            if (onRemove != null) {
                MonarchRemoveButton(
                    name = text.removeGroup(name),
                    text = text.remove,
                    onClick = onRemove,
                    enabled = context.enabled,
                    icon = context.removeIcon,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.JoinGap),
            verticalArrangement = Arrangement.spacedBy(Tokens.JoinGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = text.combinatorBefore, color = Tokens.noteColor(colors))
            MonarchConditionEditorSegments(
                options = listOf(MonarchQueryCombinator.And to text.and, MonarchQueryCombinator.Or to text.or),
                selected = group.combinator,
                onSelect = { onChange(group.copy(combinator = it)) },
                enabled = context.enabled,
                name = text.combinatorLabel(name),
            )
        }
        MonarchConditionEditor(
            value = group.rules,
            onValueChange = { onChange(group.copy(rules = it)) },
            fields = context.fields,
            maxConditions = context.maxRulesPerGroup,
            showErrors = context.showErrors,
            enabled = context.enabled,
            createId = context.nextId,
            removeIcon = context.removeIcon,
            addIcon = context.addIcon,
            labels = remember(context.ruleLabels, name, text) { text.ruleLabels(context.ruleLabels, name) },
        )
        group.groups.forEachIndexed { index, child ->
            key(child.id) {
                QueryGroup(
                    group = child,
                    path = path + (index + 1),
                    onChange = { next ->
                        onChange(group.copy(groups = group.groups.map { if (it.id == child.id) next else it }))
                    },
                    onRemove = { onChange(group.copy(groups = group.groups.filterNot { it.id == child.id })) },
                    context = context,
                )
            }
        }
        if (depth < context.maxDepth) {
            MonarchButton(
                text = text.addGroup,
                onClick = {
                    val child = MonarchQueryGroup(
                        id = context.nextId(),
                        combinator = Tokens.childCombinator(group.combinator),
                    )
                    onChange(group.copy(groups = group.groups + child))
                },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = context.enabled && context.fields.isNotEmpty(),
                leadingIcon = context.addIcon,
            )
        } else if (context.maxDepth > 1) {
            MonarchText(text = text.maxDepthReached, color = Tokens.noteColor(colors))
        }
    }
}

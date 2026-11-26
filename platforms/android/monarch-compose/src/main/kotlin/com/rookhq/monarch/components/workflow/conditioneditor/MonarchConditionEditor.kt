package com.rookhq.monarch.components.workflow.conditioneditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.theme.MonarchTheme

private typealias Tokens = MonarchConditionEditorTokens

/**
 * Edits a list of conditions: a field, an operator and a value input that fits the field (text, a number, a
 * day, one choice, several choices, free values, or none), with an all / any switch. Fields are data, so the
 * same editor serves ticket rules, form logic and eligibility rules.
 *
 * Built by Monarch as a `Column` of outlined condition cards, one control per line so each fits a phone. The
 * field and the operator are `MonarchSelect` menus; the value is a `MonarchTextField` (text, or a number with
 * the number keyboard), a `MonarchSelect` (one option), a `MonarchMultiSelect` sheet (several options, or free
 * values for tags and "is any of" on text), or a `MonarchButton` that opens the Material 3 `DatePickerDialog`
 * (a day). The all / any switch is a Material 3 `SingleChoiceSegmentedButtonRow`, shown from two conditions on
 * when [match] and [onMatchChange] are given. Changing the field starts the condition again on the field's
 * first operator; changing the operator keeps the value when it has the same shape.
 *
 * The list is controlled: every change calls [onValueChange] with the whole new list. Validation is the app's
 * call: with [showErrors], each incomplete condition shows its problem on the control that needs attention, as
 * [MonarchCondition.problem] reports it. Use [MonarchCondition.evaluate] to preview which records match.
 *
 * TalkBack reads each condition's heading ("Condition 1") as a heading, each control with its label, and the
 * remove button as "Remove condition 1".
 *
 * @param value the conditions, in order. Ids must be unique.
 * @param onValueChange called with the whole new list after every change.
 * @param fields the fields a condition can test.
 * @param modifier the modifier of the editor.
 * @param match all or any, shown as a switch with [onMatchChange] once there are two conditions.
 * @param onMatchChange called with the new choice of the all / any switch.
 * @param maxConditions the most conditions; the add button is replaced by a note at this count.
 * @param showErrors shows the problem of each incomplete condition, for example after a save attempt.
 * @param defaultField the key of the field new conditions start on. The first enabled field by default.
 * @param enabled whether the conditions can be changed.
 * @param createId makes the id of a new condition. By default the first free `condition-<n>`.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the button reads
 * "Remove".
 * @param addIcon an icon before the text of the add button.
 * @param emptyContent replaces the note shown when there are no conditions.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchConditionEditor(
    value: List<MonarchCondition>,
    onValueChange: (List<MonarchCondition>) -> Unit,
    fields: List<MonarchConditionField>,
    modifier: Modifier = Modifier,
    match: MonarchConditionMatch? = null,
    onMatchChange: ((MonarchConditionMatch) -> Unit)? = null,
    maxConditions: Int = Tokens.DEFAULT_MAX,
    showErrors: Boolean = false,
    defaultField: String? = null,
    enabled: Boolean = true,
    createId: (() -> String)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Plus,
    emptyContent: (@Composable () -> Unit)? = null,
    labels: MonarchConditionEditorLabels = MonarchConditionEditorLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchConditionEditorText(labels, resources) }
    val colors = MonarchTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap)) {
        if (match != null && onMatchChange != null && value.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tokens.MatchGap),
                verticalArrangement = Arrangement.spacedBy(Tokens.MatchGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(text = text.matchBefore)
                MonarchConditionEditorSegments(
                    options = listOf(
                        MonarchConditionMatch.All to text.matchAll,
                        MonarchConditionMatch.Any to text.matchAny,
                    ),
                    selected = match,
                    onSelect = onMatchChange,
                    enabled = enabled,
                )
                MonarchText(text = text.matchAfter)
            }
        }
        if (value.isEmpty()) {
            if (emptyContent != null) {
                emptyContent()
            } else {
                MonarchText(text = text.empty, color = Tokens.noteColor(colors))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = text.label },
                verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap),
            ) {
                value.forEachIndexed { index, condition ->
                    key(condition.id) {
                        MonarchConditionEditorRow(
                            condition = condition,
                            position = index + 1,
                            fields = fields,
                            problem = if (showErrors) condition.problem(fields) else null,
                            enabled = enabled,
                            text = text,
                            removeIcon = removeIcon,
                            onChange = { next -> onValueChange(value.map { if (it.id == condition.id) next else it }) },
                            onRemove = { onValueChange(value.filterNot { it.id == condition.id }) },
                        )
                    }
                }
            }
        }
        if (value.size < maxConditions) {
            MonarchButton(
                text = text.add,
                onClick = {
                    val id = createId?.invoke() ?: Tokens.freshId("condition", value.map { it.id })
                    onValueChange(value + MonarchCondition.create(id, Tokens.startField(fields, defaultField)))
                },
                size = MonarchButtonSize.Sm,
                enabled = enabled && fields.any { it.enabled },
                leadingIcon = addIcon,
            )
        } else {
            MonarchText(text = text.maxReached(maxConditions), color = Tokens.noteColor(colors))
        }
    }
}

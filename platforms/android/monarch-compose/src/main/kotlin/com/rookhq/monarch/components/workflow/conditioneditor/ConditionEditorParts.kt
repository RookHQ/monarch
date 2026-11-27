package com.rookhq.monarch.components.workflow.conditioneditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchRemoveButton
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

private typealias RowTokens = MonarchConditionEditorTokens

/** One condition: its heading and remove button, the field and operator menus, and the value input. */
@Composable
@Suppress("LongParameterList")
internal fun MonarchConditionEditorRow(
    condition: MonarchCondition,
    position: Int,
    fields: List<MonarchConditionField>,
    problem: MonarchConditionProblem?,
    enabled: Boolean,
    text: MonarchConditionEditorText,
    removeIcon: (@Composable () -> Unit)?,
    onChange: (MonarchCondition) -> Unit,
    onRemove: () -> Unit,
) {
    val field = fields.firstOrNull { it.key == condition.field }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(RowTokens.rowColors(MonarchTheme.colors, problem != null), MonarchShapeDefaults.Container)
            .padding(RowTokens.RowPadding),
        verticalArrangement = Arrangement.spacedBy(RowTokens.ControlGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonarchText(
                text = text.condition(position),
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.Subheading,
            )
            MonarchRemoveButton(
                name = text.removeItem(position),
                text = text.remove,
                onClick = onRemove,
                enabled = enabled,
                icon = removeIcon,
            )
        }
        MonarchSelect(
            options = RowTokens.fieldOptions(fields, condition.field, text::unknownField),
            selected = condition.field.takeIf { it.isNotEmpty() },
            onSelect = { key ->
                if (key != condition.field) {
                    onChange(MonarchCondition.create(condition.id, fields.firstOrNull { it.key == key }))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = text.field,
            placeholder = text.choose,
            errorText = if (problem == MonarchConditionProblem.Field) text.problem(problem) else null,
            enabled = enabled,
        )
        MonarchSelect(
            options = RowTokens.operatorChoices(field, condition.operator).map {
                MonarchSelectOption(it, text.operator(it))
            },
            selected = condition.operator,
            onSelect = { op ->
                if (op != condition.operator) onChange(MonarchConditionEvaluation.withOperator(condition, op, field))
            },
            modifier = Modifier.fillMaxWidth(),
            label = text.operator,
            enabled = enabled,
        )
        if (condition.operator.takesValue) {
            MonarchConditionEditorValue(
                condition = condition,
                field = field,
                errorText = problem?.takeIf { it != MonarchConditionProblem.Field }?.let(text::problem),
                enabled = enabled,
                text = text,
                onValueChange = { onChange(condition.copy(value = it)) },
            )
        }
    }
}

/**
 * A `MonarchSegmentedControl` of [options], for the all / any and AND / OR switches. [name]
 * is its TalkBack name, when the words around it do not already say what it is.
 */
@Composable
internal fun <T> MonarchConditionEditorSegments(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    enabled: Boolean,
    name: String? = null,
) {
    MonarchSegmentedControl(
        modifier = if (name != null) Modifier.semantics { contentDescription = name } else Modifier,
    ) {
        options.forEach { (option, label) ->
            Segment(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = label,
                enabled = enabled,
            )
        }
    }
}

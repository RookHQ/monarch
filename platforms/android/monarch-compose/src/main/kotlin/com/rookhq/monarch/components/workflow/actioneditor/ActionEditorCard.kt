package com.rookhq.monarch.components.workflow.actioneditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchRemoveButton
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

private typealias CardTokens = MonarchActionEditorTokens

/** One action: heading with move and remove buttons, the type menu, a warning, and the parameter inputs. */
@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun MonarchActionEditorCard(
    action: MonarchRuleAction,
    position: Int,
    count: Int,
    actionTypes: List<MonarchActionType>,
    showErrors: Boolean,
    enabled: Boolean,
    text: MonarchActionEditorText,
    removeIcon: (@Composable () -> Unit)?,
    moveUpIcon: (@Composable () -> Unit)?,
    moveDownIcon: (@Composable () -> Unit)?,
    onChange: (MonarchRuleAction) -> Unit,
    onRemove: () -> Unit,
    onMove: ((Int) -> Unit)?,
) {
    val type = actionTypes.firstOrNull { it.key == action.type }
    val problems = if (showErrors) action.problems(actionTypes) else emptyList()
    val setParams = { parameters: Map<String, MonarchActionValue> -> onChange(action.copy(parameters = parameters)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(
                CardTokens.cardColors(MonarchTheme.colors, invalid = problems.isNotEmpty()),
                MonarchShapeDefaults.Container,
            )
            .padding(CardTokens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(CardTokens.ControlGap),
    ) {
        // The web's heading: a grip, the name, the remove button and the move buttons.
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = MonarchTouchTarget.Minimum),
            horizontalArrangement = Arrangement.spacedBy(CardTokens.ControlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val headingColor = CardTokens.headingColor(MonarchTheme.colors, invalid = problems.isNotEmpty())
            if (onMove != null) {
                Box(Modifier.clearAndSetSemantics { }) {
                    CompositionLocalProvider(
                        LocalContentColor provides headingColor,
                        content = MonarchDefaultIcons.Grip,
                    )
                }
            }
            MonarchText(
                text = text.action(position),
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.Caption,
                color = headingColor,
                heading = true,
            )
            MonarchRemoveButton(
                name = text.removeItem(position),
                text = text.remove,
                onClick = onRemove,
                enabled = enabled,
                icon = removeIcon,
            )
            // As the web: Move up and Move down follow the remove button in the heading.
            if (onMove != null) {
                MoveButton(text.moveUp, text.moveUpItem(position), enabled && position > 1, moveUpIcon) {
                    onMove(position - 2)
                }
                MoveButton(text.moveDown, text.moveDownItem(position), enabled && position < count, moveDownIcon) {
                    onMove(position)
                }
            }
        }
        MonarchSelect(
            options = CardTokens.typeOptions(actionTypes, action.type, text::unknownType),
            selected = action.type.takeIf { it.isNotEmpty() },
            onSelect = { key ->
                if (key != action.type) {
                    onChange(MonarchRuleAction.create(action.id, actionTypes.firstOrNull { it.key == key }))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = text.type,
            placeholder = text.choose,
            helperText = type?.description,
            enabled = enabled,
        )
        if (type != null && !type.enabled) {
            MonarchInlineNotice(
                message = listOfNotNull(text.unavailable, type.disabledReason).joinToString(" "),
                tone = MonarchTone.Warning,
            )
        }
        val editor = type?.editor
        if (editor != null) {
            editor(action.parameters, setParams)
        } else {
            type?.let { ActionEditorParams(it, action, problems, enabled, text, setParams) }
        }
        problems.filterIsInstance<MonarchActionProblem.Invalid>().firstOrNull()?.let {
            MonarchInlineNotice(message = it.message, tone = MonarchTone.Danger)
        }
    }
}

@Composable
private fun MoveButton(
    label: String,
    name: String,
    enabled: Boolean,
    icon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    if (icon != null) {
        MonarchIconButton(
            label = name,
            onClick = onClick,
            enabled = enabled,
            icon = icon,
        )
        return
    }
    MonarchButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = name },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
    )
}

/** The inputs of the visible parameters of [type], each with its problem as error text. */
@Composable
@Suppress("LongParameterList")
private fun ActionEditorParams(
    type: MonarchActionType,
    action: MonarchRuleAction,
    problems: List<MonarchActionProblem>,
    enabled: Boolean,
    text: MonarchActionEditorText,
    setParams: (Map<String, MonarchActionValue>) -> Unit,
) {
    type.parameters.filter { it.isVisible(action.parameters) }.forEach { param ->
        MonarchActionEditorParamInput(
            param = param,
            value = action.parameters[param.key],
            errorText = paramError(param, problems, text),
            enabled = enabled,
            choose = text.choose,
            onValueChange = { setParams(CardTokens.withParam(action.parameters, param.key, it)) },
        )
    }
}

/** The error text of [param]: missing or out of range, or `null`. */
private fun paramError(
    param: MonarchActionParameter,
    problems: List<MonarchActionProblem>,
    text: MonarchActionEditorText,
): String? = when {
    MonarchActionProblem.Missing(param.key) in problems -> text.required(param.label)
    MonarchActionProblem.OutOfRange(param.key) in problems -> text.outOfRange(param.label)
    else -> null
}

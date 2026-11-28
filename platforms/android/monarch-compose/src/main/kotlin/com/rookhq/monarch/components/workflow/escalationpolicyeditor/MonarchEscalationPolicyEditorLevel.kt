package com.rookhq.monarch.components.workflow.escalationpolicyeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInput
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInputUnit
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.components.forms.entitypicker.MonarchEntityPickerEntity
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchRemoveButton
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

private typealias LevelTokens = MonarchEscalationPolicyEditorTokens

/** One level of a MonarchEscalationPolicyEditor: heading, summary, remove button, time and targets. */
@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun MonarchEscalationPolicyEditorLevel(
    level: MonarchEscalationLevel,
    position: Int,
    targets: List<MonarchEntityPickerEntity>,
    problem: MonarchEscalationLevelProblem?,
    removable: Boolean,
    enabled: Boolean,
    text: MonarchEscalationPolicyEditorText,
    removeIcon: (@Composable () -> Unit)?,
    onChange: (MonarchEscalationLevel) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MonarchTheme.colors
    val names = level.targets.map { id -> targets.firstOrNull { it.id == id }?.name ?: id }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(LevelTokens.cardColors(colors, invalid = problem != null), MonarchShapeDefaults.Container)
            .padding(LevelTokens.CardPadding)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(LevelTokens.ControlGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MonarchText(text = text.level(position), style = MonarchTextStyle.BodyStrong, heading = true)
                MonarchText(
                    text = text.summary(
                        LevelTokens.duration(level.afterMinutes),
                        names.ifEmpty { listOf(text.nobody) }.joinToString(", "),
                    ),
                    style = MonarchTextStyle.Caption,
                    color = LevelTokens.noteColor(colors),
                )
            }
            MonarchRemoveButton(
                name = text.removeLevel(position),
                text = text.remove,
                onClick = onRemove,
                enabled = enabled && removable,
                icon = removeIcon,
            )
        }
        MonarchDurationInput(
            minutes = level.afterMinutes,
            onMinutesChange = { onChange(level.copy(afterMinutes = it ?: 1)) },
            modifier = Modifier.fillMaxWidth(),
            label = text.after,
            errorText = text.problem(MonarchEscalationLevelProblem.Order)
                .takeIf { problem == MonarchEscalationLevelProblem.Order },
            minimum = 1,
            step = LevelTokens.TIME_STEP,
            largestUnit = MonarchDurationInputUnit.Days,
            enabled = enabled,
            showSteppers = false,
        )
        MonarchText(text = text.notify, style = MonarchTextStyle.Label)
        val targetsError = text.problem(MonarchEscalationLevelProblem.Targets)
            .takeIf { problem == MonarchEscalationLevelProblem.Targets }
        targets.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LevelTokens.ControlGap),
            ) {
                pair.forEach { target ->
                    val on = target.id in level.targets
                    MonarchCheckbox(
                        checked = on,
                        onCheckedChange = { checked ->
                            val chosen = if (checked) level.targets + target.id else level.targets - target.id
                            onChange(level.copy(targets = LevelTokens.ordered(chosen, targets.map { it.id })))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = enabled && target.enabled,
                        invalid = targetsError != null,
                        label = target.name,
                        description = target.description,
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        targetsError?.let {
            MonarchText(text = it, style = MonarchTextStyle.Caption, color = colors.dangerStrong)
        }
    }
}

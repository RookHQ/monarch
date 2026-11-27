package com.rookhq.monarch.components.workflow.escalationpolicyeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.entitypicker.MonarchEntityPickerEntity
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

private typealias Tokens = MonarchEscalationPolicyEditorTokens

private val RailX = MonarchTokens.Space.space3.dp
private val RailTop = MonarchTokens.Space.space4.dp

/**
 * Edits an escalation ladder: at each level, after how long and whom to notify, so an alert or request nobody
 * acts on reaches someone else. The app supplies the targets and runs the escalation.
 *
 * Built by Monarch as a `Column` of outlined levels. Each level has its heading ("Level 1") with a summary
 * ("After 15m, notify On-call rota") and a remove button, a `MonarchDurationInput` for the time and a
 * `MonarchEntityPicker` that picks several targets. With [showErrors], a level that notifies nobody, or (with
 * cumulative timing) is not later than the level before, shows the problem as the error of the field concerned
 * and takes the danger outline. A warning `MonarchInlineNotice` says when there are no targets at all.
 *
 * The editor is not scrollable: put it in a scrolling screen.
 *
 * @param value the levels, in order. Ids must be unique.
 * @param onValueChange called with the whole new list after every change.
 * @param targets what a level can notify: channels, rotas or people.
 * @param modifier the modifier of the editor.
 * @param timing whether each time is measured from the start or from the previous level.
 * @param description help under the levels, such as "Each level fires only if nobody has acknowledged yet."
 * @param defaultStepMinutes the minutes a new level adds to the last one.
 * @param minLevels the fewest levels; the remove buttons are disabled at this count.
 * @param maxLevels the most levels; the add button is replaced by a note at this count.
 * @param showErrors shows the problems of each level, for example after a save attempt.
 * @param enabled whether the policy can be changed.
 * @param createId makes the id of a new level. By default the first free `level-<n>`.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the button reads
 * "Remove".
 * @param addIcon an icon before the text of the add button.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchEscalationPolicyEditor(
    value: List<MonarchEscalationLevel>,
    onValueChange: (List<MonarchEscalationLevel>) -> Unit,
    targets: List<MonarchEntityPickerEntity>,
    modifier: Modifier = Modifier,
    timing: MonarchEscalationTiming = MonarchEscalationTiming.Cumulative,
    description: String? = null,
    defaultStepMinutes: Int = Tokens.DEFAULT_STEP_MINUTES,
    minLevels: Int = Tokens.DEFAULT_MIN,
    maxLevels: Int = Tokens.DEFAULT_MAX,
    showErrors: Boolean = false,
    enabled: Boolean = true,
    createId: (() -> String)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Plus,
    labels: MonarchEscalationPolicyEditorLabels = MonarchEscalationPolicyEditorLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchEscalationPolicyEditorText(labels, resources) }
    val colors = MonarchTheme.colors
    val problems = if (showErrors) MonarchEscalationLevel.validate(value, timing) else emptyMap()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap)) {
        if (targets.isEmpty()) MonarchInlineNotice(message = text.noTargets, tone = MonarchTone.Warning)
        val rail = colors.borderSubtle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (value.size > 1) {
                        val x = RailX.toPx()
                        drawLine(
                            rail,
                            Offset(x, RailTop.toPx()),
                            Offset(x, size.height - RailTop.toPx()),
                            MonarchTokens.Space.space0_5.dp.toPx(),
                        )
                    }
                }
                .semantics { contentDescription = text.label },
            verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap),
        ) {
            value.forEachIndexed { index, level ->
                key(level.id) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(Tokens.SectionGap / 2),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = RailTop)
                                .size(RailX * 2)
                                .background(colors.bgMuted, CircleShape)
                                .clearAndSetSemantics { },
                            contentAlignment = Alignment.Center,
                        ) {
                            MonarchText(
                                text = (index + 1).toString(),
                                style = MonarchTextStyle.Caption,
                                color = colors.textMuted,
                            )
                        }
                        Box(Modifier.weight(1f)) {
                    MonarchEscalationPolicyEditorLevel(
                        level = level,
                        position = index + 1,
                        targets = targets,
                        problem = problems[level.id],
                        removable = value.size > minLevels,
                        enabled = enabled,
                        text = text,
                        removeIcon = removeIcon,
                        onChange = { next -> onValueChange(value.map { if (it.id == level.id) next else it }) },
                        onRemove = { onValueChange(value.filterNot { it.id == level.id }) },
                    )
                        }
                    }
                }
            }
        }
        description?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = Tokens.noteColor(colors)) }
        if (value.size < maxLevels) {
            MonarchButton(
                text = text.add,
                onClick = {
                    val id = createId?.invoke() ?: Tokens.freshId(value.map { it.id })
                    val minutes = Tokens.nextMinutes(value, timing, defaultStepMinutes)
                    onValueChange(value + MonarchEscalationLevel(id, minutes))
                },
                size = MonarchButtonSize.Sm,
                enabled = enabled,
                leadingIcon = addIcon,
            )
        } else {
            MonarchText(text = text.maxReached(maxLevels), color = Tokens.noteColor(colors))
        }
    }
}

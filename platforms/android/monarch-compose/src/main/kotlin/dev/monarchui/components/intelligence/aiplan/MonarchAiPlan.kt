package dev.monarchui.components.intelligence.aiplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.intelligence.aichainofsteps.MonarchAiChainOfSteps
import dev.monarchui.components.intelligence.aichainofsteps.MonarchAiChainOfStepsLabels
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchAnnouncerNode
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchAnnouncer
import dev.monarchui.theme.MonarchTheme

/**
 * The plan an agent proposes before it acts, for a person to review: they
 * edit, reorder, add and remove steps, then approve or reject it. Once
 * approved it shows the steps as the agent works through them.
 *
 * Built in a `MonarchCard` from `MonarchReorderableList` (drag a step after a
 * long press, or use its handle's Move up and Move down), a
 * compact bordered text field per step with Remove, Move up and
 * Move down buttons, and `MonarchButton`s. After a decision, or
 * when [editable] is off, the steps show as a `MonarchAiChainOfSteps` whose
 * states follow the part. A new part from the model, not an echo of the
 * draft, replaces the draft. The list of steps scrolls inside the card once it
 * is taller than four rows of the spacing scale, so the plan can sit inside a
 * scrolling conversation. Removals and the decision are announced through a
 * polite live region; moves are announced by the reorderable list.
 *
 * @param part the plan part the agent proposed. Its steps seed the draft.
 * @param modifier the modifier of the card.
 * @param decision the decision, when the app keeps it, for example in the conversation state. `null` keeps the
 * view's own decision after Approve or Reject.
 * @param editable whether the person can change the steps before deciding. Once decided the plan is read-only.
 * @param removeIcon the icon of each step's remove button, for example a close `MonarchIcon` with a `null`
 * content description. Without it the button reads "Remove". Monarch bundles no icons.
 * @param labels replacements for the built-in strings, including the title.
 * @param onChange called with the draft after every edit, move, addition and removal.
 * @param onReject called when the person rejects the plan.
 * @param onApprove called with the approved steps: labels trimmed, empty steps left out.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiPlan(
    part: MonarchAiPlanPart,
    modifier: Modifier = Modifier,
    decision: MonarchAiPlanDecision? = null,
    editable: Boolean = true,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchAiPlanLabels = MonarchAiPlanLabels(),
    onChange: ((List<MonarchAiPlanStep>) -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onApprove: ((List<MonarchAiPlanStep>) -> Unit)? = null,
) {
    val tokens = MonarchAiPlanTokens
    val colors = MonarchTheme.colors
    val change by rememberUpdatedState(onChange)
    var draft by remember { mutableStateOf(part.steps) }
    var emitted by remember { mutableStateOf(part.steps) }
    var ownDecision by remember { mutableStateOf(MonarchAiPlanDecision.Pending) }
    var counter by remember { mutableIntStateOf(0) }
    var newStepId by remember { mutableStateOf<String?>(null) }
    val announcer = rememberMonarchAnnouncer()
    LaunchedEffect(part.steps) {
        // A new plan from the model, not an echo of the draft, replaces it.
        if (part.steps != emitted) {
            emitted = part.steps
            draft = part.steps
        }
    }
    val update: (List<MonarchAiPlanStep>) -> Unit = { steps ->
        draft = steps
        emitted = steps
        change?.invoke(steps)
    }
    val current = decision ?: ownDecision
    val readOnly = tokens.readOnly(current, editable)
    val title = monarchString(labels.title, R.string.monarch_aiplan_title)
    val approvedNotice = monarchString(labels.approvedNotice, R.string.monarch_aiplan_approved_notice)
    val rejectedNotice = monarchString(labels.rejectedNotice, R.string.monarch_aiplan_rejected_notice)
    LaunchedEffect(current) {
        when (current) {
            MonarchAiPlanDecision.Approved -> announcer.announce(approvedNotice)
            MonarchAiPlanDecision.Rejected -> announcer.announce(rejectedNotice)
            MonarchAiPlanDecision.Pending -> Unit
        }
    }
    MonarchCard(modifier = modifier.semantics { isTraversalGroup = true }, tone = MonarchTone.Ai) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap)) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MonarchTheme.typography.subheading,
                    color = tokens.title(colors),
                )
                Text(
                    text = description(current, readOnly, approvedNotice, rejectedNotice, labels),
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(colors),
                )
            }
            if (readOnly) {
                MonarchAiChainOfSteps(
                    steps = tokens.shownSteps(draft, current),
                    collapsible = false,
                    announces = current == MonarchAiPlanDecision.Approved,
                    labels = MonarchAiChainOfStepsLabels(label = title),
                )
            } else {
                val untitled = stringResource(R.string.monarch_aiplan_untitled_step)
                StepEditor(
                    steps = draft,
                    newStepId = newStepId,
                    removeIcon = removeIcon,
                    labels = labels,
                    stepName = { index, step ->
                        tokens.name(step, labels.untitledStep?.invoke(index + 1) ?: untitled.format(index + 1))
                    },
                    onUpdate = update,
                    onRemoved = announcer::announce,
                    onAdd = {
                        counter += 1
                        val id = tokens.newId(counter, draft)
                        newStepId = id
                        update(draft + MonarchAiPlanStep(id = id, label = ""))
                    },
                )
            }
            if (tokens.showsDecision(current, decision != null, onApprove != null || onReject != null)) {
                DecisionFooter(
                    approvable = tokens.cleaned(draft),
                    labels = labels,
                    onReject = {
                        if (decision == null) ownDecision = MonarchAiPlanDecision.Rejected
                        onReject?.invoke()
                    },
                    onApprove = { steps ->
                        if (decision == null) ownDecision = MonarchAiPlanDecision.Approved
                        onApprove?.invoke(steps)
                    },
                )
            }
        }
        // Always present, so a removal or a decision is announced once as a change of this live region.
        MonarchAnnouncerNode(announcer)
    }
}

@Composable
private fun description(
    decision: MonarchAiPlanDecision,
    readOnly: Boolean,
    approvedNotice: String,
    rejectedNotice: String,
    labels: MonarchAiPlanLabels,
): String = when (decision) {
    MonarchAiPlanDecision.Approved -> approvedNotice
    MonarchAiPlanDecision.Rejected -> rejectedNotice
    MonarchAiPlanDecision.Pending -> {
        val base = monarchString(labels.description, R.string.monarch_aiplan_description)
        if (readOnly) {
            base
        } else {
            val hint = monarchString(labels.editHint, R.string.monarch_aiplan_edit_hint)
            stringResource(R.string.monarch_aiplan_description_with_hint, base, hint)
        }
    }
}

@Composable
private fun StepEditor(
    steps: List<MonarchAiPlanStep>,
    newStepId: String?,
    removeIcon: (@Composable () -> Unit)?,
    labels: MonarchAiPlanLabels,
    stepName: (Int, MonarchAiPlanStep) -> String,
    onUpdate: (List<MonarchAiPlanStep>) -> Unit,
    onRemoved: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val tokens = MonarchAiPlanTokens
    val latest by rememberUpdatedState(steps)
    val removedTemplate = stringResource(R.string.monarch_aiplan_removed)
    val removeTemplate = stringResource(R.string.monarch_aiplan_remove)
    val stepTemplate = stringResource(R.string.monarch_aiplan_step_label)
    val moveUpTemplate = stringResource(R.string.monarch_reorderablelist_move_up)
    val moveDownTemplate = stringResource(R.string.monarch_reorderablelist_move_down)
    MonarchReorderableList(
        items = steps,
        key = { it.id },
        itemLabel = { step -> stepName(latest.indexOf(step), step) },
        onReorder = { from, to -> onUpdate(tokens.moved(latest, from, to)) },
        modifier = Modifier.fillMaxWidth().heightIn(max = tokens.StepsMaxHeight),
        labels = labels.reorder,
    ) { step, state ->
        val name = stepName(state.index, step)
        // As the web: number, field, Remove, Move up and Move down in one line; a narrow row puts the
        // controls under the field.
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            // The number stays with its field when the controls wrap.
            Row(
                modifier = Modifier.weight(1f).widthIn(min = tokens.FieldMinWidth),
                horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${state.index + 1}.",
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTheme.typography.label,
                    color = tokens.muted(MonarchTheme.colors),
                )
                PlanStepField(
                    value = step.label,
                    onValueChange = { label -> onUpdate(tokens.relabelled(latest, step.id, label)) },
                    label = labels.stepLabel?.invoke(state.index + 1) ?: stepTemplate.format(state.index + 1),
                    placeholder = monarchString(labels.stepPlaceholder, R.string.monarch_aiplan_step_placeholder),
                    focusOnStart = step.id == newStepId,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.weight(tokens.PUSH_WEIGHT))
            val removeName = labels.remove?.invoke(name) ?: removeTemplate.format(name)
            val remove = {
                onUpdate(latest.filterNot { it.id == step.id })
                onRemoved(labels.removed?.invoke(name) ?: removedTemplate.format(name))
            }
            if (removeIcon != null) {
                MonarchIconButton(label = removeName, onClick = remove) { removeIcon() }
            } else {
                MonarchButton(
                    text = monarchString(labels.removeText, R.string.monarch_aiplan_remove_text),
                    onClick = remove,
                    modifier = Modifier.semantics { contentDescription = removeName },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
            PlanMoveButton(
                glyph = MonarchDefaultIcons.ChevronUp,
                name = labels.reorder.moveUp?.invoke(name) ?: moveUpTemplate.format(name),
                enabled = state.enabled && state.index > 0,
            ) { onUpdate(tokens.moved(latest, state.index, state.index - 1)) }
            PlanMoveButton(
                glyph = MonarchDefaultIcons.ChevronDown,
                name = labels.reorder.moveDown?.invoke(name) ?: moveDownTemplate.format(name),
                enabled = state.enabled && state.index < latest.lastIndex,
            ) { onUpdate(tokens.moved(latest, state.index, state.index + 1)) }
        }
    }
    MonarchButton(
        text = monarchString(labels.addStep, R.string.monarch_aiplan_add_step),
        leadingIcon = MonarchDefaultIcons.Plus,
        onClick = onAdd,
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
    )
}

@Composable
private fun DecisionFooter(
    approvable: List<MonarchAiPlanStep>,
    labels: MonarchAiPlanLabels,
    onReject: () -> Unit,
    onApprove: (List<MonarchAiPlanStep>) -> Unit,
) {
    val tokens = MonarchAiPlanTokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.RowGap)) {
        if (approvable.isEmpty()) {
            Text(
                text = monarchString(labels.emptyPlan, R.string.monarch_aiplan_empty_plan),
                style = MonarchTheme.typography.caption,
                color = tokens.muted(MonarchTheme.colors),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(tokens.RowGap),
        ) {
            MonarchButton(
                text = monarchString(labels.reject, R.string.monarch_aiplan_reject),
                onClick = onReject,
                size = MonarchButtonSize.Sm,
            )
            MonarchButton(
                text = monarchString(labels.approve, R.string.monarch_aiplan_approve),
                onClick = { onApprove(approvable) },
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                enabled = approvable.isNotEmpty(),
            )
        }
    }
}

/** The step's text as web draws it: an always-open compact bordered field. */
@Composable
private fun PlanStepField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    focusOnStart: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MonarchTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusOnStart) { if (focusOnStart) focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .monarchMinimumTouchTarget()
            .focusRequester(focus)
            .semantics { contentDescription = label },
        textStyle = MonarchTheme.typography.caption.copy(color = colors.textDefault),
        cursorBrush = SolidColor(colors.textDefault),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MonarchAiPlanTokens.FieldMinHeight)
                    .border(BorderStroke(MonarchAiPlanTokens.FieldBorder, colors.borderInput), MonarchTheme.shapes.md)
                    .padding(
                        horizontal = MonarchAiPlanTokens.FieldPaddingHorizontal,
                        vertical = MonarchAiPlanTokens.FieldPaddingVertical,
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = MonarchTheme.typography.caption, color = colors.textSubtle)
                }
                inner()
            }
        },
    )
}

/** A compact chevron button for moving a step, as small as the web's, with the touch area Compose adds around it. */
@Composable
private fun PlanMoveButton(glyph: @Composable () -> Unit, name: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .size(MonarchAiPlanTokens.MoveSize)
            .clip(MonarchTheme.shapes.md)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) colors.textDefault else colors.textDisabled) {
            glyph()
        }
    }
}

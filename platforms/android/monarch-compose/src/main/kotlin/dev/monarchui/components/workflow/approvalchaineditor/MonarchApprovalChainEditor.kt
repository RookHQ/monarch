package dev.monarchui.components.workflow.approvalchaineditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.theme.MonarchTheme

private typealias Tokens = MonarchApprovalChainEditorTokens

/**
 * Edits the ordered steps of an approval chain: who approves each step (a kind of approver, plus a role or
 * person where the kind needs one), an optional label, and the order, with add, remove and accessible
 * reordering. The app owns the chain's other settings and saving.
 *
 * Built by Monarch on `MonarchReorderableList` with its move buttons: a long press drags a step, and the drag
 * handle offers Move up and Move down to TalkBack, Switch Access and the arrow keys, with every move announced.
 * Each step has its heading ("Step 1") and a remove button, the approver kind as a `MonarchSelect`, a
 * `MonarchEntityPicker` for the target when the kind has targets, and a `MonarchTextField` for the label.
 *
 * The list is lazy: give the editor a bounded height (a screen or a sheet) and do not put it in a vertically
 * scrolling parent. It shows as many steps as fit and scrolls the rest.
 *
 * @param value the steps, in order. Ids must be unique.
 * @param onValueChange called with the whole new list after every edit, move, add or remove.
 * @param approverTypes the kinds of approver a step can name. The first is used for new steps.
 * @param modifier the modifier of the editor.
 * @param description help under the heading, such as how skipped steps work.
 * @param minSteps the fewest steps; the remove buttons are disabled at this count.
 * @param maxSteps the most steps; the add button is replaced by a note at this count.
 * @param showLabels shows the optional label field on each step.
 * @param showErrors shows "choose who decides" on steps without a target, for example after a save attempt.
 * @param enabled whether the chain can be changed.
 * @param createStep makes a new step. By default the first approver kind, with the first free `step-<n>` id.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the button reads
 * "Remove".
 * @param addIcon an icon before the text of the add button.
 * @param state the scroll state of the list of steps.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchApprovalChainEditor(
    value: List<MonarchApprovalChainStep>,
    onValueChange: (List<MonarchApprovalChainStep>) -> Unit,
    approverTypes: List<MonarchApprovalApproverType>,
    modifier: Modifier = Modifier,
    description: String? = null,
    minSteps: Int = Tokens.DEFAULT_MIN,
    maxSteps: Int = Tokens.DEFAULT_MAX,
    showLabels: Boolean = true,
    showErrors: Boolean = false,
    enabled: Boolean = true,
    createStep: (() -> MonarchApprovalChainStep)? = null,
    removeIcon: (@Composable () -> Unit)? = null,
    addIcon: (@Composable () -> Unit)? = null,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchApprovalChainEditorLabels = MonarchApprovalChainEditorLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchApprovalChainEditorText(labels, resources) }
    val colors = MonarchTheme.colors
    val missing = if (showErrors) MonarchApprovalChainStep.validate(value, approverTypes) else emptySet()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap)) {
        MonarchText(text = text.title, style = MonarchTextStyle.Subheading, heading = true)
        description?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = Tokens.noteColor(colors)) }
        MonarchReorderableList(
            items = value,
            key = { it.id },
            itemLabel = { step ->
                val position = value.indexOfFirst { it.id == step.id } + 1
                "${text.step(position)}, ${MonarchApprovalChainStep.summary(step, approverTypes)}"
            },
            onReorder = { from, to -> onValueChange(Tokens.moved(value, from, to)) },
            modifier = Modifier.weight(1f, fill = false),
            enabled = enabled,
            moveButtons = true,
            state = state,
            labels = labels.reorder,
        ) { step, itemState ->
            MonarchApprovalChainEditorStep(
                step = step,
                position = itemState.index + 1,
                approverTypes = approverTypes,
                removable = value.size > minSteps,
                showLabels = showLabels,
                missingTarget = step.id in missing,
                enabled = enabled,
                text = text,
                removeIcon = removeIcon,
                onChange = { next ->
                    onValueChange(value.map { if (it.id == step.id) next.cleaned(approverTypes) else it })
                },
                onRemove = { onValueChange(value.filterNot { it.id == step.id }) },
            )
        }
        if (value.size < maxSteps) {
            MonarchButton(
                text = text.addStep,
                onClick = {
                    val step = createStep?.invoke() ?: MonarchApprovalChainStep(
                        id = Tokens.freshId(value.map { it.id }),
                        approver = approverTypes.firstOrNull()?.value.orEmpty(),
                    )
                    onValueChange(value + step)
                },
                size = MonarchButtonSize.Sm,
                enabled = enabled && approverTypes.isNotEmpty(),
                leadingIcon = addIcon,
            )
        } else {
            MonarchText(text = text.maxReached(maxSteps), color = Tokens.noteColor(colors))
        }
    }
}

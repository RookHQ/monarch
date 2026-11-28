package dev.monarchui.components.workflow.actioneditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.theme.MonarchTheme

private typealias Tokens = MonarchActionEditorTokens

/**
 * Edits the ordered actions of a rule. Each action has a type and the parameters that type defines (team,
 * priority, tags, a message…), shown as labelled inputs, with validation and reordering. Types are data, so the
 * same editor serves routing rules, automations and macros.
 *
 * Built by Monarch as a `Column` of outlined action cards: a heading ("Action 1") with Move up, Move down and
 * Remove buttons, the type as a `MonarchSelect` with the type's description as helper text, a warning
 * `MonarchInlineNotice` when the type is disabled, then one input per visible parameter: `MonarchTextField`
 * (text, several lines, a number), `MonarchSelect` (one option, with an empty choice when optional),
 * `MonarchMultiSelect` (several options, or free tags) or `MonarchCheckboxRow` (yes or no). A type can bring its
 * own editor instead. Picking another type starts the action again with that type's defaults.
 *
 * The list is controlled: every change calls [onValueChange] with the whole new list. Reordering uses buttons,
 * not drag, so it works the same with TalkBack and Switch Access, and each move is announced through a polite
 * live region ("Set priority moved to position 1 of 3."). With [showErrors], empty required parameters and
 * numbers out of range show their error on the input, and the type's own check shows under the inputs.
 *
 * @param value the actions, in the order they run. Ids must be unique.
 * @param onValueChange called with the whole new list after every change.
 * @param actionTypes the kinds of action available.
 * @param modifier the modifier of the editor.
 * @param ordered whether actions run in order and can be moved. Without it there are no move buttons.
 * @param maxActions the most actions; the add button is replaced by a note at this count.
 * @param showErrors shows what is missing on each incomplete action, for example after a save attempt.
 * @param defaultType the key of the type new actions start as. The first enabled type by default.
 * @param enabled whether the actions can be changed. A type's own editor is shown but its changes are dropped.
 * @param createId makes the id of a new action. By default the first free `action-<n>`.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the button reads
 * "Remove".
 * @param moveUpIcon an icon for the move up button, usually an up chevron. Without it the button reads its text.
 * @param moveDownIcon an icon for the move down button, usually a down chevron.
 * @param addIcon an icon before the text of the add button.
 * @param emptyContent replaces the note shown when there are no actions.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchActionEditor(
    value: List<MonarchRuleAction>,
    onValueChange: (List<MonarchRuleAction>) -> Unit,
    actionTypes: List<MonarchActionType>,
    modifier: Modifier = Modifier,
    ordered: Boolean = true,
    maxActions: Int = Tokens.DEFAULT_MAX,
    showErrors: Boolean = false,
    defaultType: String? = null,
    enabled: Boolean = true,
    createId: (() -> String)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    moveUpIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronUp,
    moveDownIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Plus,
    emptyContent: (@Composable () -> Unit)? = null,
    labels: MonarchActionEditorLabels = MonarchActionEditorLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchActionEditorText(labels, resources) }
    val colors = MonarchTheme.colors
    var announcement by remember { mutableStateOf("") }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap)) {
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
                value.forEachIndexed { index, action ->
                    key(action.id) {
                        val move: ((Int) -> Unit)? = if (ordered) {
                            { to ->
                                onValueChange(Tokens.moved(value, index, to))
                                val name = actionTypes.firstOrNull { it.key == action.type }?.label ?: action.type
                                announcement = text.moved(name, to + 1, value.size)
                            }
                        } else {
                            null
                        }
                        MonarchActionEditorCard(
                            action = action,
                            position = index + 1,
                            count = value.size,
                            actionTypes = actionTypes,
                            showErrors = showErrors,
                            enabled = enabled,
                            text = text,
                            removeIcon = removeIcon,
                            moveUpIcon = moveUpIcon,
                            moveDownIcon = moveDownIcon,
                            onChange = { next ->
                                if (enabled) onValueChange(value.map { if (it.id == action.id) next else it })
                            },
                            onRemove = { onValueChange(value.filterNot { it.id == action.id }) },
                            onMove = move,
                        )
                    }
                }
            }
        }
        if (value.size < maxActions) {
            MonarchButton(
                text = text.add,
                onClick = {
                    val id = createId?.invoke() ?: Tokens.freshId(value.map { it.id })
                    onValueChange(value + MonarchRuleAction.create(id, Tokens.startType(actionTypes, defaultType)))
                },
                size = MonarchButtonSize.Sm,
                enabled = enabled && actionTypes.any { it.enabled },
                leadingIcon = addIcon,
            )
        } else {
            MonarchText(text = text.maxReached(maxActions), color = Tokens.noteColor(colors))
        }
        // Always present, so each move is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

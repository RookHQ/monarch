package dev.monarchui.components.intelligence.aiinlineprompt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import dev.monarchui.R
import dev.monarchui.internal.monarchString

/** The key of the "Ask AI" item in the text context menu. */
private object AiInlinePromptMenuKey

/**
 * Adds "Ask AI" to the text selection menu of the text in [content]: the
 * floating toolbar that appears over a touch selection and the menu of a
 * right click. Wrap a `SelectionContainer` or a text field in it. Picking the
 * item closes the menu and calls [onAskAi]; show a `MonarchAiInlinePrompt`
 * from there.
 *
 * Built on Foundation's `Modifier.appendTextContextMenuComponents`, so the
 * item comes after the system's Cut, Copy, Paste and Select all, and the
 * toolbar may move it to its overflow menu when space is short. Compose does
 * not report the selected text of a `SelectionContainer`, so pass the text
 * the prompt acts on yourself; for a text field use the overload that takes
 * its `TextFieldState`.
 *
 * @param onAskAi called when the person picks the item.
 * @param modifier the modifier of the area, a `Box` around [content].
 * @param enabled off, the item is not added.
 * @param label the item's text. The built-in "Ask AI" without it.
 * @param content the selectable text or the text field.
 */
@Composable
public fun MonarchAiInlinePromptArea(
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val text = monarchString(label, R.string.monarch_aiinlineprompt_label)
    val action by rememberUpdatedState(onAskAi)
    Box(
        modifier = if (enabled) {
            modifier.appendTextContextMenuComponents {
                separator()
                item(key = AiInlinePromptMenuKey, label = text) {
                    close()
                    action()
                }
            }
        } else {
            modifier
        },
    ) { content() }
}

/**
 * Adds "Ask AI" to the selection menu of a text field while text is
 * selected in [state], and gives [onAskAi] the selected text. Wrap the text
 * field (`BasicTextField`, `TextField` or `MonarchTextField` with a
 * `TextFieldState`) in it. Nothing is added while only a caret is placed, so
 * the caret menu (Paste) stays as it is.
 *
 * @param state the text field's state, read when the menu opens and when the item is picked.
 * @param onAskAi receives the selected text when the person picks the item.
 * @param modifier the modifier of the area, a `Box` around [content].
 * @param enabled off, the item is not added.
 * @param label the item's text. The built-in "Ask AI" without it.
 * @param content the text field.
 */
@Composable
public fun MonarchAiInlinePromptArea(
    state: TextFieldState,
    onAskAi: (selection: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val text = monarchString(label, R.string.monarch_aiinlineprompt_label)
    val action by rememberUpdatedState(onAskAi)
    Box(
        modifier = if (enabled) {
            modifier.appendTextContextMenuComponents {
                if (!state.selection.collapsed) {
                    separator()
                    item(key = AiInlinePromptMenuKey, label = text) {
                        close()
                        val selection = state.selection
                        action(state.text.substring(selection.min, selection.max))
                    }
                }
            }
        } else {
            modifier
        },
    ) { content() }
}

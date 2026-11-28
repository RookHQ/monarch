package dev.monarchui.components.intelligence.aicodeblock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.typography.codeblock.MonarchCodeBlock
import dev.monarchui.components.typography.codeblock.MonarchCodeBlockHighlighter
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Code written by an assistant: a `MonarchCodeBlock` with copy, wrap, diffs
 * and line numbers, plus optional Apply and Insert buttons that hand the code
 * back to the app.
 *
 * Built on `MonarchCodeBlock` and `MonarchButton`. `MonarchCodeBlock` has no
 * slot in its header, so the actions sit in a row under the code, aligned to
 * the end, and wrap onto more lines when they do not fit. While the code is
 * still arriving ([incomplete]) Apply and Insert are disabled and a line under
 * them says why; Copy works on what has arrived. Without [onApply], [onInsert]
 * and [actions] it is a plain `MonarchCodeBlock`.
 *
 * @param code the code, exactly as it should be copied, applied and inserted.
 * @param modifier the modifier of the block.
 * @param language a language id that picks the highlighter and the header text; "diff" or "patch" turns on the
 * diff mode.
 * @param title the header text, such as a file name. It also names the code for TalkBack and the action buttons.
 * @param diff forces the diff mode on or off.
 * @param highlighted colours the syntax.
 * @param showLineNumbers shows line numbers, which TalkBack does not read.
 * @param showWrapToggle shows the wrap toggle in the header.
 * @param copyable shows the copy button in the header.
 * @param incomplete the code is still arriving: Apply and Insert are disabled with a hint saying why.
 * @param wrapped whether long lines wrap, when the app controls it. `null` keeps the state in the block,
 * starting unwrapped.
 * @param onWrappedChange called with the new wrap state when the toggle is pressed.
 * @param onCopy called with the copied code after each copy.
 * @param onInsert adds a secondary Insert button that hands over the code.
 * @param onApply adds a primary Apply button that hands over the code.
 * @param labels replacements for the built-in strings, including those of the code block.
 * @param actions the app's own controls, placed in the action row before Insert and Apply.
 */
@Composable
public fun MonarchAiCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    language: String? = null,
    title: String? = null,
    diff: Boolean? = null,
    highlighted: Boolean = true,
    showLineNumbers: Boolean = false,
    showWrapToggle: Boolean = true,
    copyable: Boolean = true,
    incomplete: Boolean = false,
    wrapped: Boolean? = null,
    onWrappedChange: ((Boolean) -> Unit)? = null,
    onCopy: ((String) -> Unit)? = null,
    onInsert: ((String) -> Unit)? = null,
    onApply: ((String) -> Unit)? = null,
    labels: MonarchAiCodeBlockLabels = MonarchAiCodeBlockLabels(),
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchAiCodeBlockTokens
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
        MonarchCodeBlock(
            code = code,
            language = language,
            title = title,
            diff = diff,
            highlighted = highlighted,
            showLineNumbers = showLineNumbers,
            showWrapToggle = showWrapToggle,
            copyable = copyable,
            wrapped = wrapped,
            onWrappedChange = onWrappedChange,
            onCopy = onCopy,
            labels = labels.codeBlock,
        )
        if (tokens.hasActions(onApply != null, onInsert != null, actions != null)) {
            val name = codeName(labels, title, language)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                actions?.invoke(this)
                onInsert?.let { insert ->
                    ActionButton(
                        text = monarchString(labels.insert, R.string.monarch_aicodeblock_insert),
                        name = name,
                        variant = MonarchButtonVariant.Secondary,
                        enabled = tokens.actionsEnabled(incomplete),
                        labels = labels,
                        onClick = { insert(code) },
                    )
                }
                onApply?.let { apply ->
                    ActionButton(
                        text = monarchString(labels.apply, R.string.monarch_aicodeblock_apply),
                        name = name,
                        variant = MonarchButtonVariant.Primary,
                        enabled = tokens.actionsEnabled(incomplete),
                        labels = labels,
                        onClick = { apply(code) },
                    )
                }
            }
            if (tokens.showsHint(incomplete, onApply != null, onInsert != null)) {
                Text(
                    text = monarchString(labels.incompleteHint, R.string.monarch_aicodeblock_incomplete_hint),
                    modifier = Modifier.align(Alignment.End),
                    style = MonarchTheme.typography.caption,
                    color = tokens.hintColor(MonarchTheme.colors),
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    name: String,
    variant: MonarchButtonVariant,
    enabled: Boolean,
    labels: MonarchAiCodeBlockLabels,
    onClick: () -> Unit,
) {
    val spoken = labels.actionLabel?.invoke(text, name)
        ?: stringResource(R.string.monarch_aicodeblock_action_label, text, name)
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = spoken },
        variant = variant,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
    )
}

/** The name of the code, as `MonarchCodeBlock` names it: the title, "TypeScript code" or "Code". */
@Composable
private fun codeName(labels: MonarchAiCodeBlockLabels, title: String?, language: String?): String {
    labels.codeBlock.codeName?.let { return it(title, language) }
    if (title != null) return title
    val languageName = MonarchCodeBlockHighlighter.languageName(language)
    return if (languageName != null) {
        stringResource(R.string.monarch_codeblock_language_code, languageName)
    } else {
        stringResource(R.string.monarch_codeblock_code)
    }
}

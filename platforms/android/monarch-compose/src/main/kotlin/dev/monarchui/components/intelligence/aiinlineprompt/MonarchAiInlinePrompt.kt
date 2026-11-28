package dev.monarchui.components.intelligence.aiinlineprompt

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString

/**
 * A prompt about selected text: the person picks "Ask AI" in the text
 * selection menu (see `MonarchAiInlinePromptArea`), and this
 * sheet asks what to do with the selection, such as "make this shorter".
 *
 * The web's floating panel over the selection becomes, on a phone, an action
 * in the system selection toolbar plus a `MonarchBottomSheet`. The
 * sheet shows an excerpt of [context], a `MonarchAiPromptInput` that takes
 * focus when the sheet opens, and optional quick prompts. Send, Enter on a
 * hardware keyboard, or a suggestion calls [onSubmit] with the trimmed
 * prompt; blank prompts are never sent. Then, with [closeOnSubmit], the sheet
 * slides away and calls [onDismissRequest]. Swiping it down, tapping the
 * scrim and Back close it without sending.
 *
 * Show it while a state is true, as with `MonarchBottomSheet`:
 * `if (open) MonarchAiInlinePrompt(onDismissRequest = { open = false }, …)`.
 * Each time it is shown the field starts from [initialValue]. When it closes
 * the system gives focus back to the window underneath. The app applies the
 * result, preferably as a proposal to review (`MonarchAiEditProposal`).
 *
 * @param onDismissRequest called when the sheet should close: after a swipe, a tap on the scrim, Back, or
 * after sending with [closeOnSubmit]. Stop showing it.
 * @param onSubmit receives the trimmed prompt or the tapped suggestion.
 * @param modifier the modifier of the sheet.
 * @param context what the prompt acts on, usually the selected text. Shown as an excerpt of at most 140
 * characters.
 * @param suggestions quick prompts, such as "Shorten" or "Fix spelling". Tapping one sends it at once.
 * @param initialValue the text the field starts with each time the sheet opens, with the caret after it.
 * @param closeOnSubmit closes the sheet after sending. Off, the sheet stays and the field is cleared.
 * @param pending a request is being sent: the field, Send and the suggestions are disabled and "Sending…"
 * is shown and announced.
 * @param labels replacements for the built-in strings.
 * @param sendIcon the Send button's icon, a `MonarchIcon` with a `null` description. The button shows the
 * word Send without it, and is named Send either way.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchAiInlinePrompt(
    onDismissRequest: () -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    context: String? = null,
    suggestions: List<String> = emptyList(),
    initialValue: String = "",
    closeOnSubmit: Boolean = true,
    pending: Boolean = false,
    labels: MonarchAiInlinePromptLabels = MonarchAiInlinePromptLabels(),
    sendIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ArrowUp,
) {
    val focusRequester = remember { FocusRequester() }
    val submit by rememberUpdatedState(onSubmit)
    val title = monarchString(labels.label, R.string.monarch_aiinlineprompt_label)
    MonarchBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        val sheet = this
        AiInlinePromptPanel(
            onSend = { prompt ->
                if (!pending) {
                    submit(prompt)
                    // The sheet slides away, then calls onDismissRequest.
                    if (closeOnSubmit) sheet.dismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { paneTitle = title },
            context = context,
            suggestions = suggestions,
            initialValue = initialValue,
            pending = pending,
            labels = labels,
            sendIcon = sendIcon,
            focusRequester = focusRequester,
        )
    }
}

package dev.monarchui.components.intelligence.aiinlineprompt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInput
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInputLabels
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchAiAccent
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * The content of the `MonarchAiInlinePrompt` sheet: the title, the excerpt
 * of the selection, the prompt field with Send, the quick prompts and the
 * sending status. Kept apart from the sheet so previews and fixtures can
 * show it without a window of its own.
 *
 * @param onSend receives the trimmed prompt or a suggestion. Never called with blank text.
 * @param focusRequester when given, the prompt field takes focus as soon as the panel is shown.
 */
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiInlinePromptPanel(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    context: String? = null,
    suggestions: List<String> = emptyList(),
    initialValue: String = "",
    pending: Boolean = false,
    labels: MonarchAiInlinePromptLabels = MonarchAiInlinePromptLabels(),
    sendIcon: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    val tokens = MonarchAiInlinePromptTokens
    val colors = MonarchTheme.colors
    val state = rememberTextFieldState(initialValue)
    val excerpt = tokens.excerpt(context)
    val pendingLabel = monarchString(labels.pending, R.string.monarch_aiinlineprompt_pending)
    val suggestionsLabel = monarchString(labels.suggestions, R.string.monarch_aiinlineprompt_suggestions)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchText(
            text = monarchString(labels.label, R.string.monarch_aiinlineprompt_label),
            style = MonarchTextStyle.Subheading,
            heading = true,
        )
        if (excerpt != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .monarchSurface(tokens.excerptColors(colors), MonarchShapeDefaults.Container)
                    .padding(tokens.ExcerptPadding)
                    .semantics(mergeDescendants = true) { },
                verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            ) {
                MonarchText(
                    text = monarchString(labels.context, R.string.monarch_aiinlineprompt_context),
                    style = MonarchTextStyle.Label,
                    color = tokens.contextLabelColor(colors),
                )
                MonarchText(text = excerpt, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors))
            }
        }
        MonarchAiAccent {
        MonarchAiPromptInput(
            label = monarchString(labels.inputLabel, R.string.monarch_aiinlineprompt_input_label),
            state = state,
            onSubmit = { text -> tokens.prompt(text)?.let(onSend) },
            enabled = !pending,
            placeholder = monarchString(labels.placeholder, R.string.monarch_aiinlineprompt_placeholder),
            focusRequester = focusRequester,
            labels = MonarchAiPromptInputLabels(
                send = monarchString(labels.submit, R.string.monarch_aiinlineprompt_submit),
            ),
            sendIcon = sendIcon,
            leadingIcon = MonarchDefaultIcons.Bot,
        )
        }
        if (suggestions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        isTraversalGroup = true
                        contentDescription = suggestionsLabel
                    },
                horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
                verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            ) {
                suggestions.forEach { suggestion ->
                    MonarchButton(
                        text = suggestion,
                        onClick = { tokens.prompt(suggestion)?.let(onSend) },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        enabled = !pending,
                    )
                }
            }
        }
        // Always present, so the sending status is announced as a change of this region.
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (pending) contentDescription = pendingLabel
            },
        ) {
            if (pending) {
                MonarchText(
                    text = pendingLabel,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTextStyle.Caption,
                    color = tokens.mutedColor(colors),
                )
            }
        }
    }
    if (focusRequester != null) {
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    }
}

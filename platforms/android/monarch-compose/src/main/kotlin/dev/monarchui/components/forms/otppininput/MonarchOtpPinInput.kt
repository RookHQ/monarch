package dev.monarchui.components.forms.otppininput

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.theme.MonarchTheme

/**
 * A one-time code or PIN drawn as one cell per character, over a single
 * text field.
 *
 * Built on the Compose `BasicTextField` with a `decorationBox` that draws the
 * cells, because Material 3 has no code input. There is one input, so
 * TalkBack reads one field, paste and the keyboard work as in any field, and
 * the `SmsOtpCode` autofill content type lets the system fill a code from a
 * text message. Only accepted characters reach [onValueChange], never more
 * than [length]. The cells keep left-to-right order in right-to-left layouts,
 * because a code is read left to right.
 *
 * @param value the code: digits, or digits and A to Z in upper case.
 * @param onValueChange called with the cleaned code on every change.
 * @param label the visible label, already localised.
 * @param modifier the modifier of the field.
 * @param length the number of characters. Values under 1 count as 1.
 * @param helperText guidance under the cells.
 * @param errorText the error message, for example after a failed verification. It replaces [helperText] and
 * every cell takes the danger border.
 * @param mode which characters are accepted, and the keyboard.
 * @param size the size of the cells.
 * @param enabled whether the user can focus and edit the field.
 * @param masked shows dots instead of the characters, for a PIN typed where others can see the screen.
 * @param groupSize draws a wider gap after every this many cells, for example 3 for "123 456". Visual only.
 * @param lengthHint replaces the built-in spoken state "Code length: 6".
 * @param onComplete called each time the code reaches [length] characters, typed, pasted or filled.
 * @param interactionSource the source of the focus interactions of the field.
 */
@Composable
public fun MonarchOtpPinInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    length: Int = MonarchOtpPinInputTokens.DEFAULT_LENGTH,
    helperText: String? = null,
    errorText: String? = null,
    mode: MonarchOtpPinInputMode = MonarchOtpPinInputMode.Numeric,
    size: MonarchOtpPinInputSize = MonarchOtpPinInputSize.Md,
    enabled: Boolean = true,
    masked: Boolean = false,
    groupSize: Int? = null,
    lengthHint: String? = null,
    onComplete: ((String) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val count = length.coerceAtLeast(1)
    val code = MonarchOtpPinInputTokens.clean(value, mode, count)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val hint = lengthHint ?: stringResource(R.string.monarch_otppininput_length, count)
    val colors = MonarchTheme.colors
    BasicTextField(
        value = TextFieldValue(text = code, selection = TextRange(code.length)),
        onValueChange = { next ->
            val cleaned = MonarchOtpPinInputTokens.clean(next.text, mode, count)
            if (cleaned != code) {
                onValueChange(cleaned)
                if (cleaned.length == count) onComplete?.invoke(cleaned)
            }
        },
        modifier = modifier.semantics {
            contentType = ContentType.SmsOtpCode
            stateDescription = hint
            if (errorText != null) error(errorText)
        },
        enabled = enabled,
        // The real text is drawn by the cells; the field itself is transparent.
        textStyle = TextStyle(color = Color.Transparent),
        keyboardOptions = keyboardOptions(mode, masked),
        keyboardActions = KeyboardActions.Default,
        singleLine = true,
        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
        interactionSource = source,
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = { innerTextField ->
            Column(verticalArrangement = Arrangement.spacedBy(MonarchOtpPinInputTokens.PartGap)) {
                Text(
                    text = label,
                    style = MonarchTheme.typography.label,
                    color = if (enabled) colors.textDefault else colors.textDisabled,
                )
                Box {
                    Cells(
                        code = code,
                        state = CellsState(count, size, groupSize, masked, focused, errorText != null, enabled),
                    )
                    Box(modifier = Modifier.matchParentSize()) { innerTextField() }
                }
                val supporting = errorText ?: helperText
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MonarchTheme.typography.caption,
                        color = when {
                            !enabled -> colors.textDisabled
                            errorText != null -> colors.dangerStrong
                            else -> colors.textMuted
                        },
                    )
                }
            }
        },
    )
}

/** What the cells need to know besides the code. */
private class CellsState(
    val length: Int,
    val size: MonarchOtpPinInputSize,
    val groupSize: Int?,
    val masked: Boolean,
    val focused: Boolean,
    val error: Boolean,
    val enabled: Boolean,
)

@Composable
private fun Cells(code: String, state: CellsState) {
    val tokens = MonarchOtpPinInputTokens
    val cellSize = tokens.cellSize(state.size)
    val active = code.length.coerceAtMost(state.length - 1)
    // A code is read left to right in every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.widthIn(max = tokens.rowWidth(state.length, state.groupSize, state.size)),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            repeat(state.length) { index ->
                Cell(
                    character = code.getOrNull(index),
                    active = state.focused && index == active,
                    state = state,
                    modifier = Modifier.weight(1f).widthIn(max = cellSize).heightIn(min = cellSize),
                )
                if (tokens.groupEndsAfter(index, state.length, state.groupSize)) {
                    Spacer(modifier = Modifier.width(tokens.Gap))
                }
            }
        }
    }
}

@Composable
private fun Cell(character: Char?, active: Boolean, state: CellsState, modifier: Modifier) {
    val tokens = MonarchOtpPinInputTokens
    val colors = tokens.cell(character != null, active, state.error, state.enabled, MonarchTheme.colors)
    val shape = MonarchTheme.shapes.md
    Box(
        modifier = modifier
            .background(colors.container, shape)
            .border(
                width = if (active) tokens.ActiveBorderWidth else tokens.BorderWidth,
                color = colors.border ?: Color.Transparent,
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            character != null && state.masked ->
                Box(modifier = Modifier.size(tokens.DotSize).background(colors.content, MonarchTheme.shapes.full))
            character != null -> Text(
                text = character.toString(),
                style = when (state.size) {
                    MonarchOtpPinInputSize.Md -> MonarchTheme.typography.subheading
                    MonarchOtpPinInputSize.Lg -> MonarchTheme.typography.heading
                },
                color = colors.content,
            )
            active -> Box(
                modifier = Modifier
                    .width(tokens.CaretWidth)
                    .heightIn(min = tokens.CaretHeight)
                    .background(colors.content),
            )
        }
    }
}

private fun keyboardOptions(mode: MonarchOtpPinInputMode, masked: Boolean): KeyboardOptions = when (mode) {
    MonarchOtpPinInputMode.Numeric -> KeyboardOptions(
        keyboardType = if (masked) KeyboardType.NumberPassword else KeyboardType.Number,
        imeAction = ImeAction.Done,
    )
    MonarchOtpPinInputMode.Alphanumeric -> KeyboardOptions(
        capitalization = KeyboardCapitalization.Characters,
        autoCorrectEnabled = false,
        keyboardType = if (masked) KeyboardType.Password else KeyboardType.Ascii,
        imeAction = ImeAction.Done,
    )
}

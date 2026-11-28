package dev.monarchui.components.forms.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import dev.monarchui.components.forms.formfield.MonarchFieldFrame
import dev.monarchui.components.forms.formfield.monarchFieldName
import dev.monarchui.components.forms.formfield.monarchFieldSemantics
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.foundation.MonarchFieldSize
import dev.monarchui.internal.field.MonarchFieldBox
import dev.monarchui.internal.field.MonarchFieldPlaceholder
import dev.monarchui.internal.field.fieldColors
import dev.monarchui.internal.field.monarchFieldTextStyle
import dev.monarchui.internal.field.rememberFieldInteractionSource
import dev.monarchui.internal.field.rememberMonarchFieldState
import dev.monarchui.theme.MonarchTheme

/**
 * A single-line or multi-line text input with a label, helper text and an error message.
 *
 * Built on the Compose `BasicTextField` (caret, selection, IME, autofill and keyboard stay the platform's)
 * with Monarch's own anatomy, the same as the web field: the label sits ABOVE a plain 1dp bordered box (radius
 * `md`, 14sp text, 48dp tall by default), never floats or notches into the border; the helper, success or
 * error line sits below it, the error with an icon. The box fills the available width; pass a
 * `Modifier.width(...)` to narrow it. A multi-line field (`singleLine = false`) is the web textarea: at
 * least [minLines] lines and 72dp tall.
 *
 * @param value the text in the field.
 * @param onValueChange called with the new text when the user edits the field.
 * @param modifier the modifier of the field.
 * @param label the visible label, drawn above the field. Always give one unless a visible label next to the
 * field names it; then
 * give it anyway with [labelVisibility] `Hidden`, so TalkBack can name the field.
 * @param placeholder an example shown while the field is empty. Not a replacement for [label].
 * @param helperText guidance shown below the field.
 * @param errorText the error message. When not `null` the field is in the error state, the message
 * replaces [helperText] and TalkBack announces it.
 * @param enabled whether the user can focus and edit the field.
 * @param readOnly whether the text can be selected and copied but not edited.
 * @param singleLine whether the field is one line that scrolls horizontally.
 * @param leadingIcon an icon at the start of the field.
 * @param trailingIcon an icon at the end of the field.
 * @param visualTransformation how the text is shown, for example `PasswordVisualTransformation()`.
 * @param keyboardOptions the keyboard type and IME action.
 * @param keyboardActions what happens when the user triggers the IME action.
 * @param required marks the field as required: an asterisk after the visible label, and "required" after the
 * name TalkBack reads. Validation stays with the app.
 * @param successText a confirmation below the field ("Username is available"). It puts the border in the
 * success colour and replaces [helperText]. [errorText] wins over it.
 * @param prefix text or content before the input, inside the box, such as "https://" or a currency sign.
 * @param suffix text or content after the input, such as a unit.
 * @param minLines the smallest number of lines the field shows. Use with `singleLine = false` for a multi-line
 * field that starts taller than one line.
 * @param maxLines the largest number of lines before the field scrolls. Ignored when [singleLine].
 * @param textStyle the style of the input text, for example a monospace style for codes. `null` uses the body
 * style.
 * @param labelVisibility whether [label] is drawn. `Hidden` keeps it as the TalkBack name only.
 * @param requiredLabel replaces the TalkBack name of a required field. `null` uses the built-in string
 * `monarch_textfield_required` ("Email, required").
 * @param interactionSource the source of the focus and press interactions of the field.
 * @param invalid draws the error border and cursor without a message, for a field whose error is explained elsewhere.
 * @param labelAction an action at the end of the label row, such as a "Forgot password?" link.
 * @param optionalText text after the label of an optional field, for example "Optional". Ignored when [required].
 * @param size the height step: 40dp `Small`, 48dp `Medium` (default) or 52dp `Large`. The hit area stays 48dp.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    required: Boolean = false,
    successText: String? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle? = null,
    labelVisibility: MonarchTextFieldLabelVisibility = MonarchTextFieldLabelVisibility.Visible,
    requiredLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    size: MonarchFieldSize = MonarchFieldSize.Medium,
    invalid: Boolean = false,
    labelAction: (@Composable () -> Unit)? = null,
    optionalText: String? = null,
) {
    val parts = rememberTextFieldParts(
        modifier, label, placeholder, helperText, errorText, enabled, singleLine, leadingIcon, trailingIcon,
        required, successText, prefix, suffix, textStyle, labelVisibility, requiredLabel, interactionSource, size,
        invalid, labelAction, optionalText,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = parts.modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = parts.style,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = MonarchTextFieldTokens.maxLines(singleLine, minLines, maxLines),
        minLines = minLines,
        visualTransformation = visualTransformation,
        interactionSource = parts.source,
        cursorBrush = parts.cursor,
        decorationBox = { inner -> parts.Decoration(isEmpty = value.isEmpty(), inner = inner) },
    )
}

/**
 * A [MonarchTextField] that keeps the caret and the selection: the value is a `TextFieldValue`, for a field
 * that places the caret itself (after the text when it opens) or reads the selection (a mention or slash
 * trigger). Everything else is the same as the `String` field.
 *
 * @param value the text, the selection and the composition of the field.
 * @param onValueChange called with the new value when the user edits the field or moves the caret.
 * @param onTextLayout called with the layout of the text, for example to place a hint at the caret.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,

    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    required: Boolean = false,
    successText: String? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle? = null,
    labelVisibility: MonarchTextFieldLabelVisibility = MonarchTextFieldLabelVisibility.Visible,
    requiredLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    size: MonarchFieldSize = MonarchFieldSize.Medium,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val parts = rememberTextFieldParts(
        modifier, label, placeholder, helperText, errorText, enabled, singleLine, leadingIcon, trailingIcon,
        required, successText, prefix, suffix, textStyle, labelVisibility, requiredLabel, interactionSource, size,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = parts.modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = parts.style,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = MonarchTextFieldTokens.maxLines(singleLine, minLines, maxLines),
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = parts.source,
        cursorBrush = parts.cursor,
        decorationBox = { inner -> parts.Decoration(isEmpty = value.text.isEmpty(), inner = inner) },
    )
}

/** What both overloads draw around the engine's text, and the engine's style, caret colour and semantics. */
private class TextFieldParts(
    val source: MutableInteractionSource,
    val modifier: Modifier,
    val style: TextStyle,
    val cursor: Brush,
    private val decoration: @Composable (isEmpty: Boolean, inner: @Composable () -> Unit) -> Unit,
) {
    @Composable
    fun Decoration(isEmpty: Boolean, inner: @Composable () -> Unit) = decoration(isEmpty, inner)
}

@Composable
@Suppress("LongParameterList")
private fun rememberTextFieldParts(
    modifier: Modifier,
    label: String?,
    placeholder: String?,
    helperText: String?,
    errorText: String?,
    enabled: Boolean,
    singleLine: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    required: Boolean,
    successText: String?,
    prefix: (@Composable () -> Unit)?,
    suffix: (@Composable () -> Unit)?,
    textStyle: TextStyle?,
    labelVisibility: MonarchTextFieldLabelVisibility,
    requiredLabel: String?,
    interactionSource: MutableInteractionSource?,
    size: MonarchFieldSize,
    markedInvalid: Boolean = false,
    labelAction: (@Composable () -> Unit)? = null,
    optionalText: String? = null,
): TextFieldParts {
    val source = interactionSource ?: rememberFieldInteractionSource()
    val invalid = markedInvalid || !errorText.isNullOrBlank()
    val success = !invalid && !successText.isNullOrBlank()
    val state = rememberMonarchFieldState(source, enabled = enabled, invalid = invalid, success = success)
    val colors = MonarchTheme.colors
    val fieldColors = colors.fieldColors(state)
    val baseStyle = textStyle ?: monarchFieldTextStyle()
    val style = if (baseStyle.color == Color.Unspecified) baseStyle.copy(color = fieldColors.text) else baseStyle
    val name = monarchFieldName(label, required, requiredLabel)
    return TextFieldParts(
        source = source,
        // The name is the label (and "required"), also when the label is hidden; the error is the field's error.
        modifier = modifier.fillMaxWidth().monarchFieldSemantics(name, errorText),
        style = style,
        cursor = SolidColor(if (invalid) colors.dangerStrong else colors.accentStrong),
    ) { isEmpty, inner ->
        MonarchFieldFrame(
            label = label,
            labelVisible = labelVisibility == MonarchTextFieldLabelVisibility.Visible,
            required = required,
            enabled = enabled,
            helperText = helperText,
            errorText = errorText,
            successText = successText,
            labelAction = labelAction,
            optionalText = optionalText,
        ) {
            MonarchFieldBox(
                size = size,
                state = state,
                multiline = !singleLine,
                leading = leadingIcon,
                trailing = trailingIcon,
            ) {
                FieldContent(
                    multiline = !singleLine,
                    showPlaceholder = isEmpty && placeholder != null,
                    placeholder = placeholder,
                    enabled = enabled,
                    prefix = prefix,
                    suffix = suffix,
                    affixColor = fieldColors.affix,
                    inner = inner,
                )
            }
        }
    }
}

/**
 * The value area: the placeholder behind the engine's text (on the first line of a text area), between the
 * optional prefix and suffix.
 */
@Composable
@Suppress("LongParameterList")
private fun FieldContent(
    multiline: Boolean,
    showPlaceholder: Boolean,
    placeholder: String?,
    enabled: Boolean,
    prefix: (@Composable () -> Unit)?,
    suffix: (@Composable () -> Unit)?,
    affixColor: Color,
    inner: @Composable () -> Unit,
) {
    val text: @Composable () -> Unit = {
        // A text area starts its placeholder on the first line, as the web textarea does.
        Box(contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart) {
            if (showPlaceholder && placeholder != null) MonarchFieldPlaceholder(placeholder, enabled)
            inner()
        }
    }
    if (prefix == null && suffix == null) {
        text()
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchControlMetrics.FieldGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Affix(prefix, affixColor)
        Box(Modifier.weight(1f)) { text() }
        Affix(suffix, affixColor)
    }
}

@Composable
private fun Affix(content: (@Composable () -> Unit)?, color: Color) {
    if (content == null) return
    CompositionLocalProvider(
        LocalContentColor provides color,
        LocalTextStyle provides monarchFieldTextStyle().copy(color = color),
        content = content,
    )
}

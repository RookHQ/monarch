package dev.monarchui.components.forms.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.foundation.MonarchFieldSize
import dev.monarchui.internal.field.MonarchFieldBox
import dev.monarchui.internal.field.MonarchFieldPlaceholder
import dev.monarchui.internal.field.MonarchFieldState
import dev.monarchui.internal.field.monarchFieldTextStyle
import dev.monarchui.internal.field.rememberFieldInteractionSource
import dev.monarchui.internal.field.rememberMonarchFieldState
import dev.monarchui.theme.MonarchTheme

/**
 * An editable single-line field box without the label and message chrome, for a field whose box is the
 * anchor of a menu (a combobox). The text engine is the platform's `BasicTextField`; [modifier] goes on
 * it, so a menu anchor or the field's semantics apply to the box only.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchFieldInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    placeholder: String? = null,
    textColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    size: MonarchFieldSize = MonarchFieldSize.Medium,
    interactionSource: MutableInteractionSource = rememberFieldInteractionSource(),
    leading: (@Composable () -> Unit)? = null,
) {
    val state = rememberMonarchFieldState(interactionSource, enabled = enabled, invalid = invalid)
    val colors = MonarchTheme.colors
    val style = monarchFieldTextStyle()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        textStyle = style.copy(color = if (textColor == Color.Unspecified) colors.textDefault else textColor),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(if (invalid) colors.dangerStrong else colors.accentStrong),
        decorationBox = { inner ->
            MonarchFieldBox(size = size, state = state, leading = leading, trailing = trailing) {
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) MonarchFieldPlaceholder(placeholder, enabled)
                    inner()
                }
            }
        },
    )
}

/**
 * A read-only field box that looks like the field and ends in a chevron, for a select or a picker: the
 * [text] (or the placeholder when [isPlaceholder]), an optional [extra] control before the chevron.
 * [modifier] carries the tap, for example a menu anchor or a clickable. The box shows the focus ring while
 * it holds focus or while its menu is [expanded].
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchFieldTrigger(
    text: String,
    isPlaceholder: Boolean,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    enabled: Boolean = true,
    invalid: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null,
    glyph: MonarchFieldGlyph = MonarchFieldGlyph.ChevronDown,
    size: MonarchFieldSize = MonarchFieldSize.Medium,
) {
    var focused by remember { mutableStateOf(false) }
    val state = MonarchFieldState(enabled = enabled, invalid = invalid, focused = focused || expanded)
    val colors = MonarchTheme.colors
    MonarchFieldBox(
        modifier = Modifier.onFocusChanged { focused = it.hasFocus }.then(modifier),
        size = size,
        state = state,
        leading = leading,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                extra?.invoke()
                MonarchFieldGlyphIcon(glyph)
            }
        },
    ) {
        Text(
            text = text,
            style = monarchFieldTextStyle(),
            color = when {
                !enabled -> colors.textDisabled
                isPlaceholder -> colors.textSubtle
                else -> colors.textDefault
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

package dev.monarchui.components.actions.copyfield

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.actions.copybutton.MonarchCopyButtonTokens
import dev.monarchui.components.forms.formfield.MonarchFieldFrame
import dev.monarchui.components.forms.textfield.MonarchFieldGlyph
import dev.monarchui.components.forms.textfield.MonarchFieldGlyphIcon
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.foundation.MonarchFieldSize
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme

/**
 * A read-only value with a copy button: a webhook URL, a DNS record, an API
 * key, an embed snippet.
 *
 * The label sits above a read-only `MonarchTextField` box that holds the whole value (it can also be
 * selected and copied by touch and hold), and beside it a `MonarchCopyButton` with a copy icon and the word
 * "Copy". The button takes only the width it needs, the box gets the rest, and a value that does not fit
 * scrolls instead of being cut with an ellipsis. The button is named with the label: "Copy Webhook URL".
 *
 * @param value the value shown and copied.
 * @param label what the value is ("Webhook URL"): the label above the field and the end of the button's
 * spoken name.
 * @param modifier the modifier of the field and its button.
 * @param enabled whether the value can be copied. A disabled field uses the disabled colours.
 * @param multiline shows the whole value on several lines, for certificates, JSON and snippets.
 * @param monospace shows the value in the code style, for keys, codes and hostnames.
 * @param sensitive marks the clip as sensitive, so Android 13 and later hide it in the clipboard preview.
 * @param showConfirmation shows the copied state on the button. By default only below Android 13, where
 * the system shows no confirmation of its own.
 * @param resetAfterMillis how long the copied state lasts. Zero keeps it.
 * @param helperText a line under the field, such as where to paste the value.
 * @param copyIcon replaces Monarch's own copy icon on the button.
 * @param copiedIcon replaces Monarch's own check mark, shown on the button in the copied state.
 * @param onCopy called with the copied text after each copy.
 * @param copyLabel replaces the built-in button text "Copy" (`monarch_copybutton_copy`).
 * @param copiedLabel replaces the built-in button text "Copied" (`monarch_copybutton_copied`).
 * @param size the field size, as the text field's; the web's second copy field is small.
 * @param iconOnly shows the copy button as an icon button (ghost, small) instead of the text button.
 * @param labelVisible whether the label shows above the field. False keeps it as the spoken name only, as the web's
 * one-line values do.
 * @param copyVariant the variant of the text copy button; primary when the copy is the main action of a dialog.
 * @param copiedAnnouncement replaces the built-in state "Copied to clipboard"
 * (`monarch_copybutton_copied_announcement`).
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCopyField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiline: Boolean = false,
    monospace: Boolean = true,
    sensitive: Boolean = false,
    showConfirmation: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
    resetAfterMillis: Long = MonarchCopyButtonTokens.DEFAULT_RESET_MILLIS,
    helperText: String? = null,
    copyIcon: (@Composable () -> Unit)? = null,
    copiedIcon: (@Composable () -> Unit)? = null,
    onCopy: ((String) -> Unit)? = null,
    copyLabel: String? = null,
    copiedLabel: String? = null,
    copiedAnnouncement: String? = null,
    size: MonarchFieldSize = MonarchFieldSize.Medium,
    iconOnly: Boolean = false,
    labelVisible: Boolean = true,
    copyVariant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
) {
    MonarchFieldFrame(
        modifier = modifier,
        label = label,
        labelVisible = labelVisible,
        enabled = enabled,
        helperText = helperText,
    ) {
        // The button takes the width it needs, at most half the row, so a long label or a large font wraps the
        // button's text and the value keeps the rest.
        BoxWithConstraints {
            val buttonMax = maxWidth * MonarchCopyFieldTokens.BUTTON_MAX_SHARE
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchCopyFieldTokens.Gap),
                verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
            ) {
                MonarchTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.weight(1f).semantics { contentDescription = label },
                    enabled = enabled,
                    readOnly = true,
                    size = size,
                    singleLine = !multiline,
                    textStyle = MonarchCopyFieldTokens.textStyle(monospace, MonarchTheme.typography),
                )
                if (iconOnly) {
                    MonarchCopyButton(
                        value = value,
                        icon = copyIcon ?: MonarchDefaultIcons.Copy,
                        modifier = Modifier.heightIn(min = MonarchControlMetrics.fieldMinHeight(size)),
                        enabled = enabled,
                        sensitive = sensitive,
                        showConfirmation = showConfirmation,
                        resetAfterMillis = resetAfterMillis,
                        target = label,
                        copiedIcon = copiedIcon ?: MonarchDefaultIcons.Check,
                        onCopy = onCopy,
                        label = copyLabel,
                        copiedLabel = copiedLabel,
                        copiedAnnouncement = copiedAnnouncement,
                        size = MonarchButtonSize.Sm,
                    )
                } else {
                    MonarchCopyButton(
                        value = value,
                        // As tall as the field beside it, with the field's 14sp label, as on the web.
                        modifier = Modifier
                            .widthIn(max = buttonMax)
                            .heightIn(min = MonarchControlMetrics.fieldMinHeight(size)),
                        size = MonarchButtonSize.Md,
                        variant = copyVariant,
                        enabled = enabled,
                        sensitive = sensitive,
                        showConfirmation = showConfirmation,
                        resetAfterMillis = resetAfterMillis,
                        target = label,
                        leadingIcon = copyIcon ?: { MonarchFieldGlyphIcon(MonarchFieldGlyph.Copy) },
                        copiedIcon = copiedIcon ?: { MonarchFieldGlyphIcon(MonarchFieldGlyph.Check) },
                        onCopy = onCopy,
                        label = copyLabel,
                        copiedLabel = copiedLabel,
                        copiedAnnouncement = copiedAnnouncement,
                    )
                }
            }
        }
    }
}

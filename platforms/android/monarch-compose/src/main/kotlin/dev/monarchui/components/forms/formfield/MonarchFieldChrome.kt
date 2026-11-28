package dev.monarchui.components.forms.formfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.R
import dev.monarchui.components.forms.textfield.MonarchFieldGlyph
import dev.monarchui.components.forms.textfield.MonarchFieldGlyphIcon
import dev.monarchui.foundation.MonarchControlMetrics
import dev.monarchui.theme.MonarchTheme

/**
 * The chrome every Monarch field shares: the label above, the field in the middle, and one supporting line
 * below it (the error, else the success text, else the helper text), so the lines can never overlap.
 *
 * It owns the layout and the label and message semantics, not the field: [content] draws the box and applies
 * [monarchFieldSemantics] to the node TalkBack lands on. The visible label and the error line are hidden from
 * TalkBack (the field node carries the name and the error, as `MonarchFormField` does); the helper and
 * success lines stay readable. A new error is announced once through a polite live region.
 *
 * The frame fills the available width, like the web field; pass a `Modifier.width(...)` to narrow it.
 *
 * @param label the visible label, or `null` for none.
 * @param labelVisible whether [label] is drawn. A hidden label still names the field in [content].
 * @param required shows the required mark after the label.
 * @param errorIcon a caller icon before the error message; `null` draws Monarch's own mark.
 * @param labelAction an action at the end of the label row, such as a "Forgot password?" link.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchFieldFrame(
    modifier: Modifier = Modifier,
    label: String? = null,
    labelVisible: Boolean = true,
    required: Boolean = false,
    enabled: Boolean = true,
    helperText: String? = null,
    errorText: String? = null,
    successText: String? = null,
    optionalText: String? = null,
    errorIcon: (@Composable () -> Unit)? = null,
    labelAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val error = MonarchFormFieldTokens.error(errorText)
    val success = successText?.takeIf { error == null && it.isNotBlank() }
    val helper = helperText?.takeIf { error == null && success == null && it.isNotBlank() }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchFormFieldTokens.Gap),
    ) {
        if (label != null && labelVisible) {
            if (labelAction == null) {
                MonarchFieldLabel(label, enabled, required, optionalText)
            } else {
                // The action (a "Forgot password?" link) sits at the end of the label row, as on the web.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { MonarchFieldLabel(label, enabled, required, optionalText) }
                    labelAction()
                }
            }
        }
        Box {
            content()
            // Always present, so a new error is announced once as a change of this live region. It merges its
            // own semantics, so a text field around the frame does not take the live region for itself. It sits
            // in the content's box with no size, not as a row of the column, so it adds no gap below the field.
            Box(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    if (error != null) contentDescription = error
                },
            )
        }
        if (error != null) MonarchFieldMessage(error, MonarchFieldMessageTone.Error, errorIcon, hidden = true)
        if (success != null) MonarchFieldMessage(success, MonarchFieldMessageTone.Success, null, hidden = false)
        if (helper != null) MonarchFieldHelper(helper)
    }
}

/** The name and error of the field node: TalkBack reads [name], the role, the value and [errorText]. */
internal fun Modifier.monarchFieldSemantics(name: String?, errorText: String?): Modifier = semantics {
    if (name != null) contentDescription = name
    MonarchFormFieldTokens.error(errorText)?.let { error(it) }
}

/** What TalkBack reads as the name of a field: the label, with the spoken "required" after it. */
@Composable
internal fun monarchFieldName(label: String?, required: Boolean, requiredName: String?): String? =
    when {
        label == null -> null
        !required -> label
        else -> requiredName ?: stringResource(R.string.monarch_textfield_required, label)
    }

/** The label above a field: 14sp medium, with the required mark or the optional text after it. */
@Composable
internal fun MonarchFieldLabel(label: String, enabled: Boolean, required: Boolean, optionalText: String?) {
    val tokens = MonarchFormFieldTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = MonarchTheme.typography.label,
            color = tokens.labelColor(enabled, colors),
        )
        if (required) {
            Text(
                text = stringResource(R.string.monarch_formfield_required_mark),
                style = MonarchTheme.typography.label,
                color = tokens.requiredColor(enabled, colors),
            )
        } else if (optionalText != null) {
            Text(
                text = optionalText,
                style = MonarchTheme.typography.caption,
                color = tokens.helperColor(enabled, colors),
            )
        }
    }
}

/** The helper line: 12sp, muted. It stays muted when the field is disabled, as on the web (only the label dims). */
@Composable
internal fun MonarchFieldHelper(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        style = MonarchTheme.typography.caption,
        color = MonarchTheme.colors.textMuted,
    )
}

internal enum class MonarchFieldMessageTone { Error, Success }

/** An error or success line: 12sp medium, an icon before the message (Monarch's own when [icon] is `null`). */
@Composable
internal fun MonarchFieldMessage(
    text: String,
    tone: MonarchFieldMessageTone,
    icon: (@Composable () -> Unit)?,
    hidden: Boolean,
) {
    val colors = MonarchTheme.colors
    val color = if (tone == MonarchFieldMessageTone.Error) colors.dangerStrong else colors.successStrong
    Row(
        modifier = Modifier.fillMaxWidth().then(
            if (hidden) Modifier.clearAndSetSemantics { } else Modifier.semantics(mergeDescendants = true) { },
        ),
        horizontalArrangement = Arrangement.spacedBy(MonarchFormFieldTokens.InlineGap),
        verticalAlignment = Alignment.Top,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            if (icon != null) {
                icon()
            } else {
                val glyph = when (tone) {
                    MonarchFieldMessageTone.Error -> MonarchFieldGlyph.Error
                    MonarchFieldMessageTone.Success -> MonarchFieldGlyph.Check
                }
                // The 16dp mark sits on the first line of the 12sp text, which is about 18dp tall.
                Box(Modifier.size(MonarchControlMetrics.IconSize), contentAlignment = Alignment.Center) {
                    MonarchFieldGlyphIcon(glyph)
                }
            }
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MonarchTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = color,
        )
    }
}

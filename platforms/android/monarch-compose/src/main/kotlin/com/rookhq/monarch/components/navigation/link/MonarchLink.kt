package com.rookhq.monarch.components.navigation.link

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchControlState
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A text link that is an element of its own: under a field, at the end of a
 * row, in a footer, after a paragraph.
 *
 * Compose has no link control, so Monarch builds one from `Text` and
 * `clickable`: the link colour, an underline that also marks the pressed and
 * focused states, a reserved 48dp touch target and the focus ring. It keeps
 * the type size of the text around it (`LocalTextStyle`), as the web link
 * does. For a link inside a sentence, use a `LinkAnnotation` with
 * [MonarchLinkDefaults.styles] in the sentence's annotated string.
 *
 * The link does not decide where it goes: [onClick] opens the screen or the
 * URL, for example with `LocalUriHandler.current.openUri(url)`.
 *
 * @param text the link text. Say where it goes: "View all invoices", not "Click here".
 * @param onClick called when the link is activated. Not called when not [enabled].
 * @param modifier the modifier of the link.
 * @param variant the look of the link.
 * @param enabled whether the link can be activated. A disabled link uses the disabled text colour.
 * @param external whether the link leaves the app. TalkBack reads [externalLabel] after the text.
 * @param trailingIcon an icon after the text, for example an arrow for an external link, as a
 * `MonarchIcon` with a `null` content description. It takes the colour of the link.
 * @param externalLabel replaces the built-in "opens in the browser" (`monarch_link_external`), read after the
 * text of an [external] link.
 * @param actionLabel replaces the built-in "open link" (`monarch_link_action`), which TalkBack reads as the action:
 * "double tap to open link".
 * @param interactionSource the source of the press, focus and hover interactions of the link.
 */
@Composable
public fun MonarchLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchLinkVariant = MonarchLinkVariant.Inline,
    enabled: Boolean = true,
    external: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    externalLabel: String? = null,
    actionLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = rememberMonarchControlState(source, enabled)
    val appearance = MonarchLinkTokens.appearance(variant, state, MonarchTheme.isHighContrast, MonarchTheme.colors)
    val externalNote = monarchString(externalLabel, R.string.monarch_link_external)
    val action = monarchString(actionLabel, R.string.monarch_link_action)
    val shape = MonarchTheme.shapes.xs
    // A standalone link sits on its own line beside buttons, so it takes the 14sp label size; an inline link
    // follows the text around it.
    val base = if (variant == MonarchLinkVariant.Standalone) MonarchTheme.typography.label else LocalTextStyle.current
    Row(
        modifier = modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(shape)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = source,
                // The underline is the press feedback, as on the web; a ripple would fill the 48dp target.
                indication = null,
                enabled = enabled,
                onClickLabel = action,
                onClick = onClick,
            )
            .semantics { if (external) contentDescription = "$text, $externalNote" },
        horizontalArrangement = Arrangement.spacedBy(MonarchLinkTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            style = base.merge(
                TextStyle(
                    color = appearance.color,
                    fontWeight = appearance.fontWeight,
                    textDecoration = if (appearance.underlined) TextDecoration.Underline else TextDecoration.None,
                ),
            ),
        )
        // The web draws the box-and-arrow mark itself after the text of an external link.
        val mark = trailingIcon ?: if (external) MonarchDefaultIcons.External else null
        if (mark != null) {
            CompositionLocalProvider(LocalContentColor provides appearance.color) { mark() }
        }
    }
}

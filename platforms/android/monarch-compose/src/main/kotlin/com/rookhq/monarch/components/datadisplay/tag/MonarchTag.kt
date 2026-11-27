package com.rookhq.monarch.components.datadisplay.tag

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A read-only tag: a label for user data, for example "Design" or "Priority: high".
 *
 * Material 3 has no read-only chip (every chip takes a tap), so this form is
 * drawn by Monarch. It is not interactive, has no touch target and is read as
 * its text. For a status of the system, such as "Draft", use `MonarchBadge`.
 *
 * @param text the label.
 * @param modifier the modifier of the tag.
 * @param tone the colour role. The tone is never the only carrier of meaning: say it in the text.
 * @param size the text style and visible height.
 * @param leadingIcon a decorative icon before the text, for example a `MonarchIcon` of size `Sm`. It is
 * hidden from TalkBack.
 */
@Composable
public fun MonarchTag(
    text: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Neutral,
    size: MonarchTagSize = MonarchTagSize.Md,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MonarchTagTokens.colors(tone, MonarchTheme.colors)
    Row(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Control)
            .heightIn(min = MonarchTagTokens.minHeight(size))
            .padding(horizontal = MonarchTagTokens.HorizontalPadding, vertical = MonarchTagTokens.VerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchTagTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            if (leadingIcon != null) Box(Modifier.clearAndSetSemantics { }) { leadingIcon() }
            Text(text = text, style = MonarchTagTokens.textStyle(size, MonarchTheme.typography), color = colors.content)
        }
    }
}

/**
 * A tag the user can remove, for example a recipient or a label on a record.
 *
 * The Monarch tag with a close mark after the text (see `MonarchChip`). The whole tag is the remove action, as
 * in Material 3 input chips: TalkBack reads it as a button named "Remove" and the text, and Delete or Backspace
 * on a hardware keyboard removes a focused tag. Remove the tag from your data in [onRemove].
 *
 * @param text the label.
 * @param onRemove called when the user activates the tag. Not called when not [enabled].
 * @param removeIcon the close mark after the text, for example a `MonarchIcon` of size `Sm` with a `null`
 * content description. `null` draws the Monarch close mark. It is decorative.
 * @param modifier the modifier of the tag.
 * @param tone the colour role.
 * @param size the text style and visible height. The tag has a 48dp touch target at both sizes.
 * @param enabled whether the user can remove the tag.
 * @param leadingIcon a decorative icon before the text.
 * @param removeLabel the TalkBack name of the tag. `null` uses the built-in string `monarch_tag_remove`,
 * "Remove" and the text.
 * @param interactionSource the source of the press, focus and hover interactions of the tag.
 */
@Composable
public fun MonarchTag(
    text: String,
    onRemove: () -> Unit,
    removeIcon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Neutral,
    size: MonarchTagSize = MonarchTagSize.Md,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    removeLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    MonarchChip(
        text = text,
        onRemove = onRemove,
        modifier = modifier,
        tone = tone,
        size = size,
        enabled = enabled,
        leadingIcon = leadingIcon,
        removeIcon = removeIcon,
        removeLabel = removeLabel,
        interactionSource = interactionSource,
    )
}

/**
 * A tag the user can switch on and off, for example a filter.
 *
 * The Monarch chip (see `MonarchChip`): an outlined surface when off, the accent subtle fill when on. TalkBack
 * reads it as a checkbox that is selected or not, so the state does not depend on colour.
 *
 * @param text the label.
 * @param selected whether the tag is on.
 * @param onSelectedChange called with the new state when the user toggles the tag. Not called when not
 * [enabled].
 * @param modifier the modifier of the tag.
 * @param size the text style and visible height. The tag has a 48dp touch target at both sizes.
 * @param enabled whether the user can toggle the tag.
 * @param leadingIcon a decorative icon before the text.
 * @param selectedIcon shown in place of [leadingIcon] while [selected], for example a check mark as a
 * `MonarchIcon` of size `Sm` with a `null` content description. Optional: the fill and border already show the state.
 * @param interactionSource the source of the press, focus and hover interactions of the tag.
 */
@Composable
public fun MonarchTag(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchTagSize = MonarchTagSize.Md,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    selectedIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    MonarchChip(
        text = text,
        selected = selected,
        onSelectedChange = onSelectedChange,
        modifier = modifier,
        size = size,
        enabled = enabled,
        leadingIcon = leadingIcon,
        selectedIcon = selectedIcon,
        interactionSource = interactionSource,
    )
}

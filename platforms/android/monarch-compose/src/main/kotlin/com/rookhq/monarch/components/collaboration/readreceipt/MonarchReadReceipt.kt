package com.rookhq.monarch.components.collaboration.readreceipt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarGroup
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * "Seen" under the newest message the other people have read: the text
 * "Seen", "Seen by 3" or the readers' avatars, and always the full list of
 * names for TalkBack ("Seen by Ana, Ben and Chloé").
 *
 * Built by Monarch from a `Row`, `MonarchAvatarGroup` and `MonarchAvatar`:
 * Material 3 has no receipt. The app decides which message carries it and who
 * has read it; leave the sender out. Nothing is drawn while [readers] is
 * empty. The receipt is one TalkBack element; with [onShowReaders] it is a
 * button with a 48dp touch target, for example to open the list of readers
 * with times.
 *
 * @param readers who has read the message, in order.
 * @param modifier the modifier of the receipt. Align it under the message with the conversation's own layout.
 * @param display "Seen", "Seen by 3", or the readers' avatars.
 * @param maxAvatars with the avatars display, show at most this many faces, then "+N". At least one is shown.
 * @param onShowReaders makes the receipt a button that calls this.
 * @param icon a decorative mark before the text of the text and count displays, such as a double check.
 * Monarch bundles no icons; it takes the muted content colour. The text carries the meaning.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the interaction source of the button, when [onShowReaders] is given.
 */
@Composable
public fun MonarchReadReceipt(
    readers: List<MonarchReadReceiptReader>,
    modifier: Modifier = Modifier,
    display: MonarchReadReceiptDisplay = MonarchReadReceiptDisplay.Text,
    maxAvatars: Int = 3,
    onShowReaders: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.DoneAll,
    labels: MonarchReadReceiptLabels = MonarchReadReceiptLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    if (readers.isEmpty()) return
    val tokens = MonarchReadReceiptTokens
    val full = spokenSentence(readers, labels)
    val visible = if (display == MonarchReadReceiptDisplay.Count) {
        labels.seenByCount?.invoke(readers.size)
            ?: pluralStringResource(R.plurals.monarch_readreceipt_seen_by_count, readers.size, readers.size)
    } else {
        monarchString(labels.seen, R.string.monarch_readreceipt_seen)
    }
    val shape = MonarchShapeDefaults.Row
    val interactive = if (onShowReaders != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .clip(shape)
            .monarchFocusRing(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onShowReaders,
            )
    } else {
        Modifier
    }
    val color = tokens.contentColor(MonarchTheme.colors)
    Row(
        modifier = modifier
            .then(interactive)
            .clearAndSetSemantics {
                contentDescription = full
                if (onShowReaders != null) {
                    role = Role.Button
                    onClick {
                        onShowReaders()
                        true
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (display == MonarchReadReceiptDisplay.Avatars) {
            MonarchAvatarGroup(
                items = readers,
                label = full,
                maxVisible = tokens.avatarLimit(maxAvatars),
                size = MonarchAvatarSize.Sm,
            ) { reader ->
                MonarchAvatar(
                    image = reader.image,
                    name = reader.name,
                    size = MonarchAvatarSize.Sm,
                    seed = reader.id ?: reader.name,
                    decorative = true,
                )
            }
        } else if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides color) { icon() }
        }
        Text(text = visible, style = MonarchTheme.typography.caption, color = color)
    }
}

/** "Seen by Ana, Ben and Chloé", from the labels or the built-in strings. */
@Composable
private fun spokenSentence(readers: List<MonarchReadReceiptReader>, labels: MonarchReadReceiptLabels): String {
    val names = readers.map { it.name }
    val separator = stringResource(R.string.monarch_readreceipt_list_separator)
    val pairFormat = stringResource(R.string.monarch_readreceipt_list_last_pair)
    val locale = LocalConfiguration.current.locales[0]
    val joined = labels.list?.invoke(names)
        ?: MonarchReadReceiptTokens.joinNames(names, separator) { first, last ->
            String.format(locale, pairFormat, first, last)
        }
    return labels.seenBy?.invoke(joined) ?: stringResource(R.string.monarch_readreceipt_seen_by, joined)
}

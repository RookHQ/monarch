package dev.monarchui.components.layout.notificationsbell

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The entry to the notifications: a bell with the unread count, as a
 * `MonarchIconButton` with a `BadgedBox`, or as a labelled row.
 *
 * The bell only opens the notifications: the app presents its own list from
 * [onClick], in a `ModalBottomSheet` or on a screen of its own. It is one
 * TalkBack button named "Notifications" with the count as its state ("3
 * unread"), read once; the badge and the bell are hidden from TalkBack. The
 * badge shows the count in digits up to 99 and "99+" above, while TalkBack
 * reads the exact count. Monarch bundles no icons, so the app passes the bell.
 *
 * @param onClick opens the notifications.
 * @param modifier the modifier of the button or the row.
 * @param unreadCount the number of unread notifications. Zero or less shows no badge.
 * @param variant `Icon` for a top app bar, `Row` for a navigation drawer or a menu.
 * @param enabled whether the person can open the notifications.
 * @param title replaces the built-in name "Notifications", which is also the visible title of a row.
 * @param unreadLabel replaces the built-in state "3 unread". Pass the whole phrase for the current count.
 * @param interactionSource the interaction source of the button or the row.
 * @param icon the bell, for example a `MonarchIcon` with a `null` content description. It is hidden from
 * TalkBack.
 */
@Composable
public fun MonarchNotificationsBell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    variant: MonarchNotificationsBellVariant = MonarchNotificationsBellVariant.Icon,
    enabled: Boolean = true,
    title: String? = null,
    unreadLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit,
) {
    val tokens = MonarchNotificationsBellTokens
    val colors = MonarchTheme.colors
    val name = monarchString(title, R.string.monarch_notificationsbell_title)
    val count = unreadCount.coerceAtLeast(0)
    val unread = when {
        count == 0 -> null
        unreadLabel != null -> unreadLabel
        else -> pluralStringResource(R.plurals.monarch_notificationsbell_unread, count, count)
    }
    val content = tokens.contentColor(enabled, colors)
    when (variant) {
        MonarchNotificationsBellVariant.Icon -> MonarchIconButton(
            label = name,
            onClick = onClick,
            modifier = modifier.semantics { if (unread != null) stateDescription = unread },
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            CompositionLocalProvider(LocalContentColor provides content) {
                BadgedBox(
                    modifier = Modifier.clearAndSetSemantics {},
                    badge = { if (tokens.showsBadge(count)) CountBadge(count) },
                ) {
                    icon()
                }
            }
        }
        MonarchNotificationsBellVariant.Row -> Row(
            modifier = modifier
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) { if (unread != null) stateDescription = unread }
                .heightIn(min = tokens.RowMinHeight)
                .padding(horizontal = tokens.RowHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides content) {
                Box(modifier = Modifier.clearAndSetSemantics {}) { icon() }
            }
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MonarchTheme.typography.body,
                color = content,
            )
            if (tokens.showsBadge(count)) {
                Box(modifier = Modifier.clearAndSetSemantics {}) { CountBadge(count) }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    val colors = MonarchNotificationsBellTokens.badgeColors(MonarchTheme.colors)
    Badge(containerColor = colors.container, contentColor = colors.content) {
        Text(text = MonarchNotificationsBellTokens.badgeText(count))
    }
}

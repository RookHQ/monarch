package dev.monarchui.components.collaboration.chatdock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.feedback.spinner.MonarchSpinner
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Chat that stays one tap away on any screen: a floating action button with the unread count that opens the chat
 * in a bottom sheet.
 *
 * Built on Material 3 `FloatingActionButton`, `BadgedBox` and `MonarchBottomSheet`. The app supplies the chat in
 * [content] (its own screens, or the Rook Chat app's), and decides what [unread] and [loading] say. The launcher is
 * read as its label and the unread count, such as "Rook Chat, 3 unread". Place it in a `Scaffold`'s
 * `floatingActionButton`.
 *
 * @param open whether the chat sheet is shown.
 * @param onOpenChange called with `true` when the launcher is tapped and `false` when the sheet is dismissed.
 * @param modifier the modifier of the launcher.
 * @param unread the number of unread messages; 0 shows no badge.
 * @param loading shows a spinner and "Loading chat…" in the sheet instead of [content].
 * @param label the spoken name of the launcher and the sheet. `null` uses "Rook Chat".
 * @param loadingLabel the text while [loading]. `null` uses "Loading chat…".
 * @param icon the launcher icon. Defaults to a chat bubble.
 * @param content the chat, in the sheet.
 */
@Composable
public fun MonarchChatDock(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    unread: Int = 0,
    loading: Boolean = false,
    label: String? = null,
    loadingLabel: String? = null,
    icon: @Composable () -> Unit = { MonarchIcon(MonarchGlyphs.ChatBubble, contentDescription = null) },
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val name = monarchString(label, R.string.monarch_chatdock_label)
    val badge = MonarchChatDockTokens.badgeText(unread)
    val spoken = if (unread > 0) {
        "$name, ${pluralStringResource(R.plurals.monarch_chatdock_unread, unread, unread)}"
    } else {
        name
    }
    FloatingActionButton(
        onClick = { onOpenChange(true) },
        modifier = modifier.semantics { contentDescription = spoken },
        containerColor = MonarchChatDockTokens.launcherFill(colors),
        contentColor = MonarchChatDockTokens.launcherContent(colors),
    ) {
        BadgedBox(
            modifier = Modifier.clearAndSetSemantics { },
            badge = {
                if (badge != null) {
                    Badge(
                        containerColor = MonarchChatDockTokens.badgeFill(colors),
                        contentColor = MonarchChatDockTokens.badgeContent(colors),
                    ) { Text(badge) }
                }
            },
        ) { icon() }
    }
    if (open) {
        MonarchBottomSheet(
            onDismissRequest = { onOpenChange(false) },
            modifier = Modifier.semantics { paneTitle = name },
        ) {
            if (loading) {
                val text = monarchString(loadingLabel, R.string.monarch_chatdock_loading)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(MonarchChatDockTokens.LoadingPadding),
                    horizontalArrangement = Arrangement.spacedBy(
                        MonarchChatDockTokens.LoadingGap,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchSpinner(label = text)
                    MonarchText(text = text, modifier = Modifier.clearAndSetSemantics { }, color = colors.textMuted)
                }
            } else {
                content()
            }
        }
    }
}

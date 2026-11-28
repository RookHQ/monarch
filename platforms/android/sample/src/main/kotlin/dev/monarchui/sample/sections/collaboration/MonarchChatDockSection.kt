package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.monarchui.components.collaboration.chatdock.MonarchChatDock
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ChatDock. Opening the chat marks the messages as read. */
object MonarchChatDockSection : CataloguePage {
    override val title = "ChatDock"

    private val conversations = listOf(
        "Design review" to "Notes are in the doc.",
        "Launch planning" to "Moving the date to Friday.",
        "Ada Lovelace" to "Thanks!",
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var unread by rememberSaveable { mutableStateOf(conversations.size) }
        MonarchText(text = "The page of the host app.", color = MonarchTheme.colors.textMuted)
        MonarchChatDock(
            open = open,
            onOpenChange = {
                open = it
                if (it) unread = 0
            },
            unread = unread,
        ) {
            MonarchText(
                text = "Chat",
                modifier = Modifier.padding(MonarchTheme.spacing.space4),
                style = MonarchTextStyle.Subheading,
            )
            conversations.forEach { (title, message) ->
                MonarchDivider()
                ListItem(
                    headlineContent = { MonarchText(text = title, style = MonarchTextStyle.BodyStrong) },
                    supportingContent = { MonarchText(text = message, color = MonarchTheme.colors.textMuted) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

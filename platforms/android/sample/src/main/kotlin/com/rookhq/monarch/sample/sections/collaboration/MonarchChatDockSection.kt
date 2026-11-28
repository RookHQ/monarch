package com.rookhq.monarch.sample.sections.collaboration

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
import com.rookhq.monarch.components.collaboration.chatdock.MonarchChatDock
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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

package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposer
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerMentions
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerMode
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposerReply
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of MessageComposer: type @ to mention, send, reply and switch to an internal note. */
object MonarchMessageComposerSection : CataloguePage {
    override val title = "MessageComposer"

    private const val LIMIT = 280

    private val people = listOf(
        MonarchMentionItem(id = "u_1", label = "Ana Ruiz", detail = "Design"),
        MonarchMentionItem(id = "u_2", label = "Ben Okafor", detail = "Engineering"),
    )
    private val modes = listOf(
        MonarchMessageComposerMode("reply", "Reply"),
        MonarchMessageComposerMode("note", "Internal note", note = true, sendLabel = "Add note"),
    )

    @Composable
    override fun Content() {
        var last by rememberSaveable { mutableStateOf("Nothing sent yet") }
        var replying by rememberSaveable { mutableStateOf(true) }
        var query by remember { mutableStateOf<MonarchMentionQuery?>(null) }
        val matches = query?.let { q -> people.filter { it.label.contains(q.text, ignoreCase = true) } }.orEmpty()
        CatalogueCaption("Mentions, emoji and a reply")
        MonarchMessageComposer(
            label = "Message #design",
            state = rememberTextFieldState(),
            onSend = { last = it.text },
            placeholder = "Message #design",
            replyTo = MonarchMessageComposerReply("Ana Ruiz", "Are we still on for the review?").takeIf { replying },
            mentions = MonarchMessageComposerMentions(matches) { query = it },
            maxLength = LIMIT,
            onAttach = {},
            onDismissReply = { replying = false },
            attachIcon = { MonarchIcon(SampleIcons.Attach, contentDescription = null) },
            emojiPicker = { onPick, onDismiss ->
                Box {
                    MonarchMenu(expanded = true, onDismissRequest = onDismiss) {
                        listOf("👍", "🎉", "👀").forEach { emoji ->
                            MonarchMenuItem(text = emoji, onClick = { onPick(emoji) })
                        }
                    }
                }
            },
        )
        MonarchText(text = "Last sent: $last", style = MonarchTextStyle.Caption)
        CatalogueCaption("Reply or internal note")
        MonarchMessageComposer(
            label = "Reply to customer",
            state = rememberTextFieldState(),
            onSend = {},
            placeholder = "Write a reply",
            modes = modes,
        )
        CatalogueCaption("Disabled")
        MonarchMessageComposer(
            label = "Message #announcements",
            state = rememberTextFieldState(),
            onSend = {},
            enabled = false,
            disabledReason = "Only admins can post in #announcements.",
        )
    }
}

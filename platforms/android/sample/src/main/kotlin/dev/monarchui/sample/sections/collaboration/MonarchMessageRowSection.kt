package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRow
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowAction
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowAuthor
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowStatus
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowVariant
import dev.monarchui.components.collaboration.reactionbar.MonarchReactionBarItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of MessageRow: long press a message for its actions, react, retry a failed one. */
object MonarchMessageRowSection : CataloguePage {
    override val title = "MessageRow"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val MINUTE = 60_000L
    private const val FIVE = 5
    private const val FOUR = 4
    private const val THREE = 3

    private val ana = MonarchMessageRowAuthor("Ana Ruiz")
    private val ben = MonarchMessageRowAuthor("Ben Okafor")

    @Composable
    override fun Content() {
        var reacted by rememberSaveable { mutableStateOf(true) }
        var deleted by rememberSaveable { mutableStateOf(false) }
        var highlighted by rememberSaveable { mutableStateOf(false) }
        var status by rememberSaveable { mutableStateOf(MonarchMessageRowStatus.Failed) }
        CatalogueCaption("A group, with reactions and a thread")
        MonarchMessageRow(
            author = ana,
            time = NOW - FIVE * MINUTE,
            now = NOW,
            tags = listOf("Guest"),
            reactions = listOf(
                MonarchReactionBarItem("👍", if (reacted) THREE else 2, reacted = reacted, label = "thumbs up"),
            ),
            replyCount = FOUR,
            deleted = deleted,
            highlighted = highlighted,
            actions = listOf(
                MonarchMessageRowAction("Highlight", { highlighted = !highlighted }),
                MonarchMessageRowAction("Delete", { deleted = true }, destructive = true),
            ),
            quickReactions = listOf("👍", "🎉", "👀"),
            onReactionToggle = { emoji, on -> if (emoji == "👍") reacted = on },
            onOpenThread = {},
        ) { MonarchText("The build is green again. Long press for actions.") }
        MonarchMessageRow(author = ana, time = NOW - FOUR * MINUTE, now = NOW, grouped = true, edited = true) {
            MonarchText("Shipping after lunch.")
        }
        CatalogueCaption("Sending states")
        MonarchMessageRow(
            author = ben,
            time = NOW,
            now = NOW,
            status = status,
            onRetry = { status = MonarchMessageRowStatus.Pending },
            onDiscard = { status = MonarchMessageRowStatus.Sent },
        ) { MonarchText("Can you check the invoice?") }
        CatalogueCaption("Internal note and system line")
        MonarchMessageRow(
            author = ben,
            time = NOW,
            now = NOW,
            variant = MonarchMessageRowVariant.Internal,
            tags = listOf("Internal"),
        ) { MonarchText("Customer is on the old plan.") }
        MonarchMessageRow(author = ana, time = NOW, now = NOW, variant = MonarchMessageRowVariant.System) {
            MonarchText("Ana Ruiz joined the channel")
        }
    }
}

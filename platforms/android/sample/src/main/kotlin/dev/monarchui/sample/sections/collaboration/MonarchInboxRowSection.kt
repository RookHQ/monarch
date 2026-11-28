package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRow
import dev.monarchui.components.collaboration.inboxrow.MonarchInboxRowSwipeAction
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of InboxRow. */
object MonarchInboxRowSection : CataloguePage {
    override val title = "InboxRow"

    private const val Minute = 60_000L
    private const val FiveMinutes = 5 * Minute
    private const val ThreeHours = 3 * 60 * Minute
    private const val FortyDays = 40 * 24 * 60 * Minute
    private const val Unread = 4

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var anaUnread by rememberSaveable { mutableStateOf(true) }
        var archived by rememberSaveable { mutableStateOf(false) }
        var selected by rememberSaveable { mutableStateOf(false) }
        var opened by rememberSaveable { mutableStateOf("none") }
        CatalogueCaption("Swipe: toward the end marks read, toward the start archives")
        Column {
            MonarchInboxRow(
                title = "Ana Lima",
                onClick = { opened = "Ana Lima" },
                unread = anaUnread,
                preview = "Can we move the call to Thursday?",
                timestamp = now - FiveMinutes,
                swipeActions = listOf(
                    MonarchInboxRowSwipeAction.ToggleRead { anaUnread = !anaUnread },
                    MonarchInboxRowSwipeAction.Archive { archived = true },
                ),
            )
            if (!archived) {
                MonarchInboxRow(
                    title = "Billing",
                    onClick = { opened = "Billing" },
                    unreadCount = Unread,
                    preview = "Invoice 2041 was paid",
                    timestamp = now - ThreeHours,
                    badges = { MonarchBadge(text = "Urgent", tone = MonarchTone.Danger) },
                    trailing = { MonarchBadge(text = "#2041") },
                )
            }
            MonarchInboxRow(
                title = "Ben Okafor",
                onClick = { opened = "Ben Okafor" },
                selected = selected,
                onSelectedChange = { selected = it },
                preview = "Long-press to select me",
                timestamp = now - FortyDays,
            )
        }
        MonarchText(text = "Opened: $opened")
    }
}

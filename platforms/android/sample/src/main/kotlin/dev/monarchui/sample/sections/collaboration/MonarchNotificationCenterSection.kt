package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.collaboration.notificationcenter.MonarchNotificationCenter
import dev.monarchui.components.collaboration.notificationcenter.MonarchNotificationCenterItem
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of NotificationCenter: open or swipe a notification to mark it read. */
object MonarchNotificationCenterSection : CataloguePage {
    override val title = "NotificationCenter"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val HOUR = 3_600_000L
    private const val YESTERDAY = 26
    private const val LAST_WEEK = 120
    private const val HEIGHT_STEPS = 5

    private val all = listOf(
        MonarchNotificationCenterItem(
            id = "mention",
            title = "Ana mentioned you in #design",
            time = NOW - HOUR,
            message = "Can you review the new header?",
            actorName = "Ana Ruiz",
        ),
        MonarchNotificationCenterItem(id = "paid", title = "Invoice 2041 was paid", time = NOW - 2 * HOUR),
        MonarchNotificationCenterItem(
            id = "invite",
            title = "Ben invited you to Q4 planning",
            time = NOW - YESTERDAY * HOUR,
            actorName = "Ben Okafor",
        ),
        MonarchNotificationCenterItem(id = "renewed", title = "Your plan renewed", time = NOW - LAST_WEEK * HOUR),
    )

    @Composable
    override fun Content() {
        var read by rememberSaveable { mutableStateOf(setOf("renewed")) }
        val items = all.map { item ->
            MonarchNotificationCenterItem(
                id = item.id,
                title = item.title,
                time = item.time,
                message = item.message,
                read = item.id in read,
                actorName = item.actorName,
            )
        }
        CatalogueCaption("In a sheet or a screen")
        MonarchNotificationCenter(
            items = items,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            now = NOW,
            onMarkRead = { read = read + it },
            onMarkAllRead = { read = all.map { it.id }.toSet() },
        )
    }
}

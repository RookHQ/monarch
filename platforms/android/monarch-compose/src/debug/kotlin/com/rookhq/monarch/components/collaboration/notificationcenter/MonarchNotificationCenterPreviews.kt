package com.rookhq.monarch.components.collaboration.notificationcenter

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

/** 14 November 2026, 14:00 UTC. */
private const val PreviewNow = 1_794_664_800_000L
private const val PreviewHour = 3_600_000L

private val PreviewItems = listOf(
    MonarchNotificationCenterItem(
        id = "1",
        title = "Ana mentioned you in #design",
        time = PreviewNow - PreviewHour,
        message = "Can you review the new header?",
        actorName = "Ana Ruiz",
    ),
    MonarchNotificationCenterItem(
        id = "2",
        title = "Invoice 2041 was paid",
        time = PreviewNow - 26 * PreviewHour,
        read = true,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchNotificationCenterPreview() {
    PreviewFrame {
        MonarchNotificationCenter(items = PreviewItems, now = PreviewNow, onMarkRead = {}, onMarkAllRead = {})
    }
}

@MonarchPreviews
@Composable
internal fun MonarchNotificationCenterEmptyPreview() {
    PreviewFrame {
        MonarchNotificationCenter(items = emptyList(), now = PreviewNow)
    }
}

package com.rookhq.monarch.components.collaboration.inboxrow

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewNow = 1_700_000_000_000L
private const val PreviewMinutes = 5 * 60_000L

@MonarchPreviews
@Composable
internal fun MonarchInboxRowPreview() {
    PreviewFrame {
        MonarchInboxRow(
            title = "Ana Lima",
            onClick = {},
            unread = true,
            preview = "Can we move the call to Thursday?",
            timestamp = PreviewNow - PreviewMinutes,
            now = PreviewNow,
        )
        MonarchInboxRow(
            title = "Billing team",
            onClick = {},
            unreadCount = 128,
            preview = "Invoice 2041 was paid",
            timestamp = PreviewNow,
            now = PreviewNow,
            badges = { MonarchBadge(text = "Urgent", tone = MonarchTone.Danger) },
        )
        MonarchInboxRow(
            title = "Ben Okafor",
            onClick = {},
            selected = true,
            selecting = true,
            onSelectedChange = {},
            preview = "Thanks!",
        )
    }
}

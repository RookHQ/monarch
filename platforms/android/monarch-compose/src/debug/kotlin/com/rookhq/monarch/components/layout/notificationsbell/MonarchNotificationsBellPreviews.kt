package com.rookhq.monarch.components.layout.notificationsbell

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchNotificationsBellPreview() {
    PreviewFrame {
        MonarchNotificationsBell(onClick = {}) { MonarchIcon(PreviewIcon, contentDescription = null) }
        MonarchNotificationsBell(onClick = {}, unreadCount = 3) { MonarchIcon(PreviewIcon, contentDescription = null) }
        MonarchNotificationsBell(onClick = {}, unreadCount = 120) {
            MonarchIcon(PreviewIcon, contentDescription = null)
        }
        MonarchNotificationsBell(onClick = {}, unreadCount = 2, variant = MonarchNotificationsBellVariant.Row) {
            MonarchIcon(PreviewIcon, contentDescription = null)
        }
    }
}

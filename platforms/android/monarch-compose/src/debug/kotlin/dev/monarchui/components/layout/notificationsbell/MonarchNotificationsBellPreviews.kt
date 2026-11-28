package dev.monarchui.components.layout.notificationsbell

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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

package com.rookhq.monarch.components.identity.avatar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchAvatarPreview() {
    PreviewFrame {
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchAvatarSize.entries.forEach { size ->
                MonarchAvatar(image = null, name = "Ana Silva", size = size, status = MonarchPresenceStatus.Online)
            }
            MonarchAvatar(
                image = null,
                name = "Rook Labs",
                shape = MonarchAvatarShape.Square,
                icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
            )
        }
        MonarchAvatarGroup(
            items = listOf("Ana Silva", "Ben Okafor", "Chen Wei", "Dara Kim", "Eli Roth"),
            label = "Assignees",
            maxVisible = 3,
        ) { name -> MonarchAvatar(image = null, name = name) }
    }
}

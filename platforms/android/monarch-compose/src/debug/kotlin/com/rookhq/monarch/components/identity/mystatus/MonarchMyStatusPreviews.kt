package com.rookhq.monarch.components.identity.mystatus

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMyStatusPreview() {
    PreviewFrame {
        MonarchMyStatus(
            name = "Priya Raman",
            presence = MonarchPresenceStatus.Online,
            onClick = {},
            customStatus = MonarchMyStatusCustomStatus(text = "On holiday", emoji = "🌴"),
        )
        MonarchMyStatus(name = "Priya Raman", presence = MonarchPresenceStatus.DoNotDisturb, onClick = {})
        MonarchMyStatus(name = "Priya Raman", presence = MonarchPresenceStatus.Offline, onClick = {})
    }
}

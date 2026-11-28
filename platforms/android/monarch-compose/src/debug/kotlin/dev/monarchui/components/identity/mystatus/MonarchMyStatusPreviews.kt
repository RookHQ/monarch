package dev.monarchui.components.identity.mystatus

import androidx.compose.runtime.Composable
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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

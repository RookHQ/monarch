package com.rookhq.monarch.components.collaboration.activitytimeline

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.util.TimeZone

private const val Now = 1_772_636_400_000L
private const val Hour = 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchActivityTimelinePreview() {
    PreviewFrame {
        MonarchActivityTimeline(
            events = listOf(
                MonarchActivityTimelineEvent(
                    id = "1",
                    verb = "moved",
                    time = Now - Hour,
                    actor = MonarchActivityTimelineActor("Ana Ruiz"),
                    target = "Acme renewal",
                    tone = MonarchTone.Info,
                    tags = listOf("Negotiation"),
                    details = "Stage: Proposal → Negotiation",
                ),
                MonarchActivityTimelineEvent(
                    id = "2",
                    verb = "sent a reminder to",
                    time = Now - 26 * Hour,
                    actor = MonarchActivityTimelineActor(
                        "Follow-up rule",
                        kind = MonarchActivityTimelineActorKind.Automation,
                    ),
                    target = "Ben Okafor",
                    tone = MonarchTone.Success,
                ),
            ),
            modifier = Modifier.height(320.dp),
            marker = MonarchActivityTimelineMarker.Avatar,
            now = Now,
            timeZone = "UTC",
        )
    }
}

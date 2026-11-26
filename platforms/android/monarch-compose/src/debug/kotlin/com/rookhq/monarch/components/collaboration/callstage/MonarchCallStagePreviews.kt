@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.collaboration.callstage

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControlsLayout
import com.rookhq.monarch.components.collaboration.calltile.MonarchCallQuality
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewStart = 1_767_225_600_000L
private const val PreviewElapsed = 252_000L

private val previewPeople = listOf(
    MonarchCallStageParticipant("me", "Sam Lee", self = true, micOff = true),
    MonarchCallStageParticipant("robin", "Robin Park", speaking = true, quality = MonarchCallQuality.Good),
    MonarchCallStageParticipant("alex", "Alex Chen", handRaised = true, quality = MonarchCallQuality.Poor),
)

@MonarchPreviews
@Composable
internal fun MonarchCallStagePreview() {
    PreviewFrame {
        MonarchCallStage(
            participants = previewPeople,
            title = "Weekly planning",
            startedAt = PreviewStart,
            now = PreviewStart + PreviewElapsed,
            mode = MonarchCallControlsLayout.Grid,
            onPinnedIdChange = {},
            lowData = true,
        )
        MonarchCallStage(
            participants = previewPeople,
            now = PreviewStart,
            mode = MonarchCallControlsLayout.Speaker,
            activeSpeakerId = "robin",
            surface = MonarchCallStageSurface.Docked,
            reconnecting = true,
            onSurfaceChange = {},
            onMinimize = {},
        )
    }
}

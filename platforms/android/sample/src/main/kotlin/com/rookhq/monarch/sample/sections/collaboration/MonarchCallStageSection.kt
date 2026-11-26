@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControl
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControls
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControlsLayout
import com.rookhq.monarch.components.collaboration.callstage.MonarchCallStage
import com.rookhq.monarch.components.collaboration.callstage.MonarchCallStageParticipant
import com.rookhq.monarch.components.collaboration.callstage.MonarchCallStageSurface
import com.rookhq.monarch.components.collaboration.calltile.MonarchCallQuality
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of CallStage. Avatars stand in for the call SDK's video; framework drawables for icons. */
object MonarchCallStageSection : CataloguePage {
    override val title = "CallStage"

    @Composable
    override fun Content() {
        var mic by rememberSaveable { mutableStateOf(true) }
        var lowData by rememberSaveable { mutableStateOf(false) }
        var mode by rememberSaveable { mutableStateOf(MonarchCallControlsLayout.Automatic) }
        var pinned by rememberSaveable { mutableStateOf<String?>(null) }
        var surface by rememberSaveable { mutableStateOf(MonarchCallStageSurface.Stage) }
        var last by rememberSaveable { mutableStateOf("Nothing yet") }
        val startedAt = remember { System.currentTimeMillis() }
        val people = listOf(
            MonarchCallStageParticipant("me", "Sam Lee", self = true, micOff = !mic),
            MonarchCallStageParticipant("robin", "Robin Park", speaking = true, quality = MonarchCallQuality.Good),
            MonarchCallStageParticipant("alex", "Alex Chen", micOff = true, quality = MonarchCallQuality.Fair),
        )
        CatalogueCaption("A call of three")
        MonarchCallStage(
            participants = people,
            title = "Weekly planning",
            startedAt = startedAt,
            mode = mode,
            pinnedId = pinned,
            onPinnedIdChange = { pinned = it },
            activeSpeakerId = "robin",
            surface = surface,
            lowData = lowData,
            onSurfaceChange = { surface = it },
            onMinimize = { last = "Minimized" },
            onPictureInPicture = { last = "Picture in picture" },
            onFullScreen = { last = "Full screen: $it" },
        ) {
            MonarchCallControls(
                icon = { control, on -> MonarchIcon(iconOf(control, on), contentDescription = null) },
                micOn = mic,
                onMicOnChange = { mic = it },
                layout = mode,
                onLayoutChange = { mode = it },
                lowData = lowData,
                onLowDataChange = { lowData = it },
                minimal = surface != MonarchCallStageSurface.Stage,
                onLeave = { last = "Left the call" },
            )
        }
        MonarchText("Last action: $last")
        CatalogueCaption("Alone, reconnecting")
        MonarchCallStage(
            participants = listOf(MonarchCallStageParticipant("me", "Sam Lee", self = true)),
            surface = MonarchCallStageSurface.Docked,
            reconnecting = true,
        )
    }

    private fun iconOf(control: MonarchCallControl, on: Boolean): ImageVector = when (control) {
        MonarchCallControl.Microphone ->
            if (on) SampleIcons.Mic else SampleIcons.MicOff
        MonarchCallControl.Camera -> SampleIcons.Video
        MonarchCallControl.ShareScreen -> SampleIcons.ScreenShare
        MonarchCallControl.RaiseHand -> SampleIcons.RaiseHand
        MonarchCallControl.React -> SampleIcons.React
        MonarchCallControl.More -> SampleIcons.More
        MonarchCallControl.Leave -> SampleIcons.CallEnd
    }
}

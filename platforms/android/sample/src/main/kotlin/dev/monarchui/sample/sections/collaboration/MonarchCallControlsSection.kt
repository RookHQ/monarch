package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.monarchui.components.collaboration.callcontrols.MonarchCallControl
import dev.monarchui.components.collaboration.callcontrols.MonarchCallControls
import dev.monarchui.components.collaboration.callcontrols.MonarchCallControlsLayout
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of CallControls. Framework drawables stand in for the app's call icons. */
object MonarchCallControlsSection : CataloguePage {
    override val title = "CallControls"

    @Composable
    override fun Content() {
        var mic by rememberSaveable { mutableStateOf(true) }
        var camera by rememberSaveable { mutableStateOf(true) }
        var sharing by rememberSaveable { mutableStateOf(false) }
        var hand by rememberSaveable { mutableStateOf(false) }
        var lowData by rememberSaveable { mutableStateOf(false) }
        var layout by rememberSaveable { mutableStateOf(MonarchCallControlsLayout.Automatic) }
        var last by rememberSaveable { mutableStateOf("Nothing yet") }
        val icon: @Composable (MonarchCallControl, Boolean) -> Unit = { control, on ->
            MonarchIcon(iconOf(control, on), contentDescription = null)
        }
        CatalogueCaption("Every control")
        MonarchCallControls(
            icon = icon,
            micOn = mic,
            onMicOnChange = { mic = it },
            cameraOn = camera,
            onCameraOnChange = { camera = it },
            sharing = sharing,
            onSharingChange = { sharing = it },
            handRaised = hand,
            onHandRaisedChange = { hand = it },
            layout = layout,
            onLayoutChange = { layout = it },
            lowData = lowData,
            onLowDataChange = { lowData = it },
            onReact = { last = "Reacted $it" },
            onOpenDevices = { last = "Devices" },
            onOpenStats = { last = "Connection" },
            onPictureInPicture = { last = "Picture in picture" },
            onLeave = { last = "Left the call" },
        )
        MonarchText(text = "Layout ${layout.name}. Last action: $last", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Minimal, for a small window")
        MonarchCallControls(
            icon = icon,
            micOn = mic,
            onMicOnChange = { mic = it },
            cameraOn = camera,
            onCameraOnChange = { camera = it },
            minimal = true,
            onLeave = { last = "Left the call" },
        )
    }

    private fun iconOf(control: MonarchCallControl, on: Boolean): ImageVector = when (control) {
        MonarchCallControl.Microphone ->
            if (on) SampleIcons.Mic else SampleIcons.MicOff
        MonarchCallControl.Camera ->
            if (on) SampleIcons.Video else SampleIcons.VideoOff
        MonarchCallControl.ShareScreen -> SampleIcons.ScreenShare
        MonarchCallControl.RaiseHand -> SampleIcons.RaiseHand
        MonarchCallControl.React -> SampleIcons.React
        MonarchCallControl.More -> SampleIcons.More
        MonarchCallControl.Leave -> SampleIcons.CallEnd
    }
}

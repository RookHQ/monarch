package dev.monarchui.components.collaboration.callcontrols

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.sampleicons.SampleIcons

@MonarchPreviews
@Composable
internal fun MonarchCallControlsPreview() {
    val icon: @Composable (MonarchCallControl, Boolean) -> Unit = { control, on ->
        val glyph = when (control) {
            MonarchCallControl.Microphone -> if (on) SampleIcons.Mic else SampleIcons.MicOff
            MonarchCallControl.Camera -> if (on) SampleIcons.Video else SampleIcons.VideoOff
            MonarchCallControl.ShareScreen -> SampleIcons.ScreenShare
            MonarchCallControl.RaiseHand -> SampleIcons.RaiseHand
            MonarchCallControl.React -> SampleIcons.React
            MonarchCallControl.More -> SampleIcons.More
            MonarchCallControl.Leave -> SampleIcons.CallEnd
        }
        MonarchIcon(glyph, contentDescription = null)
    }
    PreviewFrame {
        MonarchCallControls(
            icon = icon,
            micOn = false,
            onMicOnChange = {},
            onCameraOnChange = {},
            sharing = true,
            onSharingChange = {},
            handRaised = true,
            onHandRaisedChange = {},
            onReact = {},
            onOpenDevices = {},
            onLeave = {},
        )
        MonarchCallControls(icon = icon, onMicOnChange = {}, onCameraOnChange = {}, minimal = true, onLeave = {})
        MonarchCallControls(icon = icon, onMicOnChange = {}, enabled = false, onLeave = {})
    }
}

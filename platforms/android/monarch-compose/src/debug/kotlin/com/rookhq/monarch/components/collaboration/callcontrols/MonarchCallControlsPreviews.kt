package com.rookhq.monarch.components.collaboration.callcontrols

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.sampleicons.SampleIcons

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

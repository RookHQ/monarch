package com.rookhq.monarch.components.collaboration.screenshareview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@Composable
private fun PreviewVideo() {
    Box(modifier = Modifier.fillMaxSize().background(MonarchTheme.colors.bgMuted))
}

@MonarchPreviews
@Composable
internal fun MonarchScreenShareViewPreview() {
    PreviewFrame {
        MonarchScreenShareView(
            presenterName = "Ana",
            zoom = 1.5f,
            onZoomChange = {},
            onPictureInPicture = {},
            onFullScreen = {},
        ) { PreviewVideo() }
        MonarchScreenShareView(presenterName = "Ana", local = true, onStopSharing = {}) { PreviewVideo() }
        MonarchScreenShareView(presenterName = "Ana", waiting = true) { PreviewVideo() }
    }
}

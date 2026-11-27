package com.rookhq.monarch.components.technical.embeddedappframe

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchEmbeddedAppFramePreview() {
    PreviewFrame {
        MonarchEmbeddedAppFrame(
            title = "Example Desk: Email notifications",
            state = MonarchEmbeddedAppFrameState.Ready,
        ) {
            MonarchText(text = "Email notifications", modifier = Modifier.padding(MonarchTheme.spacing.space5))
        }
        MonarchEmbeddedAppFrame(title = "Example Desk", state = MonarchEmbeddedAppFrameState.Loading) {}
        MonarchEmbeddedAppFrame(
            title = "Example Desk: Billing",
            state = MonarchEmbeddedAppFrameState.Error,
            onRetry = {},
            onOpen = {},
        ) {}
    }
}

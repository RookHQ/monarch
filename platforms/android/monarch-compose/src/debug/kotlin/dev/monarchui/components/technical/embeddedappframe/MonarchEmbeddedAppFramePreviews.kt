package dev.monarchui.components.technical.embeddedappframe

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

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

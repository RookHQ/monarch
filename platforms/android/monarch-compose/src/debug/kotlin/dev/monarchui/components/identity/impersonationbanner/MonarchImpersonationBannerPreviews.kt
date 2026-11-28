package dev.monarchui.components.identity.impersonationbanner

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchImpersonationBannerPreview() {
    PreviewFrame {
        MonarchImpersonationBanner(
            name = "Sam Lee",
            onStop = {},
            detail = "sam@example.com",
            reason = "Ticket 4821",
            startedAtMillis = System.currentTimeMillis(),
        )
        MonarchImpersonationBanner(name = "Sam Lee", onStop = {}, stopping = true)
    }
}

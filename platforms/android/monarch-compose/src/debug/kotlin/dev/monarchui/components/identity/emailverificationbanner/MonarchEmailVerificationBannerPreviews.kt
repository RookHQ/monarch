package dev.monarchui.components.identity.emailverificationbanner

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchEmailVerificationBannerPreview() {
    PreviewFrame {
        MonarchEmailVerificationBanner(email = "ana@example.com", onResend = {}, onChangeEmail = {})
        MonarchEmailVerificationBanner(
            email = "ana@example.com",
            onResend = {},
            tone = MonarchTone.Info,
            initialCooldownSeconds = 42,
            onDismiss = {},
        )
    }
}

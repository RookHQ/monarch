package com.rookhq.monarch.components.identity.emailverificationbanner

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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

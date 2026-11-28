package dev.monarchui.components.identity.cookieconsentbanner

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCookieConsentBannerPreview() {
    PreviewFrame {
        MonarchCookieConsentBanner(
            regime = MonarchCookieConsentRegime.OptIn,
            onDecide = {},
            onCustomize = {},
            onPolicyClick = {},
        )
        MonarchCookieConsentBanner(regime = MonarchCookieConsentRegime.Notice, onDecide = {})
    }
}

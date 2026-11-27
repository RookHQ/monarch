package com.rookhq.monarch.components.identity.cookieconsentbanner

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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

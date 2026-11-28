package com.rookhq.monarch.components.forms.marketingconsentcheckbox

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMarketingConsentCheckboxPreview() {
    PreviewFrame {
        MonarchMarketingConsentCheckbox(
            selection = emptySet(),
            onConsentChange = {},
            description = "Product updates and offers, about twice a month.",
            policyTitle = "Privacy policy",
            onPolicyClick = {},
        )
        MonarchMarketingConsentCheckbox(
            selection = setOf(MonarchMarketingChannel.Sms),
            onConsentChange = {},
            channels = listOf(MonarchMarketingChannel.Email, MonarchMarketingChannel.Sms, MonarchMarketingChannel.Post),
        )
    }
}

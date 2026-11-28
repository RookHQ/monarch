package dev.monarchui.components.identity.consenthistory

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchConsentHistoryPreview() {
    PreviewFrame {
        MonarchConsentHistory(
            records = listOf(
                MonarchConsentRecord(
                    id = "1",
                    dateMillis = System.currentTimeMillis(),
                    method = MonarchConsentMethod.Custom,
                    policyVersion = "2026-03",
                    regime = MonarchConsentRegime.OptIn,
                    categories = mapOf("necessary" to true, "analytics" to false),
                    globalPrivacyControl = true,
                ),
            ),
            categories = listOf(
                MonarchConsentCategory("necessary", "Necessary"),
                MonarchConsentCategory("analytics", "Analytics"),
            ),
            showRelativeTime = true,
            onExport = {},
        )
        MonarchConsentHistory(records = emptyList())
    }
}

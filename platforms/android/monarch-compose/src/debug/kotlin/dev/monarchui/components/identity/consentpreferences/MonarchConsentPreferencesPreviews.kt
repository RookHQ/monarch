package dev.monarchui.components.identity.consentpreferences

import androidx.compose.runtime.Composable
import dev.monarchui.components.identity.consenthistory.MonarchConsentMethod
import dev.monarchui.components.identity.consenthistory.MonarchConsentRecord
import dev.monarchui.components.identity.consenthistory.MonarchConsentRegime
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewDay = 86_400_000L

private val PreviewCategories = listOf(
    MonarchConsentPreferencesCategory("necessary", "Strictly necessary", "Keeps you signed in.", required = true),
    MonarchConsentPreferencesCategory("analytics", "Analytics", "Helps us see which screens are used."),
    MonarchConsentPreferencesCategory("ads", "Advertising", "Shares data with ad partners.", saleOrSharing = true),
)

@MonarchPreviews
@Composable
internal fun MonarchConsentPreferencesPreview() {
    PreviewFrame {
        MonarchConsentPreferences(
            categories = PreviewCategories,
            onDecide = { _, _ -> },
            choices = mapOf("analytics" to true),
            globalPrivacyControl = true,
            record = MonarchConsentRecord(
                id = "1",
                dateMillis = PreviewNow - 2 * PreviewDay,
                method = MonarchConsentMethod.Custom,
                policyVersion = "3",
                regime = MonarchConsentRegime.OptIn,
                categories = mapOf("analytics" to true),
            ),
            now = PreviewNow,
            onPolicyClick = {},
        )
        MonarchConsentPreferences(
            categories = PreviewCategories.take(2),
            onDecide = { _, _ -> },
            mode = MonarchConsentPreferencesMode.Choice,
        )
    }
}

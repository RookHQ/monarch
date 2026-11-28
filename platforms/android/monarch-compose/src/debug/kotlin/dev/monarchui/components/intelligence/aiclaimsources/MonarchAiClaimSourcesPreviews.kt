@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiclaimsources

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private val PreviewSources = listOf(
    MonarchAiClaimSource(id = "report", title = "Q3 churn report", kind = MonarchAiSourceKind.Doc),
    MonarchAiClaimSource(id = "blog", title = "Pricing update", url = "https://example.com/pricing"),
)

private val PreviewClaims = listOf(
    MonarchAiClaim(
        id = "a",
        text = "Churn fell to 2.1% in Q3.",
        support = MonarchAiClaimSupport.Supported,
        evidence = listOf(
            MonarchAiClaimEvidence(
                id = "e1",
                sourceId = "report",
                quote = "Monthly churn averaged 2.1% across the quarter.",
                location = "Page 4",
                stance = MonarchAiClaimEvidenceStance.Supports,
            ),
        ),
    ),
    MonarchAiClaim(id = "b", text = "The price change caused the drop.", support = MonarchAiClaimSupport.Unsupported),
)

@MonarchPreviews
@Composable
internal fun MonarchAiClaimSourcesPreview() {
    PreviewFrame {
        MonarchAiClaimSources(
            claims = PreviewClaims,
            sources = PreviewSources,
            expandedIds = setOf("a"),
            onFlag = {},
            expandIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchAiClaimSources(claims = emptyList(), sources = emptyList())
    }
}

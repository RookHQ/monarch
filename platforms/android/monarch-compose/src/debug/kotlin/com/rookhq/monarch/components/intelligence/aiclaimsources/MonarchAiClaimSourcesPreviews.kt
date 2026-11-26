@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aiclaimsources

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
